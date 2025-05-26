package com.microsoft.azure.toolkit.intellij.devops.ui.clone.model

import com.intellij.collaboration.auth.ui.login.LoginModel
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsServerPath
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import com.microsoft.azure.toolkit.intellij.devops.auth.ui.AzureDevOpsTokenLoginPanelModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal interface AzureDevOpsCloneLoginViewModel : AzureDevOpsClonePanelViewModel {
    val accounts: SharedFlow<Set<AzureDevOpsAccount>>
    val tokenLoginModel: AzureDevOpsTokenLoginPanelModel
}

internal class AzureDevOpsCloneLoginViewModelImpl(
    parentCs: CoroutineScope,
    private val accountManager: AzureDevOpsAccountManager
) : AzureDevOpsCloneLoginViewModel {

    private val cs: CoroutineScope = parentCs.childScope("Azure DevOps Clone Login VM")

    private var selectedAccount: AzureDevOpsAccount? = null
    override val accounts: SharedFlow<Set<AzureDevOpsAccount>> = accountManager.accountsState
    override val tokenLoginModel: AzureDevOpsTokenLoginPanelModel = AzureDevOpsTokenLoginPanelModel(
        uniqueAccountChecker = accountManager::isAccountUnique
    )

    init {
        cs.launch {
            with(tokenLoginModel) {
                loginState.collectLatest { loginState ->
                    if (loginState is LoginModel.LoginState.Connected) {
                        val storedAccount =
                            selectedAccount ?: AzureDevOpsAccount(name = loginState.username, server = getServerPath())
                        updateAccount(storedAccount, token)
                    }
                }
            }
        }
    }

    fun setSelectedAccount(account: AzureDevOpsAccount?) {
        selectedAccount = account
        with(tokenLoginModel) {
            uniqueAccountChecker = if (account == null) accountManager::isAccountUnique else { _, _ -> true }
            serverUri = account?.server?.uri ?: AzureDevOpsServerPath.DEFAULT_PATH.uri
        }
    }

    private suspend fun updateAccount(account: AzureDevOpsAccount, credentials: String) {
        accountManager.updateAccount(account, credentials)
    }
}
