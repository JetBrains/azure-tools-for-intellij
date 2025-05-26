package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.intellij.collaboration.async.cancelledWith
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneViewModelImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import javax.swing.Icon

class AzureDevOpsCloneDialogExtension : VcsCloneDialogExtension {
    override fun getIcon(): Icon = AllIcons.Gutter.Colors

    override fun getName(): String = SERVICE_DISPLAY_NAME

    companion object {
        private const val SERVICE_DISPLAY_NAME: @NlsSafe String = "Azure DevOps"
    }

    override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
        val accounts = service<AzureDevOpsAccountManager>().accountsState.value
        return if (accounts.isEmpty())
            listOf(VcsCloneDialogExtensionStatusLine.greyText(CollaborationToolsBundle.message("clone.dialog.label.no.accounts")))
        else
            accounts.map { account ->
                VcsCloneDialogExtensionStatusLine.greyText(account.name)
            }
    }

    override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent =
        project.service<AzureDevOpsCloneDialogExtensionComponentFactory>().create(modalityState)
}

@Service(Service.Level.PROJECT)
internal class AzureDevOpsCloneDialogExtensionComponentFactory(
    private val project: Project, private val cs: CoroutineScope
) {
    fun create(modalityState: ModalityState): VcsCloneDialogExtensionComponent {
        val vmCs = cs.childScope(javaClass.name, modalityState.asContextElement())
        val vm = AzureDevOpsCloneViewModelImpl(
            project, vmCs + Dispatchers.Default, service<AzureDevOpsAccountManager>()
        )

        val componentCs = vmCs.childScope("Azure DevOps clone dialog component")
        val component = AzureDevOpsCloneComponent(project, componentCs, vm).also {
            vmCs.cancelledWith(it)
        }
        return component
    }
}
