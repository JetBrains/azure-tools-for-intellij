/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalSerializationApi::class, ExperimentalPathApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.ZipUtil
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.rd.util.concurrentMapOf
import com.jetbrains.rider.CPUKind
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.resolveFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

@Service(Service.Level.APP)
class FunctionsToolingFeedService : Disposable {
    companion object {
        fun getInstance(): FunctionsToolingFeedService = service()
        private val LOG = logger<FunctionsToolingFeedService>()
    }

    private val fixedReleases = mapOf<String, String>()

    private val releaseCache = concurrentMapOf<Pair<String, FunctionToolingFeedFilter>, FunctionsToolingRelease>()
    private val releaseCacheMutex = Mutex()
    private val functionsToolingReleaseMutex = Mutex()

    private val client = HttpClient(CIO) {
        engine {
            https {
                trustManager = CertificateManager.getInstance().trustManager
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300000
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        allowTrailingComma = true
        allowComments = true
    }

    /**
     * Downloads the latest Azure Functions tooling release for the specified Azure Functions runtime version.
     *
     * This method fetches the release information, determines if the release has already been downloaded,
     * and if not, it downloads the release, extracts it to the appropriate directory and cleans up any temporary files.
     *
     * @param functionsRuntimeVersion The version of Azure Functions runtime for which to download the latest tooling release.
     * @return A Result wrapping the path to the latest Azure Functions tooling release.
     */
    suspend fun downloadLatestFunctionsToolingRelease(functionsRuntimeVersion: String, cpuKind: CPUKind? = null): Result<Path> {
        downloadAndSaveReleaseFeed(cpuKind).onFailure { error ->
            LOG.warn("Unable to download Function tooling release feed", error)
            return Result.failure(error)
        }

        val releaseFilter = getReleaseFilterForCurrentSystem(cpuKind)
        val toolingRelease = getLatestFunctionsToolingRelease(functionsRuntimeVersion, releaseFilter)
            ?: return Result.failure(IllegalStateException("Unable to obtain latest function tooling release"))
        val toolingReleasePath = getPathForLatestFunctionsToolingRelease(toolingRelease, releaseFilter)
            ?: return Result.failure(IllegalStateException("Unable to path to download function tooling release"))

        val coreToolsExecutablePath = toolingReleasePath.resolveFunctionCoreToolsExecutable()
        if (coreToolsExecutablePath.exists()) {
            LOG.trace { "The release $toolingRelease is already downloaded" }
            return Result.success(toolingReleasePath)
        }

        return downloadAndExtractFunctionsToolingRelease(
            toolingRelease,
            toolingReleasePath,
            coreToolsExecutablePath
        )
    }

    /**
     * Retrieves a list of Azure Functions tooling releases for the specified Azure Functions runtime versions.
     *
     * @param functionsRuntimeVersions List of Azure Functions runtime versions.
     * @return List of tooling releases corresponding to the given versions, or null if the release feed could not be downloaded.
     */
    suspend fun getFunctionsToolingReleaseForVersions(functionsRuntimeVersions: List<String>): List<FunctionsToolingRelease>? {
        downloadAndSaveReleaseFeed().onFailure { exception ->
            LOG.warn("Unable to download Function tooling release feed", exception)
            return null
        }

        val releaseFilter = getReleaseFilterForCurrentSystem()
        return functionsRuntimeVersions.mapNotNull { getLatestFunctionsToolingRelease(it, releaseFilter) }
    }

    /**
     * Downloads and saves the Azure Functions tooling release feed if the release cache is empty.
     *
     * @return Result wrapping any exception encountered during the execution.
     */
    private suspend fun downloadAndSaveReleaseFeed(cpuKind: CPUKind? = null): Result<Unit> {
        val releaseFilter = getReleaseFilterForCurrentSystem(cpuKind)

        if (releaseCache.keys.any { it.second == releaseFilter }) return Result.success(Unit)

        releaseCacheMutex.withLock {
            if (releaseCache.keys.any { it.second == releaseFilter }) return Result.success(Unit)

            LOG.trace("Downloading Functions tooling release feed")

            val feedResult = downloadFunctionsToolingReleaseFeed().onFailure { error ->
                return Result.failure(error)
            }

            val feed = feedResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Unable to download Function tooling release feed"))
            val releaseTags = feed.tags
                .toSortedMap()
                .filterValues { !it.releaseQuality.isNullOrEmpty() && !it.release.isNullOrEmpty() && !it.hidden }

            for ((releaseTagName, releaseTag) in releaseTags) {
                val releaseFromTag = fixedReleases[releaseTagName] ?: releaseTag.release ?: continue
                val release = feed.releases[releaseFromTag] ?: continue
                val coreToolsRelease = release.findCoreToolsRelease(releaseFilter) ?: continue

                val releaseKey = releaseTagName.lowercase()
                val cacheKey = releaseKey to releaseFilter

                LOG.trace {
                    "Release for Azure core tools version $releaseKey (${releaseFilter.os}): $releaseFromTag; ${coreToolsRelease.downloadLink}"
                }

                releaseCache.putIfAbsent(
                    cacheKey,
                    FunctionsToolingRelease(releaseKey, releaseFromTag, coreToolsRelease.downloadLink ?: "")
                )
            }

            return Result.success(Unit)
        }
    }

    private suspend fun downloadFunctionsToolingReleaseFeed(): Result<ReleaseFeed> = runCatching {
        val feedUrl = Registry.get("azure.function_app.core_tools.feed.url").asString()
        LOG.trace { "Functions tooling release feed: $feedUrl" }

        val temporaryFeedFile = FileUtil.createTempFile(
            File(FileUtil.getTempDirectory()),
            "AzureFunctionsToolingFeed",
            ".json",
            true,
            false
        )
        val temporaryFeedPath = temporaryFeedFile.toPath()

        LOG.trace { "Created a temporary feed file: ${temporaryFeedPath.absolutePathString()}" }

        withContext(Dispatchers.IO) {
            client.prepareGet(feedUrl).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                channel.copyAndClose(temporaryFeedFile.writeChannel())
            }
        }

        LOG.trace { "Downloaded Functions tooling feed to the ${temporaryFeedPath.absolutePathString()}" }

        val feed = withContext(Dispatchers.IO) {
            json.decodeFromStream<ReleaseFeed>(temporaryFeedPath.inputStream())
        }

        temporaryFeedPath.deleteIfExists()

        return Result.success(feed)
    }


    private suspend fun downloadAndExtractFunctionsToolingRelease(
        toolingRelease: FunctionsToolingRelease,
        toolingReleasePath: Path,
        coreToolsExecutablePath: Path,
    ): Result<Path> {
        if (coreToolsExecutablePath.exists()) {
            LOG.trace { "The release $toolingRelease is already downloaded" }
            return Result.success(toolingReleasePath)
        }

        functionsToolingReleaseMutex.withLock {
            if (coreToolsExecutablePath.exists()) {
                LOG.trace { "The release $toolingRelease is already downloaded" }
                return Result.success(toolingReleasePath)
            }

            try {
                val temporaryArchive = downloadFunctionsToolingArchive(toolingRelease)

                if (!toolingReleasePath.exists()) toolingReleasePath.createDirectories()

                LOG.trace { "Extracting from ${temporaryArchive.absolutePathString()} to $toolingReleasePath" }
                ZipUtil.extract(temporaryArchive, toolingReleasePath, null, true)

                temporaryArchive.deleteIfExists()

                if (!coreToolsExecutablePath.isExecutable() && !SystemInfo.isWindows) {
                    setExecutablePermissionsForCoreTools(coreToolsExecutablePath)
                }

                return Result.success(toolingReleasePath)
            } catch (e: Exception) {
                LOG.warn("Unable to download Function tooling release $toolingRelease")
                toolingReleasePath.deleteRecursively()
                return Result.failure(e)
            }
        }
    }

    private suspend fun downloadFunctionsToolingArchive(toolingRelease: FunctionsToolingRelease): Path {
        val temporaryArchive = FileUtil.createTempFile(
            File(FileUtil.getTempDirectory()),
            "AzureFunctions-${toolingRelease.functionsVersion}-${toolingRelease.releaseTag}",
            ".zip",
            true,
            false
        )
        val temporaryArchivePath = temporaryArchive.toPath()

        LOG.trace { "Created a temporary file: ${temporaryArchivePath.absolutePathString()}" }

        withContext(Dispatchers.IO) {
            client.prepareGet(toolingRelease.artifactUrl).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                channel.copyAndClose(temporaryArchive.writeChannel())
            }
        }

        LOG.trace { "Downloaded Functions tooling archive to the ${temporaryArchivePath.absolutePathString()}" }

        return temporaryArchivePath
    }

    private fun getLatestFunctionsToolingRelease(
        functionsRuntimeVersion: String,
        releaseFilter: FunctionToolingFeedFilter
    ): FunctionsToolingRelease? {
        val toolingRelease = releaseCache[functionsRuntimeVersion.lowercase() to releaseFilter]

        if (toolingRelease == null) {
            LOG.warn("Could not determine Functions tooling release for version: '$functionsRuntimeVersion'")
            return null
        }

        LOG.trace { "Latest Functions tooling release for version '$functionsRuntimeVersion' (${releaseFilter.os}) is '$toolingRelease'" }

        return toolingRelease
    }

    private fun getPathForLatestFunctionsToolingRelease(
        toolingRelease: FunctionsToolingRelease,
        releaseFilter: FunctionToolingFeedFilter
    ): Path? {
        val settings = AzureFunctionSettings.getInstance()
        val coreToolsDownloadFolder = settings.functionDownloadPath
        val downloadRoot =
            if (coreToolsDownloadFolder.isNotEmpty()) Path(coreToolsDownloadFolder)
            else null

        val osFolder = releaseFilter.os.lowercase()

        val path = downloadRoot
            ?.resolve(osFolder)
            ?.resolve(toolingRelease.functionsVersion)
            ?.resolve(toolingRelease.releaseTag)

        LOG.trace { "Path for the Latest Functions tooling release is $path" }

        return path
    }

    private fun Release.findCoreToolsRelease(releaseFilter: FunctionToolingFeedFilter) =
        coreTools
            .asSequence()
            .filter {
                it.os.equals(releaseFilter.os, ignoreCase = true) && !it.downloadLink.isNullOrEmpty()
            }
            .sortedWith(
                compareBy<CoreToolsRelease> {
                    releaseFilter.architectures.indexOfFirst { architecture ->
                        it.architecture.equals(architecture, ignoreCase = true)
                    }.let { rank -> if (rank >= 0) rank else 9999 }
                }.thenBy {
                    releaseFilter.sizes.indexOfFirst { size ->
                        it.size.equals(size, ignoreCase = true)
                    }.let { rank -> if (rank >= 0) rank else 9999 }
                })
            .firstOrNull()

    private fun setExecutablePermissionsForCoreTools(coreToolsExecutable: Path) {
        LOG.trace { "Setting permissions for $coreToolsExecutable" }
        coreToolsExecutable.setPosixFilePermissions(
            setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            )
        )
    }

    override fun dispose() = client.close()
}
