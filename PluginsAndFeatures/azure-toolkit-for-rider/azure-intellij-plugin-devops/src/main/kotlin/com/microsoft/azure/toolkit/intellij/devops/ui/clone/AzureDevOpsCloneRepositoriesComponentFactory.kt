package com.microsoft.azure.toolkit.intellij.devops.ui.clone

import com.intellij.collaboration.async.launchNow
import com.intellij.collaboration.auth.ui.CompactAccountsPanelFactory
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.ui.codereview.details.GroupedRenderer
import com.intellij.collaboration.ui.util.LinkActionMouseAdapter
import com.intellij.collaboration.ui.util.bindBusyIn
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.FilePathDocumentChildPathHandle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.CollectionListModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import com.microsoft.azure.toolkit.intellij.devops.auth.accounts.AzureDevOpsAccount
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneRepositoriesViewModel
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneRepositoriesViewModel.SearchModel
import com.microsoft.azure.toolkit.intellij.devops.ui.clone.model.AzureDevOpsCloneViewModel
import git4idea.GitUtil
import git4idea.remote.GitRememberedInputs
import git4idea.ui.GitShallowCloneComponentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.swing.JComponent
import javax.swing.JSeparator
import javax.swing.ListCellRenderer
import javax.swing.ListModel
import javax.swing.event.DocumentEvent

internal object AzureDevOpsCloneRepositoriesComponentFactory {
    fun create(
        project: Project,
        cs: CoroutineScope,
        repositoriesVm: AzureDevOpsCloneRepositoriesViewModel,
        cloneVm: AzureDevOpsCloneViewModel,
    ): DialogPanel {
        val searchField = createSearchField(repositoriesVm)
        val directoryField = createDirectoryField(project, cs, repositoriesVm)

        val accountsModel = createAccountsModel(cs, repositoriesVm)
        val repositoriesModel = createRepositoriesModel(cs, repositoriesVm)

        val accountsPanel = CompactAccountsPanelFactory(accountsModel).create(
            repositoriesVm.accountDetailsProvider,
            VcsCloneDialogUiSpec.Components.avatarSize,
            AccountsPopupConfig(cloneVm)
        )
        val repositoryList = createRepositoryList(cs, cloneVm, repositoriesVm, accountsModel, repositoriesModel)
        CollaborationToolsUIUtil.attachSearch(repositoryList, searchField) { cloneItem ->
            when (cloneItem) {
                is AzureDevOpsCloneListItem.ErrorItem -> ""
                is AzureDevOpsCloneListItem.Repository -> cloneItem.presentation()
            }
        }

        return panel {
            row {
                cell(searchField.textEditor)
                    .resizableColumn()
                    .align(Align.FILL)
                cell(JSeparator(JSeparator.VERTICAL))
                    .align(AlignY.FILL)
                cell(accountsPanel)
                    .align(AlignY.FILL)
            }
            row {
                scrollCell(repositoryList)
                    .resizableColumn()
                    .align(Align.FILL)
            }.resizableRow()
            row(CollaborationToolsBundle.message("clone.dialog.directory.to.clone.label.text")) {
                cell(directoryField)
                    .align(AlignX.FILL)
                    .validationOnApply {
                        CloneDvcsValidationUtils.checkDirectory(it.text, it.textField as JComponent)
                    }
            }
            GitShallowCloneComponentFactory.appendShallowCloneRow(this, repositoriesVm.shallowCloneViewModel)
        }.apply {
            border = JBEmptyBorder(UIUtil.getRegularPanelInsets())
        }
    }

    private fun createAccountsModel(
        cs: CoroutineScope,
        repositoriesVm: AzureDevOpsCloneRepositoriesViewModel
    ): ListModel<AzureDevOpsAccount> {
        val accountsModel = CollectionListModel<AzureDevOpsAccount>()
        cs.launch {
            repositoriesVm.listVm.allAccounts.collectLatest { accounts ->
                accountsModel.replaceAll(accounts)
            }
        }

        return accountsModel
    }

    private fun createRepositoriesModel(
        cs: CoroutineScope,
        repositoriesVm: AzureDevOpsCloneRepositoriesViewModel,
    ): ListModel<AzureDevOpsCloneListItem> {
        val repositoriesModel = object : CollectionListModel<AzureDevOpsCloneListItem>() {
            override fun fireIntervalAdded(source: Any?, index0: Int, index1: Int) {
            }

            override fun fireIntervalRemoved(source: Any?, index0: Int, index1: Int) {
            }

            override fun replaceAll(elements: List<AzureDevOpsCloneListItem?>) {
                super.replaceAll(elements)
                super.fireContentsChanged(this, 0, elements.size)
            }
        }

        cs.launch {
            repositoriesVm.listVm.allItems.collectLatest { items ->
                repositoriesModel.replaceAll(items)
            }
        }

        return repositoriesModel
    }

    private fun createRepositoryList(
        cs: CoroutineScope,
        cloneVm: AzureDevOpsCloneViewModel,
        repositoriesVm: AzureDevOpsCloneRepositoriesViewModel,
        accountsModel: ListModel<AzureDevOpsAccount>,
        repositoriesModel: ListModel<AzureDevOpsCloneListItem>,
    ): JBList<AzureDevOpsCloneListItem> {
        return JBList(repositoriesModel).apply {
            cellRenderer = createRepositoryRenderer(cloneVm, repositoriesVm, accountsModel, repositoriesModel)
            isFocusable = false
            selectionModel.addListSelectionListener {
                repositoriesVm.selectItem(selectedValue)
            }
            bindBusyIn(cs, repositoriesVm.listVm.isLoading)

            val mouseAdapter = LinkActionMouseAdapter(this)
            addMouseListener(mouseAdapter)
            addMouseMotionListener(mouseAdapter)

            cs.launchNow {
                repositoriesVm.searchValue.collectLatest { searchValue ->
                    emptyText.text = when (searchValue) {
                        is SearchModel.Text -> StatusText.getDefaultEmptyText()
                        is SearchModel.Url -> CollaborationToolsBundle.message(
                            "clone.dialog.repository.url.text",
                            searchValue.url
                        )
                    }
                }
            }
        }
    }

    private fun createRepositoryRenderer(
        cloneVm: AzureDevOpsCloneViewModel,
        repositoriesVm: AzureDevOpsCloneRepositoriesViewModel,
        accountsModel: ListModel<AzureDevOpsAccount>,
        repositoriesModel: ListModel<AzureDevOpsCloneListItem>,
    ): ListCellRenderer<AzureDevOpsCloneListItem> {
        return GroupedRenderer.create(
            baseRenderer = AzureDevOpsCloneListRenderer(cloneVm, repositoriesVm.listVm),
            hasSeparatorAbove = { value, index ->
                when (index) {
                    0 -> accountsModel.size > 1
                    else -> {
                        val previousAccount = repositoriesModel.getElementAt(index - 1).account
                        previousAccount != value.account
                    }
                }
            },
            buildSeparator = { value, index, _ ->
                GroupedRenderer.createDefaultSeparator(text = value.account.name, paintLine = index != 0)
            }
        )
    }

    private fun createSearchField(repositoriesVm: AzureDevOpsCloneRepositoriesViewModel): SearchTextField {
        return SearchTextField(false).apply {
            addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    repositoriesVm.setSearchValue(text)
                }
            })
        }
    }

    private fun createDirectoryField(
        project: Project,
        cs: CoroutineScope,
        repositoriesVm: AzureDevOpsCloneRepositoriesViewModel,
    ): TextFieldWithBrowseButton {
        val directoryField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                project, FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withShowFileSystemRoots(true)
                    .withHideIgnored(false)
                    .withTitle(DvcsBundle.message("clone.destination.directory.browser.title"))
                    .withDescription(DvcsBundle.message("clone.destination.directory.browser.description"))
            )
            addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    repositoriesVm.setDirectoryPath(text)
                }
            })
        }
        val cloneDirectoryChildHandle = FilePathDocumentChildPathHandle.install(
            directoryField.textField.document,
            ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())
        )

        cs.launchNow {
            repositoriesVm.selectedUrl.filterNotNull().collectLatest { selectedUrl ->
                val path =
                    ClonePathProvider.relativeDirectoryPathForVcsUrl(project, selectedUrl).removeSuffix(GitUtil.DOT_GIT)
                cloneDirectoryChildHandle.trySetChildPath(path)
            }
        }

        return directoryField
    }

    private class AccountsPopupConfig(cloneVm: AzureDevOpsCloneViewModel) :
        CompactAccountsPanelFactory.PopupConfig<AzureDevOpsAccount> {
        private val loginWithTokenAction: AccountMenuItem.Action = AccountMenuItem.Action(
            CollaborationToolsBundle.message("clone.dialog.login.with.token.action"),
            { cloneVm.switchToLoginPanel(account = null) },
            showSeparatorAbove = true
        )

        override val avatarSize: Int = VcsCloneDialogUiSpec.Components.popupMenuAvatarSize

        override fun createActions(): Collection<AccountMenuItem.Action> = listOf(loginWithTokenAction)
    }
}
