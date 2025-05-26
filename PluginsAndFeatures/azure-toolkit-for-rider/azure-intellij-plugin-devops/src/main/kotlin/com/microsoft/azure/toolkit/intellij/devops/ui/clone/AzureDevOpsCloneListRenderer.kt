package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.util.swingAction
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneRepositoriesListViewModel
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneViewModel
import org.jetbrains.annotations.Nls
import javax.swing.JList

internal class AzureDevOpsCloneListRenderer(
    private val cloneVm: AzureDevOpsCloneViewModel,
    private val vm: AzureDevOpsCloneRepositoriesListViewModel
) : ColoredListCellRenderer<AzureDevOpsCloneListItem>() {
    override fun customizeCellRenderer(
        list: JList<out AzureDevOpsCloneListItem>,
        item: AzureDevOpsCloneListItem,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        clear()
        when (item) {
            is AzureDevOpsCloneListItem.Repository -> append(item.repository.name)
            is AzureDevOpsCloneListItem.ErrorItem -> {
                val cloneError = item.error

                val action = swingAction(cloneError.name(), cloneError.performAction())

                append(cloneError.message(), SimpleTextAttributes.ERROR_ATTRIBUTES)
                append(" ")
                append(cloneError.name(), SimpleTextAttributes.LINK_ATTRIBUTES, action)
            }
        }
    }

    private fun AzureDevOpsCloneException.message(): @Nls String = when (this) {
        is AzureDevOpsCloneException.ConnectionError -> CollaborationToolsBundle.message("error.connection.error")
        is AzureDevOpsCloneException.MissingAccessToken -> CollaborationToolsBundle.message("account.token.missing")
        is AzureDevOpsCloneException.RevokedToken -> CollaborationToolsBundle.message("http.status.error.refresh.token")
        is AzureDevOpsCloneException.Unknown -> message
    }

    private fun AzureDevOpsCloneException.name(): @Nls String = when (this) {
        is AzureDevOpsCloneException.MissingAccessToken,
        is AzureDevOpsCloneException.RevokedToken,
            -> CollaborationToolsBundle.message("login.again.action.text")

        is AzureDevOpsCloneException.ConnectionError,
        is AzureDevOpsCloneException.Unknown,
            -> CollaborationToolsBundle.message("clone.dialog.error.retry")
    }

    private fun AzureDevOpsCloneException.performAction(): (Any) -> Unit {
        when (this) {
            is AzureDevOpsCloneException.MissingAccessToken,
            is AzureDevOpsCloneException.RevokedToken,
                -> return { cloneVm.switchToLoginPanel(account) }

            is AzureDevOpsCloneException.ConnectionError,
            is AzureDevOpsCloneException.Unknown,
                -> return { vm.reload(account) }
        }
    }

}