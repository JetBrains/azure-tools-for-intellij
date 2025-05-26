package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount

internal sealed interface AzureDevOpsCloneException {
    val account: AzureDevOpsAccount

    data class MissingAccessToken(override val account: AzureDevOpsAccount) : AzureDevOpsCloneException
    data class RevokedToken(override val account: AzureDevOpsAccount) : AzureDevOpsCloneException
    data class ConnectionError(override val account: AzureDevOpsAccount) : AzureDevOpsCloneException
    data class Unknown(override val account: AzureDevOpsAccount, val message: String) : AzureDevOpsCloneException
}
