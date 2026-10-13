package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClientManager
import com.jetbrains.rider.environment.initializeAndGetEnvironment
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle.BICEP_EXTENSION
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettings
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettingsState.LanguageServerSource
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile

internal fun isLsAvailableFor(file: VirtualFile, project: Project): Boolean {
    if (file.extension != BICEP_EXTENSION) return false
    return getComputedLsValidityOrScheduleValidation(project) == LsValidity.VALID
}

internal fun getComputedLsValidityOrScheduleValidation(project: Project): LsValidity {
    val bicepService = BicepSchedulingService.getInstance(project)
    val currentLsValidity = bicepService.currentLsInfrastructureValidity
    if (currentLsValidity == LsValidity.YET_UNKNOWN) {
        bicepService.scheduleLsValidation()
    }
    return currentLsValidity
}

internal fun forceRunLanguageServer(project: Project) {
    LspClientManager.getInstance(project).startClientsIfNeeded(BicepLspSupportProvider::class.java)
}

internal fun findActiveBicepFileEditor(project: Project): Editor? {
    return FileEditorManager.getInstance(project).selectedTextEditor
        ?.takeIf { it.virtualFile?.extension == BICEP_EXTENSION }
}

internal fun findAllOpenedBicepFiles(project: Project): Sequence<VirtualFile> {
    return FileEditorManager.getInstance(project).openFiles.asSequence()
        .filter { file -> file.extension == BICEP_EXTENSION }
}

internal suspend fun prepareBicepServerLaunchCommandLine(project: Project): GeneralCommandLine {
    val settings = BicepSettings.getInstance(project)
    val infrastructureService = BicepInfrastructureService.getInstance(project)
    val bicepExecutablePath = when (settings.getLanguageServerSource()) {
        LanguageServerSource.LOCAL_FILE -> {
            val path = settings.getLanguageServerPath()
            require(!path.isNullOrBlank()) { "Bicep language server path is not specified" }
            Path.of(path)
        }

        LanguageServerSource.SPECIFIC_VERSION -> {
            val version = settings.getLanguageServerVersion()
            require(!version.isNullOrBlank()) { "Bicep language server version is not specified" }
            infrastructureService.findExecutablePath(BicepLS(version))
        }

        LanguageServerSource.LATEST -> {
            val version = infrastructureService.findLatestVersion()
                ?: error("No Bicep language server versions found")
            infrastructureService.findExecutablePath(BicepLS(version))
        }
    }
    if (!bicepExecutablePath.isRegularFile()) {
        throw NoSuchFileException(bicepExecutablePath.toString(), null, "Bicep language server file does not exist")
    }

    val riderEnvironment = project.initializeAndGetEnvironment()
    val dotnetExecutablePath = riderEnvironment.getRuntime().cliPath().absolutePathString()

    return GeneralCommandLine(dotnetExecutablePath, bicepExecutablePath.absolutePathString())
}
