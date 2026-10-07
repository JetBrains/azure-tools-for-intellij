/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalPathApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.VersionComparatorUtil
import com.microsoft.azure.toolkit.intellij.legacy.function.isFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver
import java.nio.file.Path
import kotlin.io.path.*

@Service(Service.Level.APP)
class FunctionCoreToolsManager {
    companion object {
        fun getInstance(): FunctionCoreToolsManager = service()
        private val LOG = logger<FunctionCoreToolsManager>()
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

    private fun resolveCoreToolsPathFromSettings(coreToolsPathValue: String, targetFramework: String?): Path? {
        if (coreToolsPathValue.isEmpty()) return null

        val coreToolsFolderPath = if (isFunctionCoreToolsExecutable(coreToolsPathValue)) {
            val coreToolsPathFromEnvironment = FunctionCliResolver.resolveFunc()?.let(::Path) ?: return null
            LOG.trace { "Resolved core tools path from environment: $coreToolsPathFromEnvironment" }
            patchCoreToolsPath(coreToolsPathFromEnvironment)
        } else {
            val coreToolsPathFromSettings = Path(coreToolsPathValue)
            LOG.trace { "Resolved core tools path from settings: $coreToolsPathFromSettings" }
            patchCoreToolsPath(coreToolsPathFromSettings)
        }

        if (targetFramework == null) return coreToolsFolderPath

        val inProcFolder = when (targetFramework) {
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