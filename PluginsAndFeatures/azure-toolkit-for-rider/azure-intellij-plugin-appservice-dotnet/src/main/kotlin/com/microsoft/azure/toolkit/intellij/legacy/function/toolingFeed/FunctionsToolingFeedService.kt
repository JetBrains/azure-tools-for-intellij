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
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.resolveFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
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

    private val releaseCache = concurrentMapOf<String, FunctionsToolingRelease>()
    private val releaseCacheMutex = Mutex()
    private val functionsToolingReleaseMutex = Mutex()

    private val client = HttpClient(CIO) {
        engine {
            https {
                trustManager = CertificateManager.getInstance().trustManager
            }
        }
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
                ignoreUnknownKeys = true
                allowTrailingComma = true
            })
        }
    }

    /**
     * Downloads and saves the Azure Functions tooling release feed if the release cache is empty.
     *
     * @return Result wrapping any exception encountered during the execution.
     */
    suspend fun downloadAndSaveReleaseFeed() = kotlin.runCatching {
        if (releaseCache.isNotEmpty()) return@runCatching

        releaseCacheMutex.withLock {
            if (releaseCache.isNotEmpty()) return@withLock

            LOG.trace("Downloading Functions tooling release feed")

            val feed = getReleaseFeed()
            val releaseTags = feed.tags
                .toSortedMap()
                .filterValues { !it.releaseQuality.isNullOrEmpty() && !it.release.isNullOrEmpty() && !it.hidden }
            val releaseFilter = getReleaseFilterForCurrentSystem()

            for ((releaseTagName, releaseTag) in releaseTags) {
                val releaseFromTag = fixedReleases[releaseTagName] ?: releaseTag.release ?: continue
                val release = feed.releases[releaseFromTag] ?: continue
                val coreToolsRelease = release.findCoreToolsRelease(releaseFilter) ?: continue

                val releaseKey = releaseTagName.lowercase()
                LOG.trace { "Release for Azure core tools version ${releaseKey}: ${releaseFromTag}; ${coreToolsRelease.downloadLink}" }

                releaseCache.putIfAbsent(
                    releaseKey,
                    FunctionsToolingRelease(releaseKey, releaseFromTag, coreToolsRelease.downloadLink ?: "")
                )
            }
        }
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
    suspend fun downloadLatestFunctionsToolingRelease(functionsRuntimeVersion: String): Result<Path> {
        downloadAndSaveReleaseFeed().onFailure { exception ->
            LOG.warn("Unable to download Function tooling release feed", exception)
            return Result.failure(exception)
        }

        val toolingRelease = getLatestFunctionsToolingRelease(functionsRuntimeVersion)
        if (toolingRelease == null) {
            return Result.failure(IllegalStateException("Unable to obtain latest function tooling release"))
        }
        val toolingReleasePath = getPathForLatestFunctionsToolingRelease(toolingRelease)
        if (toolingReleasePath == null) {
            return Result.failure(IllegalStateException("Unable to path to download function tooling release"))
        }
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

    private suspend fun downloadAndExtractFunctionsToolingRelease(
        toolingRelease: FunctionsToolingRelease,
        toolingReleasePath: Path,
        coreToolsExecutablePath: Path,
    ): Result<Path> {
        functionsToolingReleaseMutex.withLock {
            if (coreToolsExecutablePath.exists()) {
                LOG.trace { "The release $toolingRelease is already downloaded" }
                return Result.success(toolingReleasePath)
            }

            try {
                val tempFile = FileUtil.createTempFile(
                    File(FileUtil.getTempDirectory()),
                    "AzureFunctions-${toolingRelease.functionsVersion}-${toolingRelease.releaseTag}",
                    ".zip",
                    true,
                    true
                )

                LOG.trace { "Created a temporary file: ${tempFile.absolutePath}" }

                withContext(Dispatchers.IO) {
                    client.prepareGet(toolingRelease.artifactUrl).execute { httpResponse ->
                        val channel: ByteReadChannel = httpResponse.body()
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                            while (!packet.exhausted()) {
                                val bytes = packet.readByteArray()
                                tempFile.appendBytes(bytes)
                            }
                        }
                    }
                }

                LOG.trace { "Downloaded core tooling archive to the ${tempFile.absolutePath}" }

                if (!toolingReleasePath.exists())
                    toolingReleasePath.createDirectories()

                LOG.trace { "Extracting from ${tempFile.absolutePath} to $toolingReleasePath" }
                ZipUtil.extract(tempFile.toPath(), toolingReleasePath, null, true)

                if (tempFile.exists())
                    tempFile.delete()

                if (!coreToolsExecutablePath.isExecutable() && !SystemInfo.isWindows)
                    setExecutablePermissionsForCoreTools(coreToolsExecutablePath)

                return Result.success(toolingReleasePath)
            } catch (e: Exception) {
                LOG.warn("Unable to download Function tooling release $toolingRelease")
                toolingReleasePath.deleteRecursively()
                return Result.failure(e)
            }
        }
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

        return functionsRuntimeVersions.mapNotNull { getLatestFunctionsToolingRelease(it) }
    }

    private suspend fun getReleaseFeed(): ReleaseFeed {
        val feedUrl = Registry.get("azure.function_app.core_tools.feed.url").asString()
        LOG.trace { "Functions tooling release feed: $feedUrl" }

        val response = withContext(Dispatchers.IO) {
            client.get(feedUrl)
        }

        return response.body<ReleaseFeed>()
    }

    private fun getLatestFunctionsToolingRelease(functionsRuntimeVersion: String): FunctionsToolingRelease? {
        val toolingRelease = releaseCache[functionsRuntimeVersion.lowercase()]
        if (toolingRelease == null) {
            LOG.warn("Could not determine Functions tooling release for version: '$functionsRuntimeVersion'")
            return null
        }

        LOG.trace { "Latest Functions tooling release for version '$functionsRuntimeVersion' is '$toolingRelease'" }

        return toolingRelease
    }

    private fun getPathForLatestFunctionsToolingRelease(toolingRelease: FunctionsToolingRelease): Path? {
        val settings = AzureFunctionSettings.getInstance()
        val coreToolsDownloadFolder = settings.functionDownloadPath
        val downloadRoot =
            if (coreToolsDownloadFolder.isNotEmpty()) Path(coreToolsDownloadFolder)
            else null

        val path = downloadRoot?.resolve(toolingRelease.functionsVersion)?.resolve(toolingRelease.releaseTag)

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
