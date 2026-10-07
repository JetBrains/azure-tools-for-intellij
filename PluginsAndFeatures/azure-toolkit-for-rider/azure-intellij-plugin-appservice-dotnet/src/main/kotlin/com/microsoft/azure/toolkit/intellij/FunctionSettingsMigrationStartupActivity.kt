/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rider.azureFunctions.settings.AzureFunctionSettingStateService
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings.Companion.AZURE_TOOLS_FOLDER

@Suppress("UnstableApiUsage")
internal class FunctionSettingsMigrationStartupActivity : ProjectActivity {
    companion object {
        private const val AZURE_FUNCTIONS_SETTINGS_MIGRATED = "Rider.Azure.Toolkit.Functions.Settings.Migrated"
    }

    override suspend fun execute(project: Project) {
        val properties = PropertiesComponent.getInstance()

        if (properties.getBoolean(AZURE_FUNCTIONS_SETTINGS_MIGRATED)) return
        properties.setValue(AZURE_FUNCTIONS_SETTINGS_MIGRATED, true)

        val previousSettings = AzureFunctionSettings.getInstance()
        val newSettings = AzureFunctionSettingStateService.getInstance()

        if (newSettings.coreToolPath.isEmpty()) {
            val v4Path =
                previousSettings.azureCoreToolsPathEntries.firstOrNull { it.functionsVersion == "v4" }?.coreToolsPath
            if (!v4Path.isNullOrEmpty()) {
                newSettings.coreToolPath = v4Path
            }
        }

        if (newSettings.coreToolsDownloadPath.isEmpty() &&
            !previousSettings.functionDownloadPath.contains(AZURE_TOOLS_FOLDER)
        ) {
            newSettings.coreToolsDownloadPath = previousSettings.functionDownloadPath
        }

        if (newSettings.checkForMissingNuGetPackages == true) {
            newSettings.checkForMissingNuGetPackages = previousSettings.checkForFunctionMissingPackages
        }
    }
}