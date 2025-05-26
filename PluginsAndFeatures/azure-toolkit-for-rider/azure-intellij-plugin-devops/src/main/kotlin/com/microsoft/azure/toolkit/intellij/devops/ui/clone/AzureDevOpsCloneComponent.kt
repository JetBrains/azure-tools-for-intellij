package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.async.nestedDisposable
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.util.bindContentIn
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.components.panels.Wrapper
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneLoginViewModel
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneRepositoriesViewModel
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.swing.JComponent

internal class AzureDevOpsCloneComponent(
    private val project: Project,
    parentCs: CoroutineScope,
    private val vm: AzureDevOpsCloneViewModel,
) : VcsCloneDialogExtensionComponent() {
    private val cs: CoroutineScope = parentCs.childScope("Azure Devops Clone Component", Dispatchers.Main)

    private val wrapper: Wrapper = Wrapper().apply {
        bindContentIn(cs, vm.panelVm) { panelVm ->
            val innerCs = this
            when (panelVm) {
                is AzureDevOpsCloneLoginViewModel -> AzureDevOpsCloneLoginComponentFactory.create(
                    innerCs,
                    panelVm,
                    this@AzureDevOpsCloneComponent.vm
                )

                is AzureDevOpsCloneRepositoriesViewModel -> AzureDevOpsCloneRepositoriesComponentFactory.create(
                    project, innerCs, panelVm, this@AzureDevOpsCloneComponent.vm
                ).also { panel ->
                    panel.registerValidators(innerCs.nestedDisposable())

                    innerCs.launchNow {
                        panelVm.selectedUrl.collectLatest { selectedUrl ->
                            val isUrlSelected = selectedUrl != null
                            dialogStateListener.onOkActionEnabled(isUrlSelected)
                        }
                    }

                    innerCs.launchNow {
                        panelVm.listVm.allItems.collectLatest {
                            dialogStateListener.onListItemChanged()
                        }
                    }

                    innerCs.launch {
                        yield()
                        CollaborationToolsUIUtil.focusPanel(panel)
                    }
                }
            }
        }
    }

    override fun getView(): JComponent {
        return wrapper
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        this.vm.doClone(checkoutListener)
    }

    override fun doValidateAll(): List<ValidationInfo> =
        (wrapper.targetComponent as? DialogPanel)?.validationsOnApply?.values?.flatten()?.mapNotNull {
            it.validate()
        } ?: emptyList()

    override fun onComponentSelected() {
        dialogStateListener.onOkActionNameChanged("Clone")
        CollaborationToolsUIUtil.focusPanel(wrapper.targetComponent)
    }

}
