/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.projectTemplating

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.impl.DummyProject
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.use
import com.jetbrains.rider.projectView.projectTemplates.providers.RiderProjectTemplateProvider
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsConstants
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsConstants.FUNCTIONS_CORETOOLS_LATEST_SUPPORTED_VERSION
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import org.jetbrains.plugins.azure.orWhenNullOrEmpty
import org.jetbrains.plugins.azure.util.isFunctionCoreToolsExecutable
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

object FunctionsCoreToolsTemplateManager {

    private val logger = Logger.getInstance(FunctionsCoreToolsTemplateManager::class.java)

    private val netIsolatedPath = Path("net-isolated")

    fun areRegistered(): Boolean {
        val coreToolFolder = getCoreToolFolder() ?: return false

        return RiderProjectTemplateProvider
            .getUserTemplateSources()
            .any { (isFunctionProjectTemplate(it.toPath(), coreToolFolder)) && it.exists() }
    }

    fun tryReload() {

        // Determine core tools info for latest supported Azure Functions version
        val coreToolsInfo = DummyProject.getInstance().use { dummyProject ->
            FunctionsCoreToolsInfoProvider.retrieveForVersion(
                    dummyProject, FunctionsCoreToolsConstants.FUNCTIONS_CORETOOLS_LATEST_SUPPORTED_VERSION, allowDownload = false)
        } ?: return

        removePreviousTemplates(coreToolsInfo.coreToolsPath)

        // Add available templates
        val templateFolders = listOf(
                coreToolsInfo.coreToolsPath.resolve("templates"), // Default worker
                coreToolsInfo.coreToolsPath.resolve("templates").resolve("net6-isolated"), // Isolated worker - .NET 6
                coreToolsInfo.coreToolsPath.resolve("templates").resolve("net-isolated")   // Isolated worker - .NET 5 - .NET 8
        ).filter { it.exists() }

        for (templateFolder in templateFolders) {
            try {
                val templateFiles = templateFolder
                    .listDirectoryEntries()
                    .filter { isFunctionProjectTemplate(it, coreToolsInfo.coreToolsPath) }

                logger.info("Found ${templateFiles.size} function template(s) in $templateFolder")

                templateFiles.forEach { file ->
                    RiderProjectTemplateProvider.addUserTemplateSource(file.toFile())
                }
            } catch (e: Exception) {
                logger.error("Could not register project templates from $templateFolder", e)
            }
        }
    }

    private fun getCoreToolFolder(): Path? {
        val properties = PropertiesComponent.getInstance()

        val toolPathEntries = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)
        val toolPathFromConfiguration = toolPathEntries
            .firstOrNull {
                it.functionsVersion.equals(FUNCTIONS_CORETOOLS_LATEST_SUPPORTED_VERSION, ignoreCase = true)
            }
            ?.coreToolsPath
            ?: return null

        if (isFunctionCoreToolsExecutable(toolPathFromConfiguration)) {
            val toolPathFromEnvironment = FunctionCliResolver.resolveFunc()
            return Path(toolPathFromEnvironment).parent
        }

        if (toolPathFromConfiguration.isNotEmpty()) {
            return Path(toolPathFromConfiguration).parent
        }

        val downloadRoot = properties.getValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH)
            .orWhenNullOrEmpty(AzureRiderSettings.VALUE_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH)

        return Path(downloadRoot)
    }

    private fun isFunctionProjectTemplate(path: Path?, coreToolPath: Path): Boolean {
        if (path == null) return false
        if (!path.nameWithoutExtension.startsWith("projectTemplates.", true) ||
            !path.extension.equals("nupkg", true)
        ) return false

        return path.startsWith(coreToolPath)
    }

    private fun removePreviousTemplates(coreToolsPath: Path) {
        val templateSources = RiderProjectTemplateProvider
            .getUserTemplateSources()
            .map { it.toPath() }
        templateSources.forEach {
            if (it.startsWith(coreToolsPath)) {
                RiderProjectTemplateProvider.removeUserTemplateSource(it.toFile())
            } else if (it.contains(netIsolatedPath)) {
                val index = it.lastIndexOf(netIsolatedPath)
                val prefix = it.root?.resolve(it.subpath(0, index)) ?: return@forEach
                val sourcesToRemove = templateSources.filter { ts -> ts.startsWith(prefix) }

                sourcesToRemove.forEach { str ->
                    RiderProjectTemplateProvider.removeUserTemplateSource(str.toFile())
                }
            }
        }
    }
}