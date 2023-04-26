package com.microsoft.intellij.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware

class AzurePopupGroup: DefaultActionGroup(), DumbAware {
    override fun update(e: AnActionEvent) {
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}