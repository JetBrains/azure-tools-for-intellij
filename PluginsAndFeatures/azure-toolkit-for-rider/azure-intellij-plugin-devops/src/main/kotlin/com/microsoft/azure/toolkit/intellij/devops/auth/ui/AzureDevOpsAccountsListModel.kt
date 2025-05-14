package com.microsoft.azure.toolkit.intellij.devops.auth.ui

import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.MutableAccountsListModel
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount

internal class AzureDevOpsAccountsListModel : MutableAccountsListModel<AzureDevOpsAccount, String>(),
    AccountsListModel.WithDefault<AzureDevOpsAccount, String> {
    override var defaultAccount: AzureDevOpsAccount? = null
}
