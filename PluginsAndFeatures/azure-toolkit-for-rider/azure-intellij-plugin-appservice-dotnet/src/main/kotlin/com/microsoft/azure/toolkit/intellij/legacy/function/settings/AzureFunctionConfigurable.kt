/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.PathUtil
import com.intellij.util.asSafely
import com.intellij.util.ui.*
import com.microsoft.azure.toolkit.intellij.common.getSelectedValue
import com.microsoft.azure.toolkit.intellij.legacy.function.isFunctionCoreToolsExecutable
import java.io.File
import javax.swing.JTable
import javax.swing.ListSelectionModel

class AzureFunctionConfigurable : BoundConfigurable("Functions") {
    private val isCoreToolsFeedEnabled = Registry.`is`("azure.function_app.core_tools.feed.enabled")

    private val settings get() = AzureFunctionSettings.getInstance()

    private lateinit var coreToolsEditorModel: ListTableModel<AzureCoreToolsPathEntry>
    private lateinit var coreToolsEditor: TableView<AzureCoreToolsPathEntry>

    private val coreToolsEditorColumns = arrayOf<ColumnInfo<*, *>>(
        object : ColumnInfo<AzureCoreToolsPathEntry, String>("Azure Functions Version") {
            override fun valueOf(item: AzureCoreToolsPathEntry) = item.functionsVersion
            override fun setValue(item: AzureCoreToolsPathEntry, value: String) {
                item.functionsVersion = value
            }

            override fun isCellEditable(item: AzureCoreToolsPathEntry) = false
        },
        object : ColumnInfo<AzureCoreToolsPathEntry, String?>("Core Tools Path") {
            override fun valueOf(item: AzureCoreToolsPathEntry) = PathUtil.toSystemDependentName(item.coreToolsPath)
            override fun setValue(item: AzureCoreToolsPathEntry, value: String?) {
                item.coreToolsPath = value?.trim() ?: ""
            }

            override fun isCellEditable(item: AzureCoreToolsPathEntry) = true

            override fun getRenderer(item: AzureCoreToolsPathEntry) = object : ColoredTableCellRenderer() {
                override fun customizeCellRenderer(
                    table: JTable,
                    value: Any?,
                    selected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ) {
                    clear()

                    if (isCoreToolsFeedEnabled && item.coreToolsPath.isEmpty()) {
                        append("Managed by Rider", SimpleTextAttributes.GRAY_ATTRIBUTES)
                    } else if (isFunctionCoreToolsExecutable(item.coreToolsPath)) {
                        append("From environment PATH", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    } else {
                        val coreToolsFile = File(item.coreToolsPath)

                        val attributes = if (coreToolsFile.exists())
                            SimpleTextAttributes.REGULAR_ATTRIBUTES
                        else
                            SimpleTextAttributes.ERROR_ATTRIBUTES

                        append(valueOf(item), attributes)
                    }
                }
            }

            override fun getEditor(item: AzureCoreToolsPathEntry) = object : AbstractTableCellEditor() {
                private val comboBox = AzureFunctionComponentBrowseButton()

                init {
                    if (isCoreToolsFeedEnabled) {
                        comboBox.setPath(
                            CoreToolsComboBoxItem("Managed by Rider", "", true),
                            item.coreToolsPath.isEmpty()
                        )
                    }

                    comboBox.setPath(
                        CoreToolsComboBoxItem("From environment PATH", "func", true),
                        isFunctionCoreToolsExecutable(item.coreToolsPath)
                    )

                    if (item.coreToolsPath.isNotEmpty() && !isFunctionCoreToolsExecutable(item.coreToolsPath)) {
                        comboBox.setPath(
                            CoreToolsComboBoxItem(item.coreToolsPath, item.coreToolsPath, false),
                            true
                        )
                    }
                }

                override fun getCellEditorValue(): String? {
                    comboBox.childComponent.editor.item.asSafely<String>()?.let { textEntry ->
                        if (textEntry.isEmpty() || isFunctionCoreToolsExecutable(textEntry) || File(textEntry).exists()) {
                            return textEntry
                        }
                    }

                    return comboBox.getPath()
                }

                override fun getTableCellEditorComponent(
                    table: JTable?,
                    value: Any?,
                    isSelected: Boolean,
                    row: Int,
                    column: Int
                ) = CellEditorComponentWithBrowseButton(comboBox, this)
            }
        }
    )

    override fun createPanel() = panel {
        row {
            text("Configure the Azure Functions Core Tools to be used for an Azure Functions version")
        }
        row {
            val azureCoreToolsPathEntries = settings.azureCoreToolsPathEntries
            coreToolsEditorModel = ListTableModel(coreToolsEditorColumns, azureCoreToolsPathEntries, 0)
            coreToolsEditor = TableView(coreToolsEditorModel).apply {
                setShowGrid(false)
                setEnableAntialiasing(true)
                preferredScrollableViewportSize = JBUI.size(200, 100)
                emptyText.text = "No Azure Functions Core Tools configured"
                selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
                columnModel.getColumn(0).preferredWidth = JBUI.scale(250)
                columnModel.getColumn(1).preferredWidth = JBUI.scale(750)
                TableSpeedSearch.installOn(this)
            }
            scrollCell(coreToolsEditor)
                .align(AlignX.FILL)
                .onIsModified {
                    if (coreToolsEditor.isEditing) return@onIsModified true

                    val settingEntries = settings.azureCoreToolsPathEntries
                    for (i in 0..<coreToolsEditor.rowCount) {
                        val row = coreToolsEditor.getRow(i)
                        val entry = settingEntries.firstOrNull { it.functionsVersion == row.functionsVersion }
                            ?: continue
                        val cell = coreToolsEditor.getCellEditor(i, 1)

                        if (cell.cellEditorValue != entry.coreToolsPath) return@onIsModified true
                    }

                    return@onIsModified false
                }
                .onApply {
                    if (coreToolsEditor.isEditing) {
                        coreToolsEditor.stopEditing()
                    }
                    settings.azureCoreToolsPathEntries = coreToolsEditorModel.items
                }
        }
        row("Tool download path:") {
            textFieldWithBrowseButton(
                FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Azure Functions Core Tools Download Path")
            )
                .align(AlignX.FILL)
                .validationOnInput {
                    if (it.text.isNotEmpty() && !File(it.text).exists()) {
                        error("Not a valid path")
                    } else {
                        null
                    }
                }
                .bindText(settings::functionDownloadPath)

        }.visible(isCoreToolsFeedEnabled)
        row {
            checkBox("Check for Azure Functions missing nuget packages")
                .bindSelected( settings::checkForFunctionMissingPackages)
        }
    }
}

private data class CoreToolsComboBoxItem(val label: String, val value: String, val isPredefinedEntry: Boolean) {
    override fun toString() = label
}

private class AzureFunctionToolsComboBox : ComboBox<CoreToolsComboBoxItem>() {
    override fun isEditable() = true
    fun getPath() = getSelectedValue()?.value
    fun setPath(path: CoreToolsComboBoxItem, selected: Boolean = false) {
        addItem(path)
        if (selected) selectedItem = path
    }
}

private class AzureFunctionComponentBrowseButton :
    ComponentWithBrowseButton<AzureFunctionToolsComboBox>(AzureFunctionToolsComboBox(), null) {
    init {
        val fileBrowserAccessor = object : TextComponentAccessor<AzureFunctionToolsComboBox> {
            override fun getText(component: AzureFunctionToolsComboBox) = component.getPath() ?: ""
            override fun setText(component: AzureFunctionToolsComboBox, text: String) {
                val normalizedText = PathUtil.toSystemDependentName(text)
                component.setPath(CoreToolsComboBoxItem(normalizedText, normalizedText, false), true)
            }
        }
        addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.singleFile(),
            fileBrowserAccessor
        )
    }

    fun getPath() = childComponent.getPath()
    fun setPath(path: CoreToolsComboBoxItem, selected: Boolean = false) {
        childComponent.setPath(path, selected)
    }
}