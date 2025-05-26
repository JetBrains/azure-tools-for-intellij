package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.intellij.openapi.util.NlsSafe
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsRepository
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount

internal sealed interface AzureDevOpsCloneListItem {
    val account: AzureDevOpsAccount

    data class Repository(override val account: AzureDevOpsAccount, val repository: AzureDevOpsRepository) : AzureDevOpsCloneListItem

    data class ErrorItem(override val account: AzureDevOpsAccount, val error: AzureDevOpsCloneException) :
        AzureDevOpsCloneListItem
}

internal fun AzureDevOpsCloneListItem.Repository.presentation(): @NlsSafe String = repository.name
