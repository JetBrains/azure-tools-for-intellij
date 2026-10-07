/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rider.azureFunctions.settings.AzureFunctionSettingStateService
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteLocationMode
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteSettings

@Suppress("UnstableApiUsage")
internal class AzuriteSettingsMigrationStartupActivity : ProjectActivity {
    companion object {
        private const val AZURE_FUNCTIONS_STORAGE_SETTINGS_MIGRATED = "Rider.Azure.Toolkit.Storage.Settings.Migrated"
    }

    override suspend fun execute(project: Project) {
        val properties = PropertiesComponent.getInstance()

        if (properties.getBoolean(AZURE_FUNCTIONS_STORAGE_SETTINGS_MIGRATED)) return
        properties.setValue(AZURE_FUNCTIONS_STORAGE_SETTINGS_MIGRATED, true)

        val previousSettings = AzuriteSettings.getInstance(project)
        val newSettings = AzureFunctionSettingStateService.getInstance()

        if (newSettings.azuriteExecutablePath.isEmpty()) {
            newSettings.azuriteExecutablePath = previousSettings.executablePath
        }

        if (newSettings.azuriteLocationMode == com.jetbrains.rider.azureFunctions.azurite.AzuriteLocationMode.Managed) {
            when (previousSettings.locationMode) {
                AzuriteLocationMode.Managed -> newSettings.azuriteLocationMode = com.jetbrains.rider.azureFunctions.azurite.AzuriteLocationMode.Managed
                AzuriteLocationMode.Project -> newSettings.azuriteLocationMode = com.jetbrains.rider.azureFunctions.azurite.AzuriteLocationMode.Project
                AzuriteLocationMode.Custom -> newSettings.azuriteLocationMode = com.jetbrains.rider.azureFunctions.azurite.AzuriteLocationMode.Custom
            }
        }

        if (newSettings.azuriteWorkspacePath.isEmpty()) {
            newSettings.azuriteWorkspacePath = previousSettings.workspacePath
        }

        if (newSettings.azuriteLooseMode == false) {
            newSettings.azuriteLooseMode = previousSettings.looseMode
        }

        if (newSettings.azuriteSkipApiVersionCheck == false) {
            newSettings.azuriteSkipApiVersionCheck = previousSettings.skipApiVersionCheck
        }

        if (newSettings.azuriteShowService == true) {
            newSettings.azuriteShowService = previousSettings.showAzuriteService
        }

        if (newSettings.azuriteCheckExecutable == true) {
            newSettings.azuriteCheckExecutable = previousSettings.checkAzuriteExecutable
        }

        if (newSettings.azuriteBlobHost == "127.0.0.1") {
            newSettings.azuriteBlobHost = previousSettings.blobHost
        }

        if (newSettings.azuriteBlobPort == 10000) {
            newSettings.azuriteBlobPort = previousSettings.blobPort
        }

        if (newSettings.azuriteQueueHost == "127.0.0.1") {
            newSettings.azuriteQueueHost = previousSettings.queueHost
        }

        if (newSettings.azuriteQueuePort == 10001) {
            newSettings.azuriteQueuePort = previousSettings.queuePort
        }

        if (newSettings.azuriteTableHost == "127.0.0.1") {
            newSettings.azuriteTableHost = previousSettings.tableHost
        }

        if (newSettings.azuriteTablePort == 10002) {
            newSettings.azuriteTablePort = previousSettings.tablePort
        }

        if (newSettings.azuriteBasicOAuth == false) {
            newSettings.azuriteBasicOAuth = previousSettings.basicOAuth
        }

        if (newSettings.azuriteCertificatePath.isEmpty()) {
            newSettings.azuriteCertificatePath = previousSettings.certificatePath
        }

        if (newSettings.azuriteCertificateKeyPath.isEmpty()) {
            newSettings.azuriteCertificateKeyPath = previousSettings.certificateKeyPath
        }

        if (newSettings.azuriteCertificatePassword.isEmpty()) {
            newSettings.azuriteCertificatePassword = previousSettings.certificatePassword
        }
    }
}