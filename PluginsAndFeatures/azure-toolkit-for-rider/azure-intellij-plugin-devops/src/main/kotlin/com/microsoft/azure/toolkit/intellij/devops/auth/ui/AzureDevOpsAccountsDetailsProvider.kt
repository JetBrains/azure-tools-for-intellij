package com.microsoft.azure.toolkit.intellij.devops.auth.ui

import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.collaboration.auth.ui.LazyLoadingAccountsDetailsProvider
import com.intellij.collaboration.auth.ui.cancelOnRemoval
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.util.ResultUtil.runCatchingUser
import com.intellij.openapi.components.service
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsApi
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsUser
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import icons.CollaborationToolsIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import java.awt.Image

class AzureDevOpsAccountsDetailsProvider private constructor(
    cs: CoroutineScope,
    private val apiClientSupplier: suspend (AzureDevOpsAccount) -> AzureDevOpsApi?
) : LazyLoadingAccountsDetailsProvider<AzureDevOpsAccount, AzureDevOpsUser>(
    cs,
    CollaborationToolsIcons.Review.DefaultAvatar
) {
    internal constructor(
        cs: CoroutineScope,
        accountsModel: AzureDevOpsAccountsListModel,
        apiClientSupplier: suspend (AzureDevOpsAccount) -> AzureDevOpsApi?
    ) : this(cs, apiClientSupplier) {
        cancelOnRemoval(accountsModel.accountsListModel)
    }

    constructor(
        scope: CoroutineScope,
        accountManager: AzureDevOpsAccountManager,
        apiClientSupplier: suspend (AzureDevOpsAccount) -> AzureDevOpsApi?
    ) : this(scope, apiClientSupplier) {
        cancelOnRemoval(scope, accountManager)
    }

    override suspend fun loadDetails(account: AzureDevOpsAccount): Result<AzureDevOpsUser> {
        try {
            val api = apiClientSupplier(account)
                ?: return Result.Error(CollaborationToolsBundle.message("account.token.missing"), true)
            val details = runCatchingUser { api.getCurrentUser() }.getOrElse {
                if (it is HttpStatusErrorException && it.statusCode == 401) return Result.Error(
                    CollaborationToolsBundle.message(
                        "account.token.invalid"
                    ), true
                )
                return Result.Error(it.localizedMessage, false)
            }

            return Result.Success(details)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            return Result.Error(e.message, false)
        }
    }

    override suspend fun loadAvatar(account: AzureDevOpsAccount, url: String): Image? {
        return null
    }
}