package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.ui.AnimatedIcon
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class BicepInstallLsInfrastructureAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val schedulingService = BicepSchedulingService.getInstance(project)
    schedulingService.scheduleLsDownload()
    updateIconWhileLoading(e.presentation, schedulingService)
  }

  private fun updateIconWhileLoading(
    presentation: Presentation,
    schedulingService: BicepSchedulingService,
  ) {
    presentation.icon = AnimatedIcon.Default.INSTANCE
    schedulingService.coroutineScope.launch {
      schedulingService.isLsSetupInProgress.collect { isSetupInProgress ->
        if (!isSetupInProgress) {
          presentation.icon = AllIcons.Actions.Install
          coroutineContext.cancel()
        }
      }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.icon = if (BicepSchedulingService.getInstance(project).isLsSetupInProgress.value)
      AnimatedIcon.Default.INSTANCE
    else
      AllIcons.Actions.Install
  }
}