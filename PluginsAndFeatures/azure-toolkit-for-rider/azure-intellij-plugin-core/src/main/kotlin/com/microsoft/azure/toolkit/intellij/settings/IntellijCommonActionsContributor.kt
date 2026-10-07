/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.settings

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

class IntellijCommonActionsContributor : IActionsContributor {
    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS, { _, _ -> true }) { _, e: AnActionEvent ->
            val project = e.project
            if (project != null){
                AzureTaskManager.getInstance().runLater { openSettingsDialog(project) }
            } else {
                DataManager.getInstance().dataContextFromFocusAsync
                        .onSuccess { dataContext -> AzureTaskManager.getInstance().runLater { openSettingsDialog(dataContext.getData(CommonDataKeys.PROJECT)) } }
                        .onError { AzureTaskManager.getInstance().runLater { openSettingsDialog(null) } }
            }
        }
    }

    private fun openSettingsDialog(project: Project?) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureConfigurable::class.java)
    }
}
