package com.microsoft.intellij.ui

import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.hover.TreeHoverListener
import com.intellij.ui.treeStructure.Tree
import com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer
import com.microsoft.azuretools.authmanage.IdeAzureAccount
import com.microsoft.intellij.serviceexplorer.azure.AzureModuleImpl
import com.microsoft.tooling.msservices.helpers.collections.ListChangeListener
import com.microsoft.tooling.msservices.helpers.collections.ListChangedEvent
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule
import java.awt.Graphics
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class ServerExplorerToolWindowFactory : ToolWindowFactory, PropertyChangeListener, DumbAware {
    companion object {
        const val EXPLORER_WINDOW = "Azure Explorer"
    }

    private val treeModelMap: MutableMap<Project, DefaultTreeModel> = mutableMapOf()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val azureModule = AzureModuleImpl(project)

        val hiddenRoot = SortableTreeNode()
        val treeModel = DefaultTreeModel(hiddenRoot)
        val tree = Tree(treeModel)

        val favoriteRootNode = TreeNode(AzureExplorer.buildFavoriteRoot(), tree)
        val activeRootNode = TreeNode(AzureExplorer.buildAppCentricViewRoot(), tree)
        val azureRootNode = createTreeNode(azureModule, project)
        hiddenRoot.add(favoriteRootNode)
        hiddenRoot.add(activeRootNode)
        hiddenRoot.add(azureRootNode)
        azureModule.load(false)
        treeModelMap[project] = treeModel

        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        tree.isRootVisible = false
        //AzureEventBus.on("azure.explorer.highlight_resource", new AzureEventBus.EventListener(e -> TreeUtils.highlightResource(tree, e.getSource())));
        tree.cellRenderer = NodeTreeCellRenderer()
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        TreeSpeedSearch(tree)
        val modules = AzureExplorer.getModules().stream()
            .map { TreeNode(it, tree) }
            .toList()
        modules.stream()
            .sorted(Comparator.comparing { it.label })
            .forEach { azureRootNode.add(it) }
        azureModule.setClearResourcesListener {
            modules.forEach { it.clearChildren() }
            activeRootNode.clearChildren()
        }
        TreeUtils.installSelectionListener(tree)
        TreeUtils.installExpandListener(tree)
        TreeUtils.installMouseListener(tree)
        TreeHoverListener.DEFAULT.addTo(tree)
        treeModel.reload()

        toolWindow.component.add(JBScrollPane(tree))

        azureModule.tree = tree
        azureModule.treePath = tree.getPathForRow(0)

        addToolbarItems(toolWindow, project, azureModule)
    }

    private fun addToolbarItems(toolWindow: ToolWindow, project: Project, azureModule: AzureModule) {
        val refreshAction = RefreshAllAction(azureModule)

        toolWindow.setTitleActions(listOf(refreshAction))
    }

    private fun createTreeNode(node: Node, project: Project): SortableTreeNode {
        val treeNode = SortableTreeNode(node, true)

        node.viewData = treeNode
        node.addPropertyChangeListener(this)
        node.childNodes.addChangeListener(NodeListChangeListener(treeNode, project))

        node.childNodes.stream()
            .filter { !isOutdatedModule(it) }
            .sorted(Comparator.comparing { obj: Node -> obj.priority }.thenComparing { obj: Node -> obj.name })
            .map { createTreeNode(it, project) }
            .forEach { treeNode.add(it) }

        return treeNode
    }

    private fun isOutdatedModule(node: Node): Boolean {
        return node !is AzureModule
    }

    override fun propertyChange(evt: PropertyChangeEvent?) {
    }

    private class NodeListChangeListener(treeNode: SortableTreeNode, project: Project) : ListChangeListener {
        override fun listChanged(e: ListChangedEvent) {
        }

    }

    private class NodeTreeCellRenderer : NodeRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
        }

        override fun paintComponent(g: Graphics?) {
            super.paintComponent(g)
        }
    }

    private class RefreshAllAction(private val azureModule: AzureModule) : AnAction(), DumbAware {
        override fun actionPerformed(p0: AnActionEvent) {
            azureModule.load(true)
            AzureExplorer.refreshAll()
        }

        override fun update(e: AnActionEvent) {
            val isSignIn = IdeAzureAccount.getInstance().isLoggedIn
            e.presentation.isEnabled = isSignIn
        }
    }
}