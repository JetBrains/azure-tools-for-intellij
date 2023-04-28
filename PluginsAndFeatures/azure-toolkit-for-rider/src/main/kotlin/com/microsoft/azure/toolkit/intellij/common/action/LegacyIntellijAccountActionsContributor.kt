package com.microsoft.azure.toolkit.intellij.common.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.wm.ToolWindowManager
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.intellij.common.auth.AzureSignInAction
import com.microsoft.azure.toolkit.intellij.common.subscription.SelectSubscriptionsAction
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.IAccountActions
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.intellij.AzureConfigurable
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory

class LegacyIntellijAccountActionsContributor : IActionsContributor {
    companion object {
        private fun openAzureExplorer(e: AnActionEvent) {
            val toolWindow = ToolWindowManager.getInstance(e.project!!)
                .getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW)
            if (toolWindow != null && !toolWindow.isVisible) {
                AzureTaskManager.getInstance().runLater(toolWindow::show)
            }
        }

        private fun openSettingsDialog(project: Project?) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AzureConfigurable::class.java)
        }
    }

    override fun registerActions(am: AzureActionManager?) {
        Action(Action.REQUIRE_AUTH)
            .withLabel("Authorize")
            .withHandler { r: Runnable, e: AnActionEvent -> AzureSignInAction.requireSignedIn(e.project, r) }
            .withAuthRequired(false)
            .register(am)

        Action(Action.AUTHENTICATE)
            .withLabel("Sign in")
            .withHandler { _: Any?, e: AnActionEvent ->
                val az = Azure.az(AzureAccount::class.java)
                if (az.isLoggedIn) az.logout()
                AzureSignInAction.authActionPerformed(e.project)
            }
            .withAuthRequired(false)
            .register(am)

        Action(IAccountActions.SELECT_SUBS)
            .withLabel("Select Subscriptions")
            .withHandler { _: Any?, e: AnActionEvent -> SelectSubscriptionsAction.selectSubscriptions(e.project) }
            .withAuthRequired(false)
            .register(am)
    }

    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(
            ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS,
            { _, _ -> true }
        ) { _, e: AnActionEvent ->
            val project = if (e.project != null) e.project else {
                val openProjects = ProjectManagerEx.getInstanceEx().openProjects
                if (openProjects.isEmpty()) null else openProjects[0]
            }
            val title = OperationBundle.description("user/common.open_azure_settings")
            AzureTaskManager.getInstance().runLater(AzureTask(title) { openSettingsDialog(project) })
        }

        am.registerHandler(
            ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER,
            { _, _ -> true }
        ) { _, e: AnActionEvent -> openAzureExplorer(e) }
    }
}