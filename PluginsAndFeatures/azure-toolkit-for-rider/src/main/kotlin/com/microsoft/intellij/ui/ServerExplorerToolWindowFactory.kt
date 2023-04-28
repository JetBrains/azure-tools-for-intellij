package com.microsoft.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.UISettings
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.LoadingNode
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.hover.TreeHoverListener
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.application
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.common.component.Tree.LoadMoreNode
import com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azuretools.authmanage.IdeAzureAccount
import com.microsoft.intellij.helpers.UiHelperImpl
import com.microsoft.intellij.serviceexplorer.azure.AzureModuleImpl
import com.microsoft.tooling.msservices.helpers.collections.ListChangeListener
import com.microsoft.tooling.msservices.helpers.collections.ListChangedAction
import com.microsoft.tooling.msservices.helpers.collections.ListChangedEvent
import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule
import org.apache.commons.lang3.StringUtils
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Comparator.comparing
import java.util.stream.Collectors
import javax.swing.*
import javax.swing.tree.*

class ServerExplorerToolWindowFactory : ToolWindowFactory, PropertyChangeListener, DumbAware {
    companion object {
        const val EXPLORER_WINDOW = "Azure Explorer"
        private fun isOutdatedModule(node: Node): Boolean {
            return node !is AzureModule
        }
    }

    private val treeModelMap: MutableMap<Project, DefaultTreeModel> = mutableMapOf()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val azureModule = AzureModuleImpl(project)

        val hiddenRoot = SortableTreeNode()
        val treeModel = DefaultTreeModel(hiddenRoot)
        val tree = Tree(treeModel)

        val favoriteRootNode = TreeNode(AzureExplorer.buildFavoriteRoot(), tree)
        val acvRootNode = TreeNode(AzureExplorer.buildAppCentricViewRoot(), tree)
        val azureRootNode = createTreeNode(azureModule, project)
        hiddenRoot.add(favoriteRootNode)
        hiddenRoot.add(acvRootNode)
        hiddenRoot.add(azureRootNode)
        azureModule.load(false)
        treeModelMap[project] = treeModel

        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        tree.isRootVisible = false
        AzureEventBus.on("azure.explorer.highlight_resource", AzureEventBus.EventListener { e: AzureEvent ->
            TreeUtils.highlightResource(tree, e.source)
        })
        AzureEventBus.on("resource.creation_started.resource", AzureEventBus.EventListener { e: AzureEvent ->
            val source = e.source
            if (source is AbstractAzResource<*,*,*>) {
                TreeUtils.focusResource(tree, source)
            }
        })
        AzureEventBus.on("azure.explorer.focus_resource", AzureEventBus.EventListener { e: AzureEvent ->
            val source = e.source
            if (source is AbstractAzResource<*,*,*>) {
                TreeUtils.focusResource(tree, source)
            }
        })
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
            acvRootNode.clearChildren()
        }
        TreeUtils.installSelectionListener(tree)
        TreeUtils.installExpandListener(tree)
        TreeUtils.installMouseListener(tree)
        TreeHoverListener.DEFAULT.addTo(tree)
        treeModel.reload()

        DataManager.registerDataProvider(tree) {
            if (StringUtils.equals(it, Action.SOURCE)) {
                val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
                return@registerDataProvider selectedNode?.userObject
            }
            return@registerDataProvider null
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                treeMousePressed(e, tree)
            }
        })

        val scrollPane = JBScrollPane(tree)
        tree.putClientProperty(TreeUtils.KEY_SCROLL_PANE, scrollPane)
        toolWindow.component.add(scrollPane)

        azureModule.tree = tree
        azureModule.treePath = tree.getPathForRow(0)

        addToolbarItems(toolWindow, project, azureModule)
        tree.model.addTreeModelListener(TreeUtils.FocusResourceListener(tree))
    }

    private fun treeMousePressed(e: MouseEvent, tree: JTree) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            val treePath = tree.getPathForLocation(e.x, e.y) ?: return
            val node = getTreeNodeOnMouseClick(tree, treePath)
            if (node != null && !node.isLoading) {
                node.clickAction.fireNodeActionEvent()
            }
        } else if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger) {
            val treePath = tree.getClosestPathForLocation(e.x, e.y) ?: return
            val node = getTreeNodeOnMouseClick(tree, treePath)
            if (node != null && node.hasNodeActions()) {
                tree.selectionModel.selectionPath = treePath
                val menu = createPopupMenuForNode(node)
                menu.show(e.component, e.x, e.y)
            }
        }
    }

    private fun getTreeNodeOnMouseClick(tree: JTree, treePath: TreePath): Node? {
        val raw = treePath.lastPathComponent
        if (raw is TreeNode<*> || raw is LoadingNode || raw is LoadMoreNode) {
            return null
        }

        val treeNode = raw as? SortableTreeNode ?: return null
        val node = treeNode.userObject as? Node ?: return null

        node.tree = tree
        node.treePath = treePath

        return node
    }

    private fun createPopupMenuForNode(node: Node): JPopupMenu {
        val menu = JPopupMenu()

        val sortedNodeActionsGroupMap = node.nodeActions.stream()
            .sorted(
                comparing { obj: NodeAction -> obj.group }
                    .thenComparing { obj: NodeAction -> obj.priority }
                    .thenComparing { obj: NodeAction -> obj.name }
            )
            .collect(Collectors.groupingBy(NodeAction::getGroup, { linkedMapOf() }, Collectors.toList()))

        sortedNodeActionsGroupMap.forEach { (_, actions) ->
            if (menu.componentCount > 0) menu.addSeparator()
            actions.stream()
                .map { nodeAction: NodeAction -> createMenuItemFromNodeAction(nodeAction) }
                .forEachOrdered { menuItem: JMenuItem -> menu.add(menuItem) }
        }

        return menu
    }

    private fun createMenuItemFromNodeAction(nodeAction: NodeAction): JMenuItem {
        val menuItem = JMenuItem(nodeAction.name)
        menuItem.isEnabled = nodeAction.isEnabled
        val iconSymbol = nodeAction.iconSymbol
        if (iconSymbol != null) {
            menuItem.icon = IntelliJAzureIcons.getIcon(iconSymbol)
        } else if (nodeAction.iconPath.isNotBlank()) {
            menuItem.icon = UiHelperImpl.loadIcon(nodeAction.iconPath)
        }
        menuItem.addActionListener { nodeAction.fireNodeActionEvent() }
        return menuItem
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

    private fun removeEventHandlers(node: Node) {
        node.removePropertyChangeListener(this)

        val childNodes = node.childNodes
        childNodes.removeAllChangeListeners()

        if (node.hasChildNodes()) {
            node.removeAllChildNodes()
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (!application.isDispatchThread) {
            AzureTaskManager.getInstance().runAndWait({ propertyChange(evt) }, AzureTask.Modality.ANY)
            return
        }

        val node = evt.source as? Node ?: return
        val project = node.project as? Project
        val treeModel = treeModelMap[project]
        if (treeModel != null) {
            synchronized(treeModel) { treeModel.nodeChanged(node.viewData as javax.swing.tree.TreeNode) }
        }
    }

    private inner class NodeListChangeListener(private val treeNode: SortableTreeNode, private val project: Project) :
        ListChangeListener {

        override fun listChanged(e: ListChangedEvent) {
            if (!application.isDispatchThread) {
                AzureTaskManager.getInstance().runAndWait({ listChanged(e) }, AzureTask.Modality.ANY)
                return
            }

            when (e.action) {
                ListChangedAction.add -> {
                    for (childNode in e.newItems) {
                        val node = childNode as? Node ?: continue
                        if (isOutdatedModule(node)) continue
                        treeNode.add(createTreeNode(node, project))
                    }
                }

                ListChangedAction.remove -> {
                    for (childNode in e.oldItems) {
                        val node = childNode as? Node ?: continue
                        if (isOutdatedModule(node)) continue
                        removeEventHandlers(node)
                        treeNode.remove(childNode.viewData as MutableTreeNode)
                    }
                }

                else -> {}
            }

            val model = treeModelMap[project]
            if (model != null) {
                synchronized(model) { model.reload(treeNode) }
            }
        }
    }

    private class NodeTreeCellRenderer : NodeRenderer() {
        private val inlineActionIcons: MutableList<Icon> = mutableListOf()
        private var viewportRect: Rectangle? = null
        private var scrolledWidth: Int = 0

        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            inlineActionIcons.clear()

            when (value) {
                is TreeNode<*> -> {
                    val hoveredRow = TreeHoverListener.getHoveredRow(tree)
                    inlineActionIcons.addAll(value.inlineActionViews.stream()
                        .map { IntelliJAzureIcons.getIcon(it.iconPath) }
                        .filter { hoveredRow == row || it == AllIcons.Nodes.Favorite }
                        .toList()
                    )
                    val scrollPane = tree.getClientProperty(TreeUtils.KEY_SCROLL_PANE) as? JBScrollPane
                    if (scrollPane != null) {
                        viewportRect = scrollPane.viewport.viewRect
                    }
                    TreeUtils.renderMyTreeNode(tree, value, selected, this)
                    return
                }

                is LoadMoreNode -> {
                    TreeUtils.renderLoadModeNode(tree, value, selected, this)
                    return
                }

                is LoadingNode -> {
                    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
                    return
                }

                else -> {
                    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)

                    val treeNode = value as? SortableTreeNode
                    val node = treeNode?.userObject as? Node ?: return

                    val icon = node.icon
                    val iconSymbol = node.iconSymbol
                    val iconPath = node.iconPath

                    if (icon != null) setIcon(icon)
                    else if (iconSymbol != null) setIcon(IntelliJAzureIcons.getIcon(iconSymbol))
                    else if (iconPath.isNotBlank()) setIcon(UiHelperImpl.loadIcon(iconPath))

                    toolTipText = node.toolTip
                }
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (x > 0) {
                paintInlineActionIcons(g)
            }
        }

        private fun paintInlineActionIcons(g: Graphics) {
            val rect = viewportRect ?: return
            var prevIconBoxOffset = rect.x + rect.width - x - TreeUtils.NODE_PADDING
            for (icon in inlineActionIcons) {
                val iconBoxWidth = TreeUtils.INLINE_ACTION_ICON_WIDTH + TreeUtils.INLINE_ACTION_ICON_MARGIN
                val iconBoxOffset = prevIconBoxOffset - iconBoxWidth
                if (iconBoxOffset > 0) {
                    val iconOffset = iconBoxOffset + TreeUtils.INLINE_ACTION_ICON_MARGIN / 2
                    g.color = background
                    g.fillRect(iconBoxOffset, 0, iconBoxWidth, height)
                    paintIcon(g, icon, iconOffset)
                    prevIconBoxOffset = iconBoxOffset
                }
            }
        }
    }

    private fun addToolbarItems(toolWindow: ToolWindow, project: Project, azureModule: AzureModule) {
        val refreshAction = RefreshAllAction(azureModule)
        val signInAction = ActionManager.getInstance().getAction("AzureToolkit.AzureSignIn")
        val selectSubscriptionsAction = ActionManager.getInstance().getAction("AzureToolkit.SelectSubscriptions")

        toolWindow.setTitleActions(listOf(refreshAction, selectSubscriptionsAction, signInAction))
    }

    private class RefreshAllAction(private val azureModule: AzureModule) :
        AnAction("Refresh All", "Refresh Azure nodes list", AllIcons.Actions.Refresh), DumbAware {

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