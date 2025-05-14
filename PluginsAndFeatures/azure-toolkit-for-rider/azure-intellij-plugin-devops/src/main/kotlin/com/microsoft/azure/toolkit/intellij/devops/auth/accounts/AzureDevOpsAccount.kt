package com.microsoft.azure.toolkit.intellij.devops.auth.accounts

import com.intellij.collaboration.auth.ServerAccount
import com.intellij.openapi.util.NlsSafe
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsServerPath
import kotlinx.serialization.Serializable

@Serializable
class AzureDevOpsAccount(
    override val id: String = generateId(),
    override val name: @NlsSafe String = "",
    override val server: AzureDevOpsServerPath = AzureDevOpsServerPath()
) : ServerAccount() {
}
