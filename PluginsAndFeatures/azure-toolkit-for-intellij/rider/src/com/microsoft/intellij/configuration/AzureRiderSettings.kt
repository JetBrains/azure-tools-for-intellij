/**
 * Copyright (c) 2018-2023 JetBrains s.r.o.
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

package com.microsoft.intellij.configuration

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsConstants
import org.jetbrains.plugins.azure.storage.azurite.Azurite
import java.io.File
import kotlin.io.path.absolutePathString

object AzureRiderSettings {

    private const val AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ" // See com.microsoft.intellij.AzureActionsListener

    // Dismiss notifications
    const val DISMISS_NOTIFICATION_AZURE_FUNCTIONS_MISSING_NUPKG = "DismissAzureFunctionsMissingNupkg"
    const val DISMISS_NOTIFICATION_AZURE_AD_REGISTER = "DismissAzureAdRegistration"

    // Functions
    const val PROPERTY_FUNCTIONS_CORETOOLS_PATHS = "AzureFunctionsCoreToolsPaths"
    const val PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH = "AzureFunctionsCoreToolsDownloadPath"
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    val VALUE_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH: String = PathUtil.toSystemIndependentName(
            com.microsoft.azuretools.azurecommons.util.FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER).resolve("AzureFunctionsCoreTools").absolutePathString())

    @Deprecated("To be removed with 2022.3")
    const val PROPERTY_FUNCTIONS_MIGRATE_CORETOOLS_PATH_NOTIFICATION = "AzureFunctionsCoreToolsPath_Migration_Notify"

    data class AzureCoreToolsPathEntry(var functionsVersion: String, var coreToolsPath: String) {

        fun toStringEntry() = "$functionsVersion|$coreToolsPath"

        companion object {

            fun fromStringEntry(entry: String): AzureCoreToolsPathEntry? {
                val segments = entry.split('|')

                if (segments.size == 1) return AzureCoreToolsPathEntry(segments[0], "")
                if (segments.size == 2) return AzureCoreToolsPathEntry(segments[0], segments[1])
                return null
            }
        }
    }

    fun getAzureCoreToolsPathEntries(properties: PropertiesComponent) : List<AzureCoreToolsPathEntry> {

        val coreToolsFromConfiguration = properties.getList(PROPERTY_FUNCTIONS_CORETOOLS_PATHS)
                ?.mapNotNull { AzureCoreToolsPathEntry.fromStringEntry(it) }
                ?: emptyList()

        val coreTools = FunctionsCoreToolsConstants.FUNCTIONS_CORETOOLS_KNOWN_SUPPORTED_VERSIONS.map { functionsVersion ->
            coreToolsFromConfiguration.firstOrNull { it.functionsVersion.equals(functionsVersion, ignoreCase = true) }
                    ?: AzureCoreToolsPathEntry(functionsVersion, "")
        }

        return coreTools
    }

    fun setAzureCoreToolsPathEntries(properties: PropertiesComponent, pathEntries: List<AzureCoreToolsPathEntry>) {

        properties.setList(
                PROPERTY_FUNCTIONS_CORETOOLS_PATHS,
                pathEntries.map { it.toStringEntry() })
    }

    // Azurite
    const val PROPERTY_AZURITE_PATH = "AzureAzuritePath"

    const val PROPERTY_AZURITE_BLOB_HOST = "AzureAzuriteBlobHost"
    const val VALUE_AZURITE_BLOB_HOST_DEFAULT = "127.0.0.1"
    const val PROPERTY_AZURITE_BLOB_PORT = "AzureAzuriteBlobPort"
    const val VALUE_AZURITE_BLOB_PORT_DEFAULT = "10000"

    const val PROPERTY_AZURITE_QUEUE_HOST = "AzureAzuriteQueueHost"
    const val VALUE_AZURITE_QUEUE_HOST_DEFAULT = "127.0.0.1"
    const val PROPERTY_AZURITE_QUEUE_PORT = "AzureAzuriteQueuePort"
    const val VALUE_AZURITE_QUEUE_PORT_DEFAULT = "10001"

    const val PROPERTY_AZURITE_TABLE_HOST = "AzureAzuriteTableHost"
    const val VALUE_AZURITE_TABLE_HOST_DEFAULT = "127.0.0.1"
    const val PROPERTY_AZURITE_TABLE_PORT = "AzureAzuriteTablePort"
    const val VALUE_AZURITE_TABLE_PORT_DEFAULT = "10002"

    const val PROPERTY_AZURITE_LOCATION_MODE = "AzureAzuriteLocationMode"
    const val PROPERTY_AZURITE_LOCATION = "AzureAzuriteLocation"

    enum class AzuriteLocationMode(val description: String) {
        Managed(RiderAzureBundle.message("settings.azurite.row.general.workspace.use_managed")),
        Project(RiderAzureBundle.message("settings.azurite.row.general.workspace.use_project")),
        Custom(RiderAzureBundle.message("settings.azurite.row.general.workspace.use_custom"))
    }

    const val PROPERTY_AZURITE_LOOSE_MODE = "AzureAzuriteLooseMode"

    const val PROPERTY_AZURITE_CERT_PATH = "AzureAzuriteCertPath"
    const val PROPERTY_AZURITE_CERT_KEY_PATH = "AzureAzuriteCertKeyPath"
    const val PROPERTY_AZURITE_CERT_PASSWORD = "AzureAzuriteCertPassword"

    fun getAzuriteExecutable(properties: PropertiesComponent): String {
        var azuritePath = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_PATH)

        if (azuritePath.isNullOrEmpty()) {
            if (SystemInfo.isWindows) {
                azuritePath = PathEnvironmentVariableUtil.findInPath("azurite.cmd")?.absolutePath

                if (azuritePath.isNullOrEmpty()) {
                    azuritePath = PathEnvironmentVariableUtil.findInPath("azurite.exe")?.absolutePath
                }
            } else {
                azuritePath = PathEnvironmentVariableUtil.findInPath("azurite")?.absolutePath
            }

            if (!File(azuritePath).exists()) {
                azuritePath = null
            }
        }

        return FileUtil.toSystemDependentName(azuritePath ?: "")
    }
    fun getAzuriteWorkspaceMode(properties: PropertiesComponent): AzuriteLocationMode {
        val mode = properties.getValue(PROPERTY_AZURITE_LOCATION_MODE)
                ?: return AzuriteLocationMode.Managed

        return AzuriteLocationMode.valueOf(mode)
    }

    fun getAzuriteWorkspacePath(properties: PropertiesComponent, project: Project): File =
        when(getAzuriteWorkspaceMode(properties)) {
            AzuriteLocationMode.Managed -> {
                val workspace = File(PathManager.getConfigPath(), Azurite.ManagedPathSuffix)
                workspace.mkdir()
                workspace
            }
            AzuriteLocationMode.Project -> {
                val workspace = File(project.basePath, Azurite.ProjectPathSuffix)
                workspace.mkdir()
                workspace
            }
            AzuriteLocationMode.Custom -> {
                val customPath = properties.getValue(PROPERTY_AZURITE_LOCATION)
                if (customPath.isNullOrEmpty()) {
                    val workspace = File(PathManager.getConfigPath(), Azurite.ManagedPathSuffix)
                    workspace.mkdir()
                    workspace
                } else {
                    val workspace = File(customPath)
                    workspace
                }
            }
        }
}
