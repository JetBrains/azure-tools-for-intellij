package com.microsoft.azure.toolkit.intellij.devops.ui.clone.model

import com.intellij.collaboration.async.mapState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsApiManager
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccountManager
import com.microsoft.azure.toolkit.intellij.devops.auth.ui.AzureDevOpsAccountsDetailsProvider
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.AzureDevOpsCloneListItem
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.GitCloneUtils
import git4idea.ui.GitShallowCloneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.net.MalformedURLException
import java.net.URL

internal interface AzureDevOpsCloneRepositoriesViewModel : AzureDevOpsClonePanelViewModel {
    val listVm: AzureDevOpsCloneRepositoriesListViewModel

    val searchValue: SharedFlow<SearchModel>
    val selectedUrl: SharedFlow<String?>
    val shallowCloneViewModel: GitShallowCloneViewModel

    val accountDetailsProvider: AzureDevOpsAccountsDetailsProvider

    fun selectItem(item: AzureDevOpsCloneListItem?)

    fun setSearchValue(text: String)

    fun setDirectoryPath(path: String)

    fun doClone(checkoutListener: CheckoutProvider.Listener)

    sealed interface SearchModel {
        class Url(val url: String) : SearchModel
        object Text : SearchModel
    }
}

internal class AzureDevOpsCloneRepositoriesViewModelImpl(
    private val project: Project,
    parentCs: CoroutineScope,
    accountManager: AzureDevOpsAccountManager,
) : AzureDevOpsCloneRepositoriesViewModel {
    private val apiManager: AzureDevOpsApiManager = service<AzureDevOpsApiManager>()

    private val cs = parentCs.childScope("Azure DevOps Clone Repositories VM")

    override val listVm: AzureDevOpsCloneRepositoriesListViewModel =
        AzureDevOpsCloneRepositoriesListViewModelImpl(cs, accountManager)

    private val _searchValue: MutableStateFlow<String> = MutableStateFlow("")
    override val searchValue: SharedFlow<AzureDevOpsCloneRepositoriesViewModel.SearchModel> =
        _searchValue.mapState(cs) { text ->
            try {
                URL(text)
                AzureDevOpsCloneRepositoriesViewModel.SearchModel.Url(text)
            } catch (_: MalformedURLException) {
                AzureDevOpsCloneRepositoriesViewModel.SearchModel.Text
            }
        }

    private val selectedItem: MutableStateFlow<AzureDevOpsCloneListItem?> = MutableStateFlow(null)
    private val _selectedUrl: StateFlow<String?> = combine(searchValue, selectedItem) { searchValue, selectedItem ->
        when {
            searchValue is AzureDevOpsCloneRepositoriesViewModel.SearchModel.Url -> searchValue.url
            selectedItem != null && selectedItem is AzureDevOpsCloneListItem.Repository -> selectedItem.repository.remoteUrl
            else -> null
        }
    }.stateIn(cs, SharingStarted.Eagerly, initialValue = null)
    override val selectedUrl: SharedFlow<String?> = _selectedUrl

    override val shallowCloneViewModel = GitShallowCloneViewModel()

    private val directoryPath: MutableStateFlow<String> = MutableStateFlow("")

    override val accountDetailsProvider = AzureDevOpsAccountsDetailsProvider(cs, accountManager) { account ->
        val token = accountManager.findCredentials(account) ?: return@AzureDevOpsAccountsDetailsProvider null
        apiManager.getClient(account.server) { token }
    }

    override fun selectItem(item: AzureDevOpsCloneListItem?) {
        selectedItem.value = item
    }

    override fun setSearchValue(text: String) {
        _searchValue.value = text
    }

    override fun setDirectoryPath(path: String) {
        directoryPath.value = path
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val selectedUrl = _selectedUrl.value ?: error("Clone button is enabled when repository is not selected")
        GitCloneUtils.clone(project, selectedUrl, directoryPath.value, shallowCloneViewModel.getShallowCloneOptions(), checkoutListener,
            CLONE_UNABLE_TO_CREATE_DESTINATION_DIRECTORY,
            CLONE_UNABLE_TO_FIND_DESTINATION_DIRECTORY)
    }

    companion object {
        private const val CLONE_UNABLE_TO_CREATE_DESTINATION_DIRECTORY = "Unable to create destination directory"
        private const val CLONE_UNABLE_TO_FIND_DESTINATION_DIRECTORY = "gitlab clone unable to find destination directory"
    }
}
