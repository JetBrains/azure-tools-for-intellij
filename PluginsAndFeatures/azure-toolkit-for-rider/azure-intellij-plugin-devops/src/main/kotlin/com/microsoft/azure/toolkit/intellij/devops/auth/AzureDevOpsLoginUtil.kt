package com.microsoft.azure.toolkit.intellij.devops.auth

import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount

sealed interface LoginResult {
    data class Success(val account: AzureDevOpsAccount, val token: String) : LoginResult
    data object Failure : LoginResult
    data object OtherMethod : LoginResult
}
