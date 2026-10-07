/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.launchOnShow
import com.intellij.util.ui.tree.TreeUtil
import com.microsoft.azure.toolkit.intellij.appservice.utils.isSameApp
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

abstract class AppServiceDeploymentTreePanel<TConfig : AppServiceConfig>(
    private val vm: AppServiceDeploymentViewModel<TConfig>,
    emptySearchText: String,
    private val emptyTreeText: String,
    private val openCreateAppServiceDialog: (vm: AppServiceDeploymentViewModel<TConfig>, panel: AppServiceDeploymentTreePanel<TConfig>) -> Unit
) : Disposable {
    private val searchTextField = SearchTextField(false).apply {
        textEditor.emptyText.text = emptySearchText
    }

    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode())
    private val tree = Tree(treeModel)
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this).apply {
        border = JBUI.Borders.customLine(JBColor.border(), 1)
    }

    private var isUpdatingSelection = false

    val component: JComponent
        get() = loadingPanel

    init {
        setupTree()
        setupLayout()

        tree.launchOnShow("${AppServiceDeploymentTreePanel::class.java.name}.tree.rebuild") {
            combine(
                vm.remoteAppServiceState,
                vm.draftAppServiceState
            ) { state, draftApps ->
                state to draftApps
            }.collectLatest { (state, draftApps) ->
                withContext(Dispatchers.EDT) {
                    when (state) {
                        is AppServiceLoadState.Loading -> {
                            loadingPanel.startLoading()
                            rebuildTreeModel(emptyList(), emptyList())
                        }

                        is AppServiceLoadState.Loaded<TConfig> -> {
                            loadingPanel.stopLoading()
                            rebuildTreeModel(state.items, draftApps)
                        }

                        is AppServiceLoadState.Error -> {
                            loadingPanel.stopLoading()
                            rebuildTreeModel(emptyList(), draftApps)
                        }
                    }
                }
            }
        }

        tree.launchOnShow("${AppServiceDeploymentTreePanel::class.java.name}.tree.select") {
            vm.selectedAppService.collect { pair ->
                withContext(Dispatchers.EDT) {
                    selectNodeForConfig(pair?.first, pair?.second)
                }
            }
        }
    }

    private fun setupTree() {
        tree.isRootVisible = false
        tree.visibleRowCount = 10
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = AppServiceDeploymentTreeCellRenderer()
        tree.emptyText.text = emptyTreeText
        AppServiceTreeSpeedSearch.installOn(tree, searchTextField)

        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            @Suppress("UNCHECKED_CAST")
            withSelectionGuard {
                when (val userObject = node.userObject) {
                    is ResourceGroupNode -> vm.selectAppService(
                        null,
                        null
                    )

                    is AppServiceNode<*> -> vm.selectAppService(
                        userObject.appServiceModel as AppServiceDeploymentModel<TConfig>,
                        null
                    )

                    is DeploymentSlotNode<*> -> vm.selectAppService(
                        userObject.appServiceModel as AppServiceDeploymentModel.RemoteAppServiceModel<TConfig>,
                        userObject.slotName
                    )
                }
            }
        }
    }

    private fun setupLayout() {
        val actionGroup = DefaultActionGroup(
            DumbAwareAction.create("Create New", AllIcons.General.Add) {
                openCreateAppServiceDialog(vm, this@AppServiceDeploymentTreePanel)
            },
            DumbAwareAction.create("Refresh", AllIcons.Actions.Refresh) {
                vm.refreshAppServices()
            }
        )
        val toolbar = ActionUtil.createToolbarComponent(tree, "AppServiceDeploymentTreePanel", actionGroup, true)

        val topPanel = panel {
            row {
                cell(searchTextField).align(AlignX.FILL).resizableColumn()
                cell(toolbar).align(AlignX.RIGHT)
            }
        }.apply {
            border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
        }

        val scrollPane = JBScrollPane(tree).apply {
            border = JBUI.Borders.empty()
        }

        loadingPanel.add(topPanel, BorderLayout.NORTH)
        loadingPanel.add(scrollPane, BorderLayout.CENTER)
    }

    private fun rebuildTreeModel(
        remoteApps: List<AppServiceDeploymentModel.RemoteAppServiceModel<TConfig>>,
        draftApps: List<AppServiceDeploymentModel.DraftAppServiceModel<TConfig>>
    ) {
        val expandedPaths = TreeUtil.collectExpandedPaths(tree)

        val root = treeModel.root as DefaultMutableTreeNode
        root.removeAllChildren()

        if (draftApps.isNotEmpty()) {
            val draftsGroup = DefaultMutableTreeNode(GroupNode("Drafts"))
            draftApps
                .sortedBy { it.config.appName }
                .forEach { draftsGroup.add(DefaultMutableTreeNode(createAppNode(it))) }
            root.add(draftsGroup)
        }

        val appsByResourceGroup = remoteApps.groupBy { it.resourceGroup }
        for ((resourceGroup, apps) in appsByResourceGroup.entries.sortedBy { it.key.lowercase() }) {
            val rgNode = DefaultMutableTreeNode(ResourceGroupNode(resourceGroup))
            for (app in apps.sortedBy { it.config.appName?.lowercase() }) {
                val appNode = DefaultMutableTreeNode(createAppNode(app))
                app.deploymentSlots.sorted().forEach { slotName ->
                    appNode.add(DefaultMutableTreeNode(DeploymentSlotNode(slotName, app)))
                }
                rgNode.add(appNode)
            }
            root.add(rgNode)
        }

        treeModel.reload()

        if (expandedPaths.isNotEmpty()) {
            TreeUtil.restoreExpandedPaths(tree, expandedPaths)
        }

        val selectedAppService = vm.selectedAppService.value
        selectNodeForConfig(selectedAppService?.first, selectedAppService?.second)
    }

    protected abstract fun createAppNode( appServiceModel: AppServiceDeploymentModel<TConfig>): AppServiceNode<TConfig>

    private fun selectNodeForConfig(config: AppServiceConfig?, slotName: String?) {
        if (config == null) {
            tree.clearSelection()
            return
        }

        val root = treeModel.root as? DefaultMutableTreeNode ?: return

        val targetNode = TreeUtil.findNode(root) { node ->
            when (val obj = node.userObject) {
                is AppServiceNode<*> ->
                    slotName == null && isSameApp(obj.appServiceModel.config, config)

                is DeploymentSlotNode<*> ->
                    slotName != null && obj.slotName == slotName && isSameApp(obj.appServiceModel.config, config)

                else -> false
            }
        } ?: return

        withSelectionGuard { TreeUtil.selectNode(tree, targetNode) }
    }

    private inline fun withSelectionGuard(action: () -> Unit) {
        if (isUpdatingSelection) return
        isUpdatingSelection = true
        try {
            action()
        } finally {
            isUpdatingSelection = false
        }
    }

    override fun dispose() {}
}