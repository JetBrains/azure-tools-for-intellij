/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.storage.azurite.settings

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.findOrCreateDirectory
import com.jetbrains.rider.projectView.solutionPath
import java.nio.file.Path
import kotlin.io.path.Path

@State(
    name = "com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteSettings"
)
@Service(Service.Level.PROJECT)
class AzuriteSettings(private val project: Project) : SimplePersistentStateComponent<AzuriteSettingsState>(AzuriteSettingsState()) {
    companion object {
        private const val AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ"
        private const val AZURITE_FOLDER = "azurite"
        private const val PROJECT_AZURITE_FOLDER = ".idea/azurite"

        fun getInstance(project: Project) = project.service<AzuriteSettings>()
    }

    var executablePath
        get() = state.executablePath ?: ""
        set(value) {
            state.executablePath = FileUtil.toSystemIndependentName(value)
        }

    var locationMode
        get() = state.locationMode
        set(value) {
            state.locationMode = value
        }

    var workspacePath
        get() = state.workspacePath ?: ""
        set(value) {
            state.workspacePath = FileUtil.toSystemIndependentName(value)
        }

    var looseMode
        get() = state.looseMode
        set(value) {
            state.looseMode = value
        }

    var skipApiVersionCheck
        get() = state.skipApiVersionCheck
        set(value) {
            state.skipApiVersionCheck = value
        }

    var showAzuriteService
        get() = state.showAzuriteService
        set(value) {
            state.showAzuriteService = value
        }

    var checkAzuriteExecutable
        get() = state.checkAzuriteExecutable
        set(value) {
            state.checkAzuriteExecutable = value
        }

    var blobHost
        get() = state.blobHost ?: ""
        set(value) {
            state.blobHost = value
        }

    var blobPort
        get() = state.blobPort
        set(value) {
            state.blobPort = value
        }

    var queueHost
        get() = state.queueHost ?: ""
        set(value) {
            state.queueHost = value
        }

    var queuePort
        get() = state.queuePort
        set(value) {
            state.queuePort = value
        }

    var tableHost
        get() = state.tableHost ?: ""
        set(value) {
            state.tableHost = value
        }

    var tablePort
        get() = state.tablePort
        set(value) {
            state.tablePort = value
        }

    var basicOAuth
        get() = state.basicOAuth
        set(value) {
            state.basicOAuth = value
        }

    var certificatePath
        get() = state.certificatePath ?: ""
        set(value) {
            state.certificatePath = FileUtil.toSystemIndependentName(value)
        }

    var certificateKeyPath
        get() = state.certificateKeyPath ?: ""
        set(value) {
            state.certificateKeyPath = FileUtil.toSystemIndependentName(value)
        }

    var certificatePassword
        get() = state.certificatePassword ?: ""
        set(value) {
            state.certificatePassword = value
        }

    fun getAzuriteExecutablePath(): Path? {
        val azuritePath = executablePath

        val path = azuritePath.ifEmpty {
            val environmentPath = if (SystemInfo.isWindows) {
                val azuriteCmd = PathEnvironmentVariableUtil.findInPath("azurite.cmd")?.absolutePath
                if (azuriteCmd.isNullOrEmpty()) {
                    PathEnvironmentVariableUtil.findInPath("azurite.exe")?.absolutePath
                } else {
                    azuriteCmd
                }
            } else {
                PathEnvironmentVariableUtil.findInPath("azurite")?.absolutePath
            }

            if (!environmentPath.isNullOrEmpty()) {
                executablePath = environmentPath
            }

            environmentPath
        } ?: return null

        return Path(path)
    }

    fun getAzuriteWorkspacePath(): Path = when (locationMode) {
        AzuriteLocationMode.Managed -> {
            val workspacePath = Path.of(System.getProperty("user.home"))
                .resolve(AZURE_TOOLS_FOLDER)
                .resolve(AZURITE_FOLDER)
            workspacePath.findOrCreateDirectory()
            workspacePath
        }

        AzuriteLocationMode.Project -> {
            val workspacePath = Path(project.basePath ?: project.solutionPath)
                .resolve(PROJECT_AZURITE_FOLDER)
            workspacePath.findOrCreateDirectory()
            workspacePath
        }

        AzuriteLocationMode.Custom -> {
            val customPath = workspacePath
            if (customPath.isNotEmpty()) {
                Path(customPath)
            } else {
                val workspacePath = Path.of(System.getProperty("user.home"))
                    .resolve(AZURE_TOOLS_FOLDER)
                    .resolve(AZURITE_FOLDER)
                workspacePath.findOrCreateDirectory()
                workspacePath
            }
        }
    }
}