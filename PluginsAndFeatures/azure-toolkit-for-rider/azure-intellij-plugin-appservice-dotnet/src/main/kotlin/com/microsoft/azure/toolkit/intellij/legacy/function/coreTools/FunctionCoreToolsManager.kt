/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalPathApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.rider.CPUKind
import com.microsoft.azure.toolkit.intellij.legacy.function.isFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed.FunctionsToolingFeedService
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.sequences.sortedWith

@Service(Service.Level.APP)
class FunctionCoreToolsManager {
    companion object {
        fun getInstance(): FunctionCoreToolsManager = service()
        private val LOG = logger<FunctionCoreToolsManager>()

        private const val CORE_TOOLING_FOLDERS_COUNT = 5
    }

    /**
     * Retrieves the path to the Azure Function core tools for a specified Azure Function runtime version
     * or downloads the latest core tools if not available.
     *
     * @param functionsRuntimeVersion The version of Azure Functions runtime for which to get or download the core tools.
     * @param targetFramework The target framework of the Azure Functions project. It is used to find local tools with the in-process model.
     * @return The path to the Azure Function core tools for the specified Azure Function runtime version,
     * or null if the path cannot be determined or the download fails.
     */
    suspend fun getFunctionCoreToolsPathOrDownloadForVersion(
        functionsRuntimeVersion: String,
        targetFramework: String? = null
    ): Path? {
        val existingCoreToolsPath = getFunctionCoreToolsPathForVersion(functionsRuntimeVersion, targetFramework)
        if (existingCoreToolsPath != null) {
            LOG.trace { "Found existing core tools path: $existingCoreToolsPath" }
            return existingCoreToolsPath
        }

        LOG.trace { "Existing core tools aren't found, downloading the latest one" }
        return downloadLatestFunctionCoreToolsForVersion(functionsRuntimeVersion)
    }

    /**
     * Retrieves the path to the Azure Function core tools folder for a specified Azure Function runtime version.
     *
     * @param functionsRuntimeVersion The version of Azure Functions runtime for which to get the folder.
     * @param targetFramework The target framework of the Azure Functions project. It is used to find local tools with the in-process model.
     * @return The path to the Azure Function core tools folder for the specified Azure Function runtime version, or null if not found.
     */
    fun getFunctionCoreToolsPathForVersion(functionsRuntimeVersion: String, targetFramework: String? = null): Path? {
        val settings = AzureFunctionSettings.getInstance()
        val coreToolsPathEntries = settings.azureCoreToolsPathEntries
        LOG.trace { "Core tools path from the settings: ${coreToolsPathEntries.joinToString()}" }

        val coreToolsPathFromSettings =
            if (functionsRuntimeVersion.equals("v0", true)) {
                coreToolsPathEntries
                    .firstOrNull { it.functionsVersion.equals("v4", ignoreCase = true) }
                    ?.coreToolsPath
                    ?.let { resolveCoreToolsPathFromSettings(it, targetFramework) }
            } else {
                coreToolsPathEntries
                    .firstOrNull { it.functionsVersion.equals(functionsRuntimeVersion, ignoreCase = true) }
                    ?.coreToolsPath
                    ?.let { resolveCoreToolsPathFromSettings(it, null) }
            }
        if (coreToolsPathFromSettings?.exists() == true) {
            LOG.trace { "Get Azure Function core tools path from the settings: $coreToolsPathFromSettings" }
            return coreToolsPathFromSettings
        }

        val coreToolsDownloadFolder = settings.functionDownloadPath
        if (coreToolsDownloadFolder.isEmpty()) {
            LOG.info("Unable to find any downloaded core tools because tool download path is not set up")
            return null
        }
        val coreToolsPathForVersion = Path(coreToolsDownloadFolder).resolve(functionsRuntimeVersion)
        if (coreToolsPathForVersion.notExists()) {
            LOG.info("Unable to find any downloaded core tools in the folder $coreToolsDownloadFolder for version $functionsRuntimeVersion")
            return null
        }

        LOG.trace { "Get Azure Function core tools path from the download folder: $coreToolsPathForVersion" }
        return findCoreToolsPathWithLatestTag(coreToolsPathForVersion)
    }

    /**
     * Downloads the latest Azure Function core tools release for the specified Azure Functions runtime version.
     *
     * @param functionsRuntimeVersion The version of Azure Functions runtime for which to download the latest core tools release.
     * @return The path to the downloaded Azure Function core tools, or null if the download was unsuccessful.
     */
    suspend fun downloadLatestFunctionCoreToolsForVersion(functionsRuntimeVersion: String, cpuKind: CPUKind? = null): Path? {
        val downloadLatestReleaseResult = FunctionsToolingFeedService
            .getInstance()
            .downloadLatestFunctionsToolingRelease(functionsRuntimeVersion, cpuKind)

        val latestReleasePath = downloadLatestReleaseResult.getOrNull()
        if (latestReleasePath == null) {
            LOG.warn(
                "Unable to download the latest Azure Function core tooling release for version $functionsRuntimeVersion",
                downloadLatestReleaseResult.exceptionOrNull()
            )
            return null
        }

        LOG.trace { "Latest Functions tooling release path: $latestReleasePath " }

        return latestReleasePath
    }

    /**
     * Updates the Azure Function core tools to the latest versions managed by Rider.
     *
     * This method retrieves the current core tools path settings and determines the versions
     * managed by Rider. It then checks if these versions already exist in the specified
     * core tools download directory. If they do, it retrieves the tooling releases
     * for these versions and updates the core tools accordingly. After updating,
     * it cleans up any unnecessary core tools for the specified versions.
     */
    suspend fun updateFunctionCoreTools() {
        val settings = AzureFunctionSettings.getInstance()
        val coreToolsPathEntries = settings.azureCoreToolsPathEntries
        val versionsManagedByRider = coreToolsPathEntries
            .filter { it.coreToolsPath.isEmpty() }
            .map { it.functionsVersion }
            .toMutableList()
        versionsManagedByRider.add("v0")
        LOG.trace { "Functions tooling managed by Rider: ${versionsManagedByRider.joinToString()}" }

        val coreToolsDownloadFolder = settings.functionDownloadPath
        if (coreToolsDownloadFolder.isEmpty()) {
            LOG.trace { "Unable to update core tools because tool download path is not set up" }
        }
        val coreToolsDownloadFolderPath = Path(coreToolsDownloadFolder)
        val versionsToUpdate = versionsManagedByRider.filter {
            coreToolsDownloadFolderPath.resolve(it).exists()
        }
        if (versionsToUpdate.isEmpty()) {
            LOG.trace { "Unable to find any Functions tooling versions to update" }
            return
        }

        val toolingReleases = FunctionsToolingFeedService
            .getInstance()
            .getFunctionsToolingReleaseForVersions(versionsToUpdate)
        if (toolingReleases == null) {
            LOG.trace { "Unable to get tooling releases for versions: ${versionsToUpdate.joinToString()}" }
            return
        }

        for (toolingRelease in toolingReleases) {
            updateFunctionCoreToolsForVersion(
                toolingRelease.functionsVersion,
                toolingRelease.releaseTag,
                coreToolsDownloadFolderPath
            )
            cleanUpCoreToolsForVersion(toolingRelease.functionsVersion, coreToolsDownloadFolderPath)
        }
    }

    private suspend fun updateFunctionCoreToolsForVersion(
        functionsRuntimeVersion: String,
        releaseTag: String,
        coreToolsDownloadFolder: Path
    ) {
        LOG.trace { "Updating Functions tooling. Version: $functionsRuntimeVersion, release: $releaseTag, folder: $coreToolsDownloadFolder" }

        val coreToolsPath = coreToolsDownloadFolder.resolve(functionsRuntimeVersion).resolve(releaseTag)
        if (coreToolsPath.exists() && coreToolsPath.resolveFunctionCoreToolsExecutable().exists()) {
            LOG.trace { "Core tools with tag $releaseTag already exists" }
            return
        }

        FunctionsToolingFeedService
            .getInstance()
            .downloadLatestFunctionsToolingRelease(functionsRuntimeVersion)
            .onFailure {
                LOG.warn("Unable to update core tools for version $functionsRuntimeVersion", it)
            }
    }

    private fun cleanUpCoreToolsForVersion(functionsRuntimeVersion: String, coreToolsDownloadFolder: Path) {
        LOG.trace { "Cleaning Functions tooling folders. Version: $functionsRuntimeVersion, download folder: $coreToolsDownloadFolder" }

        val coreToolsPathForVersion = coreToolsDownloadFolder.resolve(functionsRuntimeVersion)
        val tagFolders = coreToolsPathForVersion.listAllTagFolders()

        for (tagFolder in tagFolders) {
            runCatching {
                if (!tagFolder.resolveFunctionCoreToolsExecutable().exists()) {
                    LOG.trace { "Core tools folder $tagFolder is probably empty, removing it" }
                    tagFolder.deleteRecursively()
                }
            }.onFailure {
                LOG.trace(it)
            }
        }

        val tagFoldersWithoutEmpty = coreToolsPathForVersion.listAllTagFolders().toList()
        if (tagFoldersWithoutEmpty.size <= CORE_TOOLING_FOLDERS_COUNT) return

        val folderCountToDelete = tagFoldersWithoutEmpty.size - CORE_TOOLING_FOLDERS_COUNT
        for (tagFolderToDelete in tagFoldersWithoutEmpty.takeLast(folderCountToDelete)) {
            runCatching {
                LOG.trace { "Removing core tools folder $tagFolderToDelete" }
                tagFolderToDelete.deleteRecursively()
            }.onFailure {
                LOG.trace(it)
            }
        }
    }

    private fun resolveCoreToolsPathFromSettings(coreToolsPathValue: String, targetFramework: String?): Path? {
        if (coreToolsPathValue.isEmpty()) return null

        val coreToolsFolderPath = if (isFunctionCoreToolsExecutable(coreToolsPathValue)) {
            val coreToolsPathFromEnvironment = FunctionCliResolver.resolveFunc()?.let(::Path) ?: return null
            LOG.trace { "Resolved core tools path from environment: $coreToolsPathFromEnvironment" }
            patchCoreToolsPath(coreToolsPathFromEnvironment)
        }  else {
            val coreToolsPathFromSettings = Path(coreToolsPathValue)
            LOG.trace { "Resolved core tools path from settings: $coreToolsPathFromSettings" }
            patchCoreToolsPath(coreToolsPathFromSettings)
        }

        if (targetFramework == null) return coreToolsFolderPath

        val inProcFolder = when(targetFramework) {
            "net8.0" -> "in-proc8"
            "net6.0" -> "in-proc6"
            else -> {
                LOG.info("Unsupported target framework: $targetFramework for in-process worker model")
                return coreToolsFolderPath
            }
        }

        val inProcCoreToolsFolderPath = coreToolsFolderPath.resolve(inProcFolder)

        return if (inProcCoreToolsFolderPath.exists()) inProcCoreToolsFolderPath else coreToolsFolderPath
    }

    private fun patchCoreToolsPath(funcCoreToolsPath: Path): Path {
        val normalizedPath = if (funcCoreToolsPath.isRegularFile() && funcCoreToolsPath.isFunctionCoreTools()) {
            funcCoreToolsPath.parent
        } else {
            funcCoreToolsPath
        }
        if (!SystemInfo.isWindows) return normalizedPath

        // Chocolatey and NPM have shim executables that are not .NET (and not debuggable).
        // If it's a Chocolatey install or NPM install, rewrite the path to the tools path
        // where the func executable is located.
        //
        // Logic is similar to com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver.resolveFunc()
        val chocolateyPath = normalizedPath.resolve("../lib/azure-functions-core-tools/tools").normalize()
        if (chocolateyPath.exists()) {
            LOG.info("Functions core tools path $normalizedPath is Chocolatey-installed. Rewriting path to $chocolateyPath")
            return chocolateyPath
        }

        val npmPath = normalizedPath.resolve("../node_modules/azure-functions-core-tools/bin").normalize()
        if (npmPath.exists()) {
            LOG.info("Functions core tools path $normalizedPath is NPM-installed. Rewriting path to $npmPath")
            return npmPath
        }

        return normalizedPath
    }

    private fun findCoreToolsPathWithLatestTag(coreToolsPathForVersion: Path): Path? {
        val latestTagFolderForVersion = coreToolsPathForVersion
            .listAllTagFolders()
            .firstOrNull {
                val coreToolExecutablePath = it.resolveFunctionCoreToolsExecutable()
                coreToolExecutablePath.exists()
            }

        LOG.trace { "The latest tag folder from $coreToolsPathForVersion is $latestTagFolderForVersion" }

        return latestTagFolderForVersion
    }

    private fun Path.listAllTagFolders() = listDirectoryEntries()
        .asSequence()
        .filter { it.isDirectory() && it.exists() }
        .sortedWith { first, second -> -1 * VersionComparatorUtil.compare(first.name, second.name) }
}