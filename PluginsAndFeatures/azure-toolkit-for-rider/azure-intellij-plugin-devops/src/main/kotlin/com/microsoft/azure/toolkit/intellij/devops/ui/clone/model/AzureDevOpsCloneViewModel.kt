package com.microsoft.azure.toolkit.intellij.devops.ui.clone.model

import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal interface AzureDevOpsCloneViewModel {
    val panelVm: SharedFlow<AzureDevOpsClonePanelViewModel>

    fun switchToLoginPanel(account: AzureDevOpsAccount? = null)

    fun switchToRepositoriesPanel()

    fun doClone(checkoutListener: CheckoutProvider.Listener)
}

internal class AzureDevOpsCloneViewModelImpl(
    project: Project,
    parentCs: CoroutineScope,
    accountManager: AzureDevOpsAccountManager
) : AzureDevOpsCloneViewModel {
    private val cs: CoroutineScope = parentCs.childScope("Azure DevOps Clone VM")

    private val loginVm = AzureDevOpsCloneLoginViewModelImpl(cs, accountManager)
    private val repositoriesVm = AzureDevOpsCloneRepositoriesViewModelImpl(project, cs, accountManager)

    private val accounts: SharedFlow<Set<AzureDevOpsAccount>> = accountManager.accountsState

    private val _panelVm: MutableStateFlow<AzureDevOpsClonePanelViewModel> = MutableStateFlow(repositoriesVm)
    override val panelVm: SharedFlow<AzureDevOpsClonePanelViewModel> = _panelVm.asSharedFlow()

    init {
        cs.launch {
            accounts.collectLatest { accounts ->
                if (accounts.isNotEmpty()) switchToRepositoriesPanel() else switchToLoginPanel(null)
            }
        }

        cs.launch {
            loginVm.tokenLoginModel.loginState.collectLatest { loginState ->
                if (loginState is LoginModel.LoginState.Connected) {
                    switchToRepositoriesPanel()
                }
            }
        }
    }

    override fun switchToLoginPanel(account: AzureDevOpsAccount?) {
        loginVm.setSelectedAccount(account)
        _panelVm.value = loginVm
    }

    override fun switchToRepositoriesPanel() {
        _panelVm.value = repositoriesVm
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        repositoriesVm.doClone(checkoutListener)
    }
}
