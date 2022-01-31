/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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

package com.microsoft.intellij.configuration.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.layout.noGrowY
import com.intellij.ui.layout.panel
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.PathUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.LocalPathCellEditor
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.io.File
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class AzureFunctionsConfigurationPanel: AzureRiderAbstractConfigurablePanel {

    private val properties: PropertiesComponent = PropertiesComponent.getInstance()

    private lateinit var coreToolsEditorModel: ListTableModel<AzureRiderSettings.AzureCoreToolsPathEntry>
    private lateinit var coreToolsEditor: JBTable
    private lateinit var coreToolsDownloadPathEditor: TextFieldWithBrowseButton

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

                            if (item.coreToolsPath.isEmpty()) {
                                append(message("settings.app_services.function_app.core_tools.configuration.managed_by_ide"), SimpleTextAttributes.GRAY_ATTRIBUTES)
                            } else {
                                val coreToolsFile = File(item.coreToolsPath)

                                val attributes = if (coreToolsFile.exists() || coreToolsFile.nameWithoutExtension.equals("func", ignoreCase = true))
                                    SimpleTextAttributes.REGULAR_ATTRIBUTES
                                else
                                    SimpleTextAttributes.ERROR_ATTRIBUTES

                                append(valueOf(item), attributes)
                            }
                        }
                    }
                }

                override fun getEditor(item: AzureRiderSettings.AzureCoreToolsPathEntry): TableCellEditor? = LocalPathCellEditor()
                        .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
                        .normalizePath(true)

                override fun isCellEditable(item: AzureRiderSettings.AzureCoreToolsPathEntry) = true
            }
    )

    override val displayName: String = message("settings.app_services.function_app.name")

    override val panel: JPanel = panel {

        val coreToolsConfiguration = AzureRiderSettings.getAzureCoreToolsPathEntries(properties)

        coreToolsEditorModel = ListTableModel(
                coreToolsEditorColumns,
                coreToolsConfiguration,
                0
        )

        coreToolsEditor = TableView(coreToolsEditorModel).apply {
            setShowGrid(false)
            setEnableAntialiasing(true)
            preferredScrollableViewportSize = JBUI.size(200, 100)

            emptyText.setText(message("settings.app_services.function_app.core_tools.configuration.empty_list"))

            TableSpeedSearch(this)

            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

            columnModel.getColumn(0).preferredWidth = JBUI.scale(250)
            columnModel.getColumn(1).preferredWidth = JBUI.scale(750)
        }

        coreToolsDownloadPathEditor = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                    "",
                    message("settings.app_services.function_app.core_tools.download_path_description"),
                    null,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            )

            text = FileUtil.toSystemDependentName(
                    properties.getValue(
                            AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH,
                            AzureRiderSettings.VALUE_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH))
        }

        noteRow(message("settings.app_services.function_app.core_tools.description"))

        row {
            cell(isFullWidth = true) {
                scrollPane(coreToolsEditor).noGrowY()
            }
        }

        row(message("settings.app_services.function_app.core_tools.download_path")) { }
        row {
            cell(isFullWidth = true) {
                component(coreToolsDownloadPathEditor)
            }
        }

        row {
            placeholder().constraints(growY, pushY)
        }
    }

    override fun doOKAction() {

        AzureRiderSettings.setAzureCoreToolsPathEntries(properties, coreToolsEditorModel.items)

        if (coreToolsDownloadPathEditor.text != "" && File(coreToolsDownloadPathEditor.text).exists()) {
            properties.setValue(
                    AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH,
                    FileUtil.toSystemIndependentName(coreToolsDownloadPathEditor.text))
        }
    }
}
