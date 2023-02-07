///**
// * Copyright (c) 2019-2022 JetBrains s.r.o.
// *
// * All rights reserved.
// *
// * MIT License
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// *
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package org.jetbrains.plugins.azure.functions.coreTools
//
//import com.intellij.ide.util.PropertiesComponent
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.util.SystemInfo
//import com.intellij.openapi.util.registry.Registry
//import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver
//import com.microsoft.intellij.configuration.AzureRiderSettings
//import java.io.File
//
//data class FunctionsCoreToolsInfo(val coreToolsPath: String, var coreToolsExecutable: String)
//
//object FunctionsCoreToolsInfoProvider {
//
//    private val properties: PropertiesComponent = PropertiesComponent.getInstance()
//    private val logger = Logger.getInstance(FunctionsCoreToolsInfoProvider::class.java)
//
//    fun retrieveForProject(project: Project, projectFilePath: String, allowDownload: Boolean): FunctionsCoreToolsInfo? {
//
//        // Determine "AzureFunctionsVersion" property in MSBuild.
//        // This will be used to find the correct Azure Functions Core Tools executable.
//        val azureFunctionsVersion = FunctionsCoreToolsMsBuild.requestAzureFunctionsVersion(project, projectFilePath)
//
//        if (azureFunctionsVersion == null) {
//            logger.error("Could not determine project MSBuild property '${FunctionsCoreToolsMsBuild.PROPERTY_AZURE_FUNCTIONS_VERSION}'.")
//            return null
//        }
//
//        logger.info("MSBuild property ${FunctionsCoreToolsMsBuild.PROPERTY_AZURE_FUNCTIONS_VERSION}: $azureFunctionsVersion")
//
//        // Retrieve matching Azure Functions Core Tools information.
//        return retrieveForVersion(project, azureFunctionsVersion, allowDownload)
//    }
//
//    fun retrieveForVersion(project: Project, azureFunctionsVersion: String, allowDownload: Boolean): FunctionsCoreToolsInfo? {
//
//        // Based on priority, try and retrieve the tool information.
//        val coreToolsFromConfiguration = retrieveFromConfiguration(azureFunctionsVersion)
//        if (coreToolsFromConfiguration != null) {
//            return coreToolsFromConfiguration
//        }
//        logger.info("Could not determine Azure Core Tools path from configuration")
//
//        val coreToolsFromFeed = retrieveFromFeed(project, azureFunctionsVersion, allowDownload)
//        if (coreToolsFromFeed != null) {
//            return coreToolsFromFeed
//        }
//        logger.info("Could not determine Azure Core Tools path from feed")
//
//        return null
//    }
//
//    private fun retrieveFromConfiguration(azureFunctionsVersion: String): FunctionsCoreToolsInfo? {
//
//        // Determine Azure Functions Core Tools from configuration
//        val coreToolsConfiguration = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)
//        logger.debugValues("Azure Core Tools path entries", coreToolsConfiguration.map { "${it.functionsVersion}: ${it.coreToolsPath}" })
//
//        val coreToolsPathFromConfiguration = coreToolsConfiguration
//                .firstOrNull { it.functionsVersion.equals(azureFunctionsVersion, ignoreCase = true) }
//                ?.coreToolsPath ?: return null
//
//        // If configuration is func/func.cmd/func.exe, try and determine the full path from the environment
//        if (coreToolsPathFromConfiguration.equals("func", ignoreCase = true) ||
//                coreToolsPathFromConfiguration.equals("func.cmd", ignoreCase = true) ||
//                coreToolsPathFromConfiguration.equals("func.exe", ignoreCase = true)) {
//
//            val coreToolsInfoFromEnvironment = FunctionCliResolver.resolveFunc()
//                    ?.let { resolveFromPath(File(it)) }
//
//            if (coreToolsInfoFromEnvironment == null) {
//                logger.warn("Azure Functions Core Tools path is set to '$coreToolsPathFromConfiguration' in configuration, but could not be resolved.")
//            }
//
//            return coreToolsInfoFromEnvironment // can be null: if func.exe is configured but not found, user needs to check settings
//        }
//
//        // Try and resolve from full configured path
//        val coreToolsInfoFromConfiguration = resolveFromPath(File(coreToolsPathFromConfiguration))
//        if (coreToolsInfoFromConfiguration != null) {
//            return coreToolsInfoFromConfiguration
//        }
//
//        return null
//    }
//
//    private fun retrieveFromFeed(project: Project, azureFunctionsVersion: String, allowDownload: Boolean): FunctionsCoreToolsInfo? {
//
//        // Determine Azure Functions Core Tools from release feed
//        val coreToolsPathFromFeed = FunctionsCoreToolsManager.demandCoreToolsPathForVersion(
//                project, azureFunctionsVersion, Registry.get("azure.function_app.core_tools.feed.url").asString(), allowDownload)
//                ?: return null
//
//        val coreToolsInfoFromFeed = resolveFromPath(File(coreToolsPathFromFeed))
//        if (coreToolsInfoFromFeed != null) {
//            return coreToolsInfoFromFeed
//        }
//
//        return null
//    }
//
//    private fun resolveFromPath(funcCoreToolsPath: File): FunctionsCoreToolsInfo? {
//
//        val patchedFuncCoreToolsPath = patchCoreToolsPath(funcCoreToolsPath)
//
//        val coreToolsExecutablePath = if (SystemInfo.isWindows) {
//            patchedFuncCoreToolsPath.resolve("func.exe")
//        } else {
//            patchedFuncCoreToolsPath.resolve("func")
//        }
//
//        if (!coreToolsExecutablePath.exists()) {
//            return null
//        }
//
//        if (!coreToolsExecutablePath.canExecute()) {
//            logger.warn("Updating executable flag for $coreToolsExecutablePath...")
//            try {
//                coreToolsExecutablePath.setExecutable(true)
//            } catch (s: SecurityException) {
//                logger.error("Failed setting executable flag for $coreToolsExecutablePath", s)
//            }
//        }
//
//        return FunctionsCoreToolsInfo(patchedFuncCoreToolsPath.path, coreToolsExecutablePath.path)
//    }
//
//    private fun normalizeCoreToolsPath(funcCoreToolsPath: File): File {
//        // Normalize path - if the user appends func/func.cmd/func.exe, strip it off
//        return if (funcCoreToolsPath.isFile && funcCoreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)) {
//            funcCoreToolsPath.parentFile
//        } else {
//            funcCoreToolsPath
//        }
//    }
//
//    private fun patchCoreToolsPath(funcCoreToolsPath: File): File {
//
//        val normalizedFuncCoreToolsPath = normalizeCoreToolsPath(funcCoreToolsPath)
//
//        if (!SystemInfo.isWindows) return normalizedFuncCoreToolsPath
//
//        // Chocolatey and NPM have shim executables that are not .NET (and not debuggable).
//        // If it's a Chocolatey install or NPM install, rewrite the path to the tools path
//        // where the func executable is located.
//        //
//        // Logic is similar to com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver.resolveFunc()
//        val chocolateyCoreToolsPath = normalizedFuncCoreToolsPath.resolve("..").resolve("lib").resolve("azure-functions-core-tools").resolve("tools").normalize()
//        if (chocolateyCoreToolsPath.exists()) {
//            logger.info("Functions core tools path ${normalizedFuncCoreToolsPath.path} is Chocolatey-installed. Rewriting path to ${chocolateyCoreToolsPath.path}")
//            return chocolateyCoreToolsPath
//        }
//
//        val npmCoreToolsPath = normalizedFuncCoreToolsPath.resolve("..").resolve("node_modules").resolve("azure-functions-core-tools").resolve("bin").normalize()
//        if (npmCoreToolsPath.exists()) {
//            logger.info("Functions core tools path ${normalizedFuncCoreToolsPath.path} is NPM-installed. Rewriting path to ${npmCoreToolsPath.path}")
//            return npmCoreToolsPath
//        }
//
//        return normalizedFuncCoreToolsPath
//    }
//}