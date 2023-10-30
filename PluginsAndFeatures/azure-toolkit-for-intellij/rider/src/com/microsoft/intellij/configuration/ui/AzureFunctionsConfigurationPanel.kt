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
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.PathUtil
import com.intellij.util.asSafely
import com.intellij.util.ui.*
import com.microsoft.intellij.configuration.AzureRiderAbstractConfigurable
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.ui.extension.getSelectedValue
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.orWhenNullOrEmpty
import java.awt.Component
import java.io.File
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellRenderer

@Suppress("UnstableApiUsage")
class AzureFunctionsConfigurationPanel(parentDisposable: Disposable)
    : AzureRiderAbstractConfigurable(message("settings.app_services.function_app.name"), parentDisposable) {

    private val properties: PropertiesComponent = PropertiesComponent.getInstance()

    private lateinit var coreToolsConfiguration: List<AzureRiderSettings.AzureCoreToolsPathEntry>
    private lateinit var coreToolsEditorModel: ListTableModel<AzureRiderSettings.AzureCoreToolsPathEntry>
    private lateinit var coreToolsEditor: TableView<AzureRiderSettings.AzureCoreToolsPathEntry>

    private val isCoreToolsFeedEnabled = Registry.`is`("azure.function_app.core_tools.feed.enabled")

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

                override fun getEditor(item: AzureRiderSettings.AzureCoreToolsPathEntry) = object : AbstractTableCellEditor() {
                    private val comboBox = AzureFunctionComponentBrowseButton()

                    init {
                        val coreToolsPath = File(item.coreToolsPath)

                        // Setup values for editor
                        if (isCoreToolsFeedEnabled) {
                            comboBox.setPath(
                                    CoreToolsComboBoxItem(
                                            message("settings.app_services.function_app.core_tools.configuration.managed_by_ide"),
                                            "",
                                            true
                                    ),
                                    item.coreToolsPath.isEmpty()
                            )
                        }

                        comboBox.setPath(
                                CoreToolsComboBoxItem(message(
                                        "settings.app_services.function_app.core_tools.configuration.func_from_path"),
                                        "func",
                                        true
                                ),
                                coreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)
                        )

                        if (item.coreToolsPath.isNotEmpty() && !coreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)) {
                            comboBox.setPath(CoreToolsComboBoxItem(item.coreToolsPath, item.coreToolsPath, false), true)
                        }
                    }

                    override fun getCellEditorValue(): String? {
                        comboBox.childComponent.editor.item.asSafely<String>()?.let { textEntry ->
                            if (textEntry.isEmpty()) {
                                return ""
                            }

                            val coreToolsPath = File(textEntry)
                            if (coreToolsPath.exists() || coreToolsPath.nameWithoutExtension.equals("func", ignoreCase = true)) {
                                return textEntry
                            }
                        }

                        return comboBox.getPath()
                    }

                    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
                        return CellEditorComponentWithBrowseButton(comboBox, this)
                    }
                }

                override fun isCellEditable(item: AzureRiderSettings.AzureCoreToolsPathEntry) = true
            }
    )

    private fun createPanel(): DialogPanel = panel {

        group(message("settings.app_services.function_app.core_tools.group")) {
            coreToolsConfiguration = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)

            coreToolsEditorModel = ListTableModel(
                    coreToolsEditorColumns,
                    coreToolsConfiguration,
                    0
            )

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
                scrollCell(coreToolsEditor)
                        .onIsModified {
                            if (coreToolsEditor.isEditing) return@onIsModified true

                            val coreToolsConfiguration = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)
                            for (i in 0 ..< coreToolsEditor.rowCount) {
                                val row = coreToolsEditor.getRow(i)
                                val configuration = coreToolsConfiguration.firstOrNull { it.functionsVersion == row.functionsVersion } ?: return@onIsModified true
                                val editor = coreToolsEditor.getCellEditor(i, 1)

                                if (editor.cellEditorValue != configuration.coreToolsPath) return@onIsModified true
                            }

                            return@onIsModified false
                        }
                        .onApply {
                            if (coreToolsEditor.isEditing) {
                                coreToolsEditor.stopEditing()
                            }
                            AzureRiderSettings.setAzureCoreToolsPathEntries(properties, coreToolsEditorModel.items)
                        }
                        .align(AlignX.FILL)
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
            if (textField.text.isNotEmpty() && !File(textField.text).exists()) {
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
        super.reset()
    }

    override fun isProjectLevel() = false
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

private class AzureFunctionComponentBrowseButton : ComponentWithBrowseButton<AzureFunctionToolsComboBox>(AzureFunctionToolsComboBox(), null) {
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
                null,
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                fileBrowserAccessor
        )
    }

    fun getPath() = childComponent.getPath()
    fun setPath(path: CoreToolsComboBoxItem, selected: Boolean = false) {
        childComponent.setPath(path, selected)
    }
}