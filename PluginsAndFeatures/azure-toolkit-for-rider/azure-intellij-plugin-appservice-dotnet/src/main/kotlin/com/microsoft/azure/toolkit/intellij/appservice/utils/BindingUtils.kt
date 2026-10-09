/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.utils

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.dsl.builder.Cell
import com.intellij.util.ui.launchOnShow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.swing.JToggleButton
import javax.swing.JSpinner
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.text.JTextComponent

internal fun <T : JTextComponent> Cell<T>.bindText(flow: MutableStateFlow<String>): Cell<T> =
    applyToComponent {
        launchOnShow("Text field state binding") {
            val listener = object : DocumentAdapter() {
                var isActive = true

                override fun textChanged(e: DocumentEvent) {
                    if (isActive) {
                        flow.value = text
                    }
                }
            }
            document.addDocumentListener(listener)

            try {
                flow.collect {
                    try {
                        listener.isActive = false
                        if (text != it) text = it
                    } finally {
                        listener.isActive = true
                    }
                }
            } finally {
                document.removeDocumentListener(listener)
            }
        }
    }

internal fun <T : JSpinner> Cell<T>.bindIntValue(flow: MutableStateFlow<Int>): Cell<T> =
    applyToComponent {
        launchOnShow("Spinner state binding") {
            val listener = object : ChangeListener {
                var isActive = true

                override fun stateChanged(e: ChangeEvent) {
                    if (isActive) {
                        flow.value = (value as Number).toInt()
                    }
                }
            }
            addChangeListener(listener)

            try {
                flow.collect {
                    try {
                        listener.isActive = false
                        value = it
                    } finally {
                        listener.isActive = true
                    }
                }
            } finally {
                removeChangeListener(listener)
            }
        }
    }

fun Cell<JToggleButton>.bindSelected(
    isSelected: Flow<Boolean>,
    onSelected: (selected: Boolean) -> Unit
): Cell<JToggleButton> =
    applyToComponent {
        bindSelected(isSelected, onSelected)
    }

private fun JToggleButton.bindSelected(isSelected: Flow<Boolean>, onSelected: (selected: Boolean) -> Unit) {
    launchOnShow("Checkbox state binding") {
        val listener = object : ChangeListener {
            var isActive = true

            override fun stateChanged(e: ChangeEvent) {
                if (isActive) {
                    onSelected(model.isSelected)
                }
            }
        }
        model.addChangeListener(listener)

        try {
            isSelected.collect {
                try {
                    listener.isActive = false
                    model.isSelected = it
                } finally {
                    listener.isActive = true
                }
            }
        } finally {
            model.removeChangeListener(listener)
        }
    }
}

fun <T> Cell<ComboBox<T>>.bindItems(flow: StateFlow<List<T>>): Cell<ComboBox<T>> =
    applyToComponent {
        bindItems(flow)
    }

private fun <T> ComboBox<T>.bindItems(flow: StateFlow<List<T>>) {
    launchOnShow("ComboBox items binding") {
        flow.collect { items ->
            (model as MutableCollectionComboBoxModel<T>).update(items)
        }
    }
}

fun <T : Any> Cell<ComboBox<T>>.bindSelectedItem(flow: MutableStateFlow<T?>): Cell<ComboBox<T>> =
    applyToComponent {
        bindSelectedItem(flow)
    }

fun <T : Any> ComboBox<T>.bindSelectedItem(flow: MutableStateFlow<T?>) {
    launchOnShow("ComboBox selection binding") {
        val listener = object : ListDataListener {
            var isActive = true

            override fun contentsChanged(e: ListDataEvent) {
                if (isActive && e.index0 == -1 && e.index1 == -1) {
                    @Suppress("UNCHECKED_CAST")
                    flow.value = (model.selectedItem as T?)
                }
            }

            override fun intervalAdded(e: ListDataEvent) {}
            override fun intervalRemoved(e: ListDataEvent) {}
        }
        model.addListDataListener(listener)

        try {
            flow.collect {
                try {
                    listener.isActive = false
                    selectedItem = it
                } finally {
                    listener.isActive = true
                }
            }
        } finally {
            model.removeListDataListener(listener)
        }
    }
}
