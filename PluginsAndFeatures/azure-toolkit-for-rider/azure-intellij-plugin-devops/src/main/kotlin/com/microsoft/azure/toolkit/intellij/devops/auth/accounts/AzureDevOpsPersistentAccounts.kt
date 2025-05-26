package com.microsoft.azure.toolkit.intellij.devops.auth.accounts

import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlinx.serialization.Serializable

@State(name = "AzureDevOpsAccounts",
    category = SettingsCategory.TOOLS,
    exportable = true,
    storages = [Storage(value = "azuredevops.xml", roamingType = RoamingType.DISABLED)], reportStatistic = false)
internal class AzureDevOpsPersistentAccounts : AccountsRepository<AzureDevOpsAccount>,
    SerializablePersistentStateComponent<AzureDevOpsPersistentAccounts.AzureDevOpsAccountsState>(
        AzureDevOpsAccountsState()
    ) {
    @Serializable
    data class AzureDevOpsAccountsState(val accounts: Set<AzureDevOpsAccount> = emptySet())

    override var accounts: Set<AzureDevOpsAccount>
        get() = state.accounts.toSet()
        set(value) {
            updateState {
                AzureDevOpsAccountsState(value.toSet())
            }
        }
}
