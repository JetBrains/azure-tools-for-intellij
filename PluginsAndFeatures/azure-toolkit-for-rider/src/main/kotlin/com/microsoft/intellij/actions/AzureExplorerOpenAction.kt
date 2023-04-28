package com.microsoft.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory

class AzureExplorerOpenAction : AzureAnAction() {
    override fun onActionPerformed(event: AnActionEvent, operation: Operation?): Boolean {
        val project = CommonDataKeys.PROJECT.getData(event.dataContext) ?: return false
        ToolWindowManager
            .getInstance(project)
            .getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW)
            ?.activate(null)
        return true
    }
}