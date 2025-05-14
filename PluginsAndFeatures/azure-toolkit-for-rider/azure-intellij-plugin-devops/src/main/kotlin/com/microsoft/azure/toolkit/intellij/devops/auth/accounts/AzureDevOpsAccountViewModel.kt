package com.microsoft.azure.toolkit.intellij.devops.auth.accounts

import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.actions.AzureDevOpsLoginAgainAction
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.ApiStatus
import javax.swing.Action

@ApiStatus.Internal
interface AzureDevOpsAccountViewModel {
    fun loginAction(): Action
}

internal class AzureDevOpsAccountViewModelImpl(
    private val project: Project,
    parentCs: CoroutineScope,
    private val account: AzureDevOpsAccount,
    private val accountManager: AzureDevOpsAccountManager
) : AzureDevOpsAccountViewModel {
    private val cs: CoroutineScope = parentCs.childScope("Azure DevOps Account VM")

    override fun loginAction(): Action {
        return AzureDevOpsLoginAgainAction("template", project, cs, account, accountManager)
    }
}