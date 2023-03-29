package com.microsoft.intellij.ui

import com.microsoft.tooling.msservices.serviceexplorer.Node
import com.microsoft.tooling.msservices.serviceexplorer.Sortable
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

class SortableTreeNode : DefaultMutableTreeNode, Sortable {
    private var node: Node?

    constructor() : super() {
        node = null
    }

    constructor(userObject: Node, allowsChildren: Boolean) : super(userObject, allowsChildren) {
        node = userObject
    }

    override fun add(newChild: MutableTreeNode?) {
        super.add(newChild)
    }

    override fun insert(newChild: MutableTreeNode?, childIndex: Int) {
        super.insert(newChild, childIndex)
    }

    override fun getPriority(): Int {
        return node?.priority ?: Sortable.DEFAULT_PRIORITY
    }
}