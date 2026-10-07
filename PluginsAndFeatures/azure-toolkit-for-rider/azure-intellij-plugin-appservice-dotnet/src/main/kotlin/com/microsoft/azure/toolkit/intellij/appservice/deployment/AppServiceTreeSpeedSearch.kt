/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.intellij.ui.SearchTextField
import com.intellij.ui.TreeSpeedSearch
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.TreePath

internal class AppServiceTreeSpeedSearch private constructor(
    tree: JTree,
    canExpand: Boolean,
    presentableStringFunction: (TreePath) -> String,
    private val searchTextField: SearchTextField
) : TreeSpeedSearch(tree, canExpand, null, presentableStringFunction) {

    private val popup = Popup(searchTextField.text, searchTextField)

    init {
        mySearchPopup = popup
    }

    companion object {
        fun installOn(
            tree: JTree,
            searchTextField: SearchTextField,
            canExpand: Boolean = true,
            presentableStringFunction: (TreePath) -> String = { it.lastPathComponent.toString() }
        ): AppServiceTreeSpeedSearch {
            val search = AppServiceTreeSpeedSearch(tree, canExpand, presentableStringFunction, searchTextField)
            search.setupListeners()
            return search
        }
    }

    override fun showPopup(searchText: String?) {
        mySearchPopup?.refreshSelection()
    }

    override fun hidePopup() {}

    override fun moveSearchPopup() {}

    override fun manageSearchPopup(popup: SearchPopup?) {}

    override fun keepEvenWhenFocusLost(): Boolean = true

    override fun createPopup(searchText: String?): SearchPopup = popup

    override fun isPopupActive(): Boolean = true

    override fun getSearchField(): JTextField = searchTextField.textEditor

    private fun expandMatchingPaths(pattern: String) {
        if (pattern.isBlank()) return
        val it = getElementIterator(0)
        while (it.hasNext()) {
            val element = it.next()
            if (isMatchingElement(element, pattern)) {
                val path = element as TreePath
                val parentPath = path.parentPath
                if (parentPath != null) {
                    myComponent.expandPath(parentPath)
                }
            }
        }
    }

    private inner class Popup(
        initialString: String?,
        private val textField: SearchTextField
    ) : SearchPopup(initialString) {
        init {
            textField.textEditor.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    val text = textField.text
                    val element = when (e.keyCode) {
                        KeyEvent.VK_DOWN -> findNextElement(text)
                        KeyEvent.VK_UP -> findPreviousElement(text)
                        else -> return
                    }
                    updateSelection(element, text)
                    e.consume()
                }
            })

            textField.textEditor.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = onTextChanged()
                override fun removeUpdate(e: DocumentEvent?) = onTextChanged()
                override fun changedUpdate(e: DocumentEvent?) = onTextChanged()

                private fun onTextChanged() {
                    val text = textField.text
                    updateSelection(findElement(text), text)
                    expandMatchingPaths(text)
                    myComponent.repaint()
                }
            })
        }
    }
}