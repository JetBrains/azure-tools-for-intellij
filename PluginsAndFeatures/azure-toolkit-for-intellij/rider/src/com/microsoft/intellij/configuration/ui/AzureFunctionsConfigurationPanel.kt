/**
 * Copyright (c) 2019-2023 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("MissingRecentApi")

package com.microsoft.intellij.configuration.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.*
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.InsertPathAction
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.PathUtil
import com.intellij.util.asSafely
import com.intellij.util.ui.*
import com.microsoft.intellij.configuration.AzureRiderAbstractConfigurable
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.ui.extension.getSelectedValue
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.orWhenNullOrEmpty
import java.io.File
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

@Suppress("UnstableApiUsage")
class AzureFunctionsConfigurationPanel(parentDisposable: Disposable)
    : AzureRiderAbstractConfigurable(message("settings.app_services.function_app.name"), parentDisposable) {

    private val properties: PropertiesComponent = PropertiesComponent.getInstance()

    private lateinit var coreToolsEditorModel: ListTableModel<AzureRiderSettings.AzureCoreToolsPathEntry>
    private lateinit var coreToolsEditor: JBTable

    private val isCoreToolsFeedEnabled = Registry.`is`("azure.function_app.core_tools.feed.enabled")

    private var coreToolsEditorChanged = false
    private val coreToolsEditorColumns = arrayOf<ColumnInfo<*, *>>(
            object : ColumnInfo<AzureRiderSettings.AzureCoreToolsPathEntry, String>(message("settings.app_services.function_app.core_tools.configuration.column.functionsVersion")) {
                override fun valueOf(item: AzureRiderSettings.AzureCoreToolsPathEntry) = item.functionsVersion

                override fun setValue(item: AzureRiderSettings.AzureCoreToolsPathEntry, value: String) {
                    item.functionsVersion = value
                }

                override fun isCellEditable(item: AzureRiderSettings.AzureCoreToolsPathEntry) = false
            },
            object : ColumnInfo<AzureRiderSettings.AzureCoreToolsPathEntry, String?>(message("settings.app_services.function_app.core_tools.configuration.column.coreToolsPath")) {
                override fun valueOf(item: AzureRiderSettings.AzureCoreToolsPathEntry) = PathUtil.toSystemDependentName(item.coreToolsPath)

                override fun setValue(item: AzureRiderSettings.AzureCoreToolsPathEntry, value: String?) {
                    item.coreToolsPath = value?.trim() ?: ""
                }

                override fun getRenderer(item: AzureRiderSettings.AzureCoreToolsPathEntry): TableCellRenderer {
                    return object : ColoredTableCellRenderer() {
                        override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
                            clear()

                            if (isCoreToolsFeedEnabled && item.coreToolsPath.isEmpty()) {
                                append(message("settings.app_services.function_app.core_tools.configuration.managed_by_ide"), SimpleTextAttributes.GRAY_ATTRIBUTES)
                            } else {
                                val coreToolsFile = File(item.coreToolsPath)

                                if (coreToolsFile.nameWithoutExtension.equals("func", ignoreCase = true)) {
                                    append(message("settings.app_services.function_app.core_tools.configuration.func_from_path"), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                                } else {
                                    val attributes = if (coreToolsFile.exists())
                                        SimpleTextAttributes.REGULAR_ATTRIBUTES
                                    else
                                        SimpleTextAttributes.ERROR_ATTRIBUTES

                                    append(valueOf(item), attributes)
                                }
                            }
                        }
                    }
                }

                override fun getEditor(item: AzureRiderSettings.AzureCoreToolsPathEntry): TableCellEditor? = object : AbstractTableCellEditor() {

                    private val comboBox: ComboBox<CoreToolsComboBoxItem> = ComboBox<CoreToolsComboBoxItem>()

                    init {
                        val coreToolsPath = File(item.coreToolsPath)

                        // Setup values for editor
                        if (isCoreToolsFeedEnabled) {
                            comboBox.addItem(CoreToolsComboBoxItem(message("settings.app_services.function_app.core_tools.configuration.managed_by_ide"), "", true))
                            if (item.coreToolsPath.isEmpty()) comboBox.selectedIndex = comboBox.itemCount - 1
                        }

                        comboBox.addItem(CoreToolsComboBoxItem(message("settings.app_services.function_app.core_tools.configuration.func_from_path"), "func", true))
                        if (coreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)) comboBox.selectedIndex = comboBox.itemCount - 1

                        if (item.coreToolsPath.isNotEmpty() && !coreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)) {
                            comboBox.addItem(CoreToolsComboBoxItem(item.coreToolsPath, item.coreToolsPath, false))
                            comboBox.selectedIndex = comboBox.itemCount - 1
                        }

                        // Setup editor
                        val fileBrowserAccessor = object : TextComponentAccessor<ComboBox<CoreToolsComboBoxItem>> {
                            override fun getText(component: ComboBox<CoreToolsComboBoxItem>) = component.getSelectedValue()?.value ?: ""
                            override fun setText(component: ComboBox<CoreToolsComboBoxItem>, text: String) {
                                val normalizedText = PathUtil.toSystemDependentName(text)
                                comboBox.addItem(CoreToolsComboBoxItem(normalizedText, normalizedText, false))
                                comboBox.selectedIndex = comboBox.itemCount - 1
                            }
                        }
                        val selectFolderAction = BrowseFolderRunnable<ComboBox<CoreToolsComboBoxItem>>(
                                null,
                                null,
                                null,
                                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                                comboBox,
                                fileBrowserAccessor
                        )

                        comboBox.isEditable = true
                        comboBox.editor = object : BasicComboBoxEditor() {
                            override fun createEditorComponent(): JTextField {
                                val editor = ExtendableTextField()
                                editor.addBrowseExtension(selectFolderAction, null)
                                editor.border = null
                                InsertPathAction.addTo(editor, FileChooserDescriptorFactory.createSingleFolderDescriptor())
                                return editor
                            }
                        }

                        comboBox.addItemListener {
                            coreToolsEditorChanged = true
                        }
                    }

                    override fun getCellEditorValue(): String {
                        // Allow for manual input
                        comboBox.editor.item.asSafely<String>()?.let { textEntry ->
                            if (textEntry.isEmpty()) {
                                return ""
                            }

                            val coreToolsPath = File(textEntry)
                            if (coreToolsPath.exists() || coreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)) {
                                return textEntry
                            }
                        }

                        return comboBox.getSelectedValue()?.value ?: ""
                    }

                    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int) = comboBox
                }

                override fun isCellEditable(item: AzureRiderSettings.AzureCoreToolsPathEntry) = true
            }
    )

    private fun createPanel(): DialogPanel = panel {

        group(message("settings.app_services.function_app.core_tools.group")) {
            val coreToolsConfiguration = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)

            coreToolsEditorModel = ListTableModel(
                    coreToolsEditorColumns,
                    coreToolsConfiguration,
                    0
            ).apply {
                onIsModified {
                    coreToolsEditorChanged
                }
            }

            coreToolsEditor = TableView(coreToolsEditorModel).apply {
                setShowGrid(false)
                setEnableAntialiasing(true)
                preferredScrollableViewportSize = JBUI.size(200, 100)

                emptyText.text = message("settings.app_services.function_app.core_tools.configuration.empty_list")

                TableSpeedSearch.installOn(this)

                selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

                columnModel.getColumn(0).preferredWidth = JBUI.scale(250)
                columnModel.getColumn(1).preferredWidth = JBUI.scale(750)
            }

            row {
                text(message("settings.app_services.function_app.core_tools.description"))
            }

            row {
                scrollCell(coreToolsEditor).onApply {
                    AzureRiderSettings.setAzureCoreToolsPathEntries(properties, coreToolsEditorModel.items)
                    coreToolsEditorChanged = false
                }.align(AlignX.FILL)
            }

            if (isCoreToolsFeedEnabled) {
                row {
                    text(message("settings.app_services.function_app.core_tools.download_path"))
                }
                row {
                    var value = FileUtil.toSystemDependentName(properties.getValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH)
                            .orWhenNullOrEmpty(AzureRiderSettings.VALUE_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH))

                    textFieldWithBrowseButton(
                            message("settings.app_services.function_app.core_tools.download_path_description"),
                            null,
                            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    )
                        .bindText({ value }, { value = FileUtil.toSystemIndependentName(it) })
                        .onApply { properties.setValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH, value) }
                        .validationOnInput { validationForPath(it) }
                        .align(AlignX.FILL)
                }
            }
        }

        row {
            placeholder()
        }
    }

    private fun validationForPath(textField: TextFieldWithBrowseButton) =
            if (!textField.text.isNullOrEmpty() && !File(textField.text).exists()) {
                ValidationInfo(message("settings.app_services.function_app.core_tools.download_path.invalid"), textField)
            } else {
                null
            }

    override val panel = createPanel().apply {
        registerValidators(parentDisposable)
        reset()
    }

    override fun reset() {
        val coreToolsConfiguration = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)
        coreToolsEditorModel.items = coreToolsConfiguration
        coreToolsEditorChanged = false
        super.reset()
    }

    override fun isProjectLevel() = false

    private data class CoreToolsComboBoxItem(val label: String, val value: String, val isPredefinedEntry: Boolean) {

        override fun toString() = label
    }
}

