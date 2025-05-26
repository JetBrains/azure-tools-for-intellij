package com.microsoft.azure.toolkit.intellij.devops.ui.clone.model

import com.intellij.collaboration.api.HttpStatusErrorException
import com.intellij.collaboration.async.mapModelsToViewModels
import com.intellij.collaboration.async.withInitial
import com.intellij.openapi.components.service
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsApiManager
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.AzureDevOpsCloneException
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.AzureDevOpsCloneListItem
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.ConnectException

internal interface AzureDevOpsCloneRepositoriesForAccountListViewModel {
    val account: AzureDevOpsAccount

    val isLoading: StateFlow<Boolean>
    val items: StateFlow<List<AzureDevOpsCloneListItem>>

    fun reload()
}

internal class AzureDevOpsCloneRepositoriesForAccountListViewModelImpl(
    parentCs: CoroutineScope,
    override val account: AzureDevOpsAccount,
    private val accountManager: AzureDevOpsAccountManager
) : AzureDevOpsCloneRepositoriesForAccountListViewModel {
    private val cs = parentCs.childScope("Azure DevOps Clone Repositories List VM for account")

    private val reloadSignal = MutableSharedFlow<Unit>(1)
    private val apiManager = service<AzureDevOpsApiManager>()

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override fun reload() {
        cs.launch {
            reloadSignal.emit(Unit)
        }
    }

    override val items: StateFlow<List<AzureDevOpsCloneListItem>> =
        reloadSignal.withInitial(Unit).transformLatest { _ ->
            try {
                _isLoading.value = true
                val token = accountManager.findCredentials(account) ?: run {
                    emit(
                        listOf(
                            AzureDevOpsCloneListItem.ErrorItem(
                                account,
                                AzureDevOpsCloneException.MissingAccessToken(account)
                            )
                        )
                    )
                    return@transformLatest
                }
                val apiClient = apiManager.getClient(account.server) { token }
                val repositories = apiClient.getRepositories()
                emit(repositories.map { AzureDevOpsCloneListItem.Repository(account, it) })
            } catch (_: ConnectException) {
                emit(
                    listOf(
                        AzureDevOpsCloneListItem.ErrorItem(
                            account,
                            AzureDevOpsCloneException.ConnectionError(account)
                        )
                    )
                )
            } catch (error: Throwable) {
                if (error is HttpStatusErrorException && error.statusCode == 401) {
                    emit(
                        listOf(
                            AzureDevOpsCloneListItem.ErrorItem(
                                account,
                                AzureDevOpsCloneException.RevokedToken(account)
                            )
                        )
                    )
                } else {
                    val errorMessage =
                        error.localizedMessage ?: "unable to load repositories"
                    emit(
                        listOf(
                            AzureDevOpsCloneListItem.ErrorItem(
                                account, AzureDevOpsCloneException.Unknown(account, errorMessage)
                            )
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                _isLoading.value = false
            }
        }
            .flowOn(Dispatchers.IO)
            .stateIn(cs, SharingStarted.Eagerly, listOf())
}

internal interface AzureDevOpsCloneRepositoriesListViewModel {
    val allItems: StateFlow<List<AzureDevOpsCloneListItem>>
    val allAccounts: StateFlow<List<AzureDevOpsAccount>>
    val isLoading: StateFlow<Boolean>

    fun reload()
    fun reload(account: AzureDevOpsAccount)
}

internal class AzureDevOpsCloneRepositoriesListViewModelImpl(
    parentCs: CoroutineScope,
    accountManager: AzureDevOpsAccountManager
) : AzureDevOpsCloneRepositoriesListViewModel {
    private val cs = parentCs.childScope("Azure DevOps Clone Repositories List VM")
    private val reloadSignal = MutableSharedFlow<Unit>(1)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listsPerAccount = reloadSignal.withInitial(Unit).flatMapLatest { _ ->
        accountManager.accountsState.mapModelsToViewModels<AzureDevOpsAccount, AzureDevOpsCloneRepositoriesForAccountListViewModel> { account ->
            AzureDevOpsCloneRepositoriesForAccountListViewModelImpl(this, account, accountManager)
        }
    }.stateIn(cs, SharingStarted.Eagerly, listOf())

    override val allAccounts: StateFlow<List<AzureDevOpsAccount>> = listsPerAccount
        .map { viewModels -> viewModels.map { it.account } }
        .stateIn(cs, SharingStarted.Eagerly, listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allItems: StateFlow<List<AzureDevOpsCloneListItem>> = listsPerAccount.flatMapLatest { viewModels ->
        combine(viewModels.map { it.items }) { it ->
            it.flatMap { it }
        }
    }.stateIn(cs, SharingStarted.Eagerly, listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isLoading: StateFlow<Boolean> = listsPerAccount.flatMapLatest { viewModels ->
        combine(viewModels.map { model -> model.isLoading }) { it ->
            it.any { it }
        }
    }.stateIn(cs, SharingStarted.Eagerly, false)

    override fun reload() {
        cs.launch { reloadSignal.emit(Unit) }
    }

    override fun reload(account: AzureDevOpsAccount) {
        cs.launch { listsPerAccount.value.find { it.account == account }?.reload() }
    }
}
