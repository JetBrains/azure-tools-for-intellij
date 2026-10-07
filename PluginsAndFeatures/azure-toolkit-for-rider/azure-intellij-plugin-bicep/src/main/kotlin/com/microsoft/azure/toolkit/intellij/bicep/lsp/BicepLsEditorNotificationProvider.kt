package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepSettings
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls
import java.util.function.Function
import javax.swing.JComponent

internal class BicepLsEditorNotificationProvider : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (file.extension != BicepBundle.BICEP_EXTENSION
        || BicepSchedulingService.getInstance(project).isLsSetupInProgress.value
        || BicepSettings.getInstance(project).shouldIgnoreLsDownloadSuggestion()) {
      return null
    }

    return if (getComputedLsValidityOrScheduleValidation(project) == LsValidity.INVALID) {
      Function { createNotification(project) }
    }
    else {
      null
    }
  }

  private fun createNotification(project: Project): EditorNotificationPanel? {
    return EditorNotificationPanel().text(BicepBundle.message("editor.notification.install.ls.title"))
      .apply {
        createActionLabel(BicepBundle.message("editor.notification.answer.yes")) {
          BicepSchedulingService.getInstance(project).scheduleLsDownload()
          scheduleReloadEditorNotifications(project)
        }
        createActionLabel(BicepBundle.message("editor.notification.answer.no")) {
          BicepSettings.getInstance(project).rememberToIgnoreLsDownloadSuggestion()
          scheduleReloadEditorNotifications(project)
        }
      }
  }

  private fun scheduleReloadEditorNotifications(project: Project) {
    BicepSchedulingService.getInstance(project).coroutineScope.launch {
      reloadEditorNotifications(project)
    }
  }
}

internal suspend fun reloadEditorNotifications(project: Project) {
  withContext(Dispatchers.EDT) {
    findAllOpenedBicepFiles(project)
      .forEach { file -> EditorNotifications.getInstance(project).updateNotifications(file) }
  }
}

internal suspend fun displayPopupWithServerInfo(project: Project, @Nls message: String) {
  withContext(Dispatchers.EDT) {
    findActiveBicepFileEditor(project)
      ?.let { editor -> HintManager.getInstance().showInformationHint(editor, message) }
  }
}