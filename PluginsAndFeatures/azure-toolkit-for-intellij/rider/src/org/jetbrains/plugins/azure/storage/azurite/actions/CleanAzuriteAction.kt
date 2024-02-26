/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.storage.azurite.actions

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.VcsShowConfirmationOption
import com.intellij.util.ui.ConfirmationDialog
import com.microsoft.intellij.configuration.AzureRiderSettings
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.storage.azurite.AzuriteService

class CleanAzuriteAction
    : AnAction(
        message("action.azurite.clean.name"),
        message("action.azurite.clean.description"),
        AllIcons.Actions.Rollback) {

    private val azuriteService = service<AzuriteService>()

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return

        val properties = PropertiesComponent.getInstance()
        val azuritePath = AzureRiderSettings.getAzuriteExecutable(properties)

        e.presentation.isEnabled = !azuriteService.isRunning && azuritePath.isNotEmpty() && AzureRiderSettings.getAzuriteWorkspacePath(properties, project).exists()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val properties = PropertiesComponent.getInstance()
        e.presentation.isEnabled = AzureRiderSettings.getAzuriteWorkspacePath(properties, project).exists()

        val shouldPerformClean = ConfirmationDialog.requestForConfirmation(VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION,
                project,
                message("action.azurite.clean.confirmation.message"),
                message("action.azurite.clean.confirmation.title"),
                CommonIcons.Azurite,
                message("action.azurite.clean.confirmation.confirm"),
                CommonBundle.getCancelButtonText()
        )

        if (shouldPerformClean) {
            ApplicationManager.getApplication().runWriteAction {
                azuriteService.clean(AzureRiderSettings.getAzuriteWorkspacePath(properties, project))
            }
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}