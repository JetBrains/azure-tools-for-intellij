package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import com.jetbrains.rider.environment.RiderEnvironment
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle.BICEP_EXTENSION
import kotlin.io.path.absolutePathString

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
    LspServerManager.getInstance(project).startServersIfNeeded(BicepLspSupportProvider::class.java)
}

internal fun findActiveBicepFileEditor(project: Project): Editor? {
    return FileEditorManager.getInstance(project).selectedTextEditor
        ?.takeIf { it.virtualFile?.extension == BICEP_EXTENSION }
}

internal fun findAllOpenedBicepFiles(project: Project): Sequence<VirtualFile> {
    return FileEditorManager.getInstance(project).openFiles.asSequence()
        .filter { file -> file.extension == BICEP_EXTENSION }
}

internal fun prepareBicepServerLaunchCommandLine(riderEnvironment: RiderEnvironment): GeneralCommandLine {
    val dotnetExecutablePath = riderEnvironment.getRuntime().cliPath().absolutePathString()
    val bicepExecutablePath = BicepLS.findExecutablePath().absolutePathString()

    return GeneralCommandLine(dotnetExecutablePath, bicepExecutablePath)
}