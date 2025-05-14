package com.microsoft.azure.toolkit.intellij.devops.auth.accounts

import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.collaboration.auth.PasswordSafeCredentialsRepository
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsServerPath
import com.microsoft.azure.toolkit.intellij.devops.util.AzureDevOpsConstants

interface AzureDevOpsAccountManager : AccountManager<AzureDevOpsAccount, String> {
    fun isAccountUnique(server: AzureDevOpsServerPath, accountName: String): Boolean
}

class AzureDevOpsPersistentAccountManager :
    AzureDevOpsAccountManager,
    AccountManagerBase<AzureDevOpsAccount, String>(logger<AzureDevOpsAccountManager>()) {

    override fun accountsRepository(): AccountsRepository<AzureDevOpsAccount> = service<AzureDevOpsPersistentAccounts>()

    override fun credentialsRepository() =
        PasswordSafeCredentialsRepository<AzureDevOpsAccount, String>(
            AzureDevOpsConstants.SERVICE_NAME,
            PasswordSafeCredentialsRepository.CredentialsMapper.Simple
        )

    override fun isAccountUnique(server: AzureDevOpsServerPath, accountName: String): Boolean {
        return accountsState.value.none { account: AzureDevOpsAccount ->
            account.server.toHttpsURI() == server.toHttpsURI() && account.name == accountName
        }
    }
}
