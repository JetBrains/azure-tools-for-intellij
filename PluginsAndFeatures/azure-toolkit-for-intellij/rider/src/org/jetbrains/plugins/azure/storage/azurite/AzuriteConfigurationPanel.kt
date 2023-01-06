/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.storage.azurite

import com.intellij.ide.IdeBundle
import com.intellij.ide.util.PropertiesComponent
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.javascript.nodejs.util.NodePackageRef
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selectedValueIs
import com.intellij.util.application
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.configuration.ui.AzureRiderAbstractConfigurablePanel
import com.microsoft.intellij.helpers.validator.IpAddressInputValidator
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.orWhenNullOrEmpty
import java.io.File
import javax.swing.*

@Suppress("UNUSED_LAMBDA_EXPRESSION")
class AzuriteConfigurationPanel : AzureRiderAbstractConfigurablePanel, Disposable {

    private val disposable = Disposer.newDisposable()
    private val properties = PropertiesComponent.getInstance()
    private val project = ProjectManager.getInstance().defaultProject

    init {
        Disposer.register(application, this)
    }

    private fun createPanel(): DialogPanel =
            panel {
                group(RiderAzureBundle.message("settings.azurite.row.package")) {
                    // Node interpreter
                    val nodeInterpreterField = NodeJsInterpreterField(project, false)
                    row(JLabel(NodeJsInterpreterField.getLabelTextForComponent()).apply { labelFor = nodeInterpreterField }) {
                        var value = NodeJsInterpreterRef.create(properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_INTERPRETER) ?: "project")

                        cell(nodeInterpreterField).bind(
                                { nodeJsInterpreterField: NodeJsInterpreterField -> nodeJsInterpreterField.interpreterRef },
                                { nodeJsInterpreterField: NodeJsInterpreterField, interpreterRef: NodeJsInterpreterRef -> nodeJsInterpreterField.interpreterRef = interpreterRef },
                                MutableProperty({ value }, { value = it })
                        )
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_INTERPRETER, value.referenceName) }
                            .align(AlignX.FILL)
                    }

                    // Azurite package
                    val packageField = NodePackageField(nodeInterpreterField, Azurite.PackageName)
                    row(JLabel(RiderAzureBundle.message("settings.azurite.row.package.path")).apply { labelFor = packageField }) {
                        fun getNodePackageRefFromSettings(): NodePackageRef {
                            val packagePath = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE)
                            if (!packagePath.isNullOrEmpty()) {
                                val nodePackage = Azurite.PackageDescriptor.createPackage(packagePath)
                                return NodePackageRef.create(nodePackage)
                            }
                            return NodePackageRef.create(Azurite.PackageDescriptor.createPackage(""))
                        }
                        var value = getNodePackageRefFromSettings()

                        cell(packageField).bind(
                                { nodePackageField: NodePackageField -> nodePackageField.selectedRef },
                                { nodePackageField: NodePackageField, nodePackageRef: NodePackageRef -> nodePackageField.selectedRef = nodePackageRef },
                                MutableProperty({ value }, { value = it })
                        )
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE, value.constantPackage!!.systemDependentPath) }
                            .validationOnInput {
                                val selected = it.selected
                                if (selected.version != null && selected.version!!.major < 3) {
                                    ValidationInfo(RiderAzureBundle.message("settings.azurite.validation.invalid.package_version"), it)
                                } else {
                                    null
                                }
                            }
                            .align(AlignX.FILL)
                    }
                }

                group(RiderAzureBundle.message("settings.azurite.row.general")) {
                    // Workspace folder
                    lateinit var workspaceLocationCombo: Cell<ComboBox<AzureRiderSettings.AzuriteLocationMode>>
                    row(RiderAzureBundle.message("settings.azurite.row.general.workspace")) {
                        var value: AzureRiderSettings.AzuriteLocationMode? = AzureRiderSettings.getAzuriteWorkspaceMode(properties)

                        workspaceLocationCombo = comboBox(
                                DefaultComboBoxModel(arrayOf(AzureRiderSettings.AzuriteLocationMode.Managed, AzureRiderSettings.AzuriteLocationMode.Project, AzureRiderSettings.AzuriteLocationMode.Custom)),
                                object : ColoredListCellRenderer<AzureRiderSettings.AzuriteLocationMode>() {
                                    override fun customizeCellRenderer(list: JList<out AzureRiderSettings.AzuriteLocationMode>, value: AzureRiderSettings.AzuriteLocationMode, index: Int, selected: Boolean, hasFocus: Boolean) {
                                        this.clear()
                                        this.append(value.description)
                                    }
                                })
                                .bindItem(MutableProperty({ value }, { value = it }))
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_LOCATION_MODE, value?.name ?: AzureRiderSettings.AzuriteLocationMode.Managed.name ) }
                    }
                    row("") {
                        var value = if (AzureRiderSettings.getAzuriteWorkspaceMode(properties) == AzureRiderSettings.AzuriteLocationMode.Custom) {
                            FileUtil.toSystemDependentName(AzureRiderSettings.getAzuriteWorkspacePath(properties, project).absolutePath)
                        } else {
                            ""
                        }

                        textFieldWithBrowseButton(
                                RiderAzureBundle.message("settings.azurite.row.general.workspace.browse"),
                                null,
                                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        )
                            .bindText({ value }, { value = FileUtil.toSystemIndependentName(it) })
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_LOCATION, FileUtil.toSystemIndependentName(value)) }
                            .visibleIf(workspaceLocationCombo.component.selectedValueIs(AzureRiderSettings.AzuriteLocationMode.Custom))
                            .validationOnInput { validationForPath(it) }
                            .columns(1)
                            .align(AlignX.FILL)
                    }

                    // Loose mode
                    row {
                        var value = properties.getBoolean(AzureRiderSettings.PROPERTY_AZURITE_LOOSE_MODE)

                        checkBox(RiderAzureBundle.message("settings.azurite.row.general.loosemode"))
                            .bindSelected(MutableProperty({ value }, { value = it }))
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_LOOSE_MODE, value) }
                    }
                }

                // Host/port settings
                group(RiderAzureBundle.message("settings.azurite.row.host")) {
                    row {
                        label(RiderAzureBundle.message("settings.azurite.row.blob.host"))

                        var hostValue = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_HOST_DEFAULT)
                        textField()
                                .bindText(MutableProperty({ hostValue }, { hostValue = it }))
                                .validationOnInput { validationForIpAddress(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_HOST, hostValue) }

                        label(RiderAzureBundle.message("settings.azurite.row.blob.port"))

                        var portValue = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_PORT_DEFAULT)
                        textField()
                                .bindText(MutableProperty({ portValue }, { portValue = it }))
                                .validationOnInput { validationForPort(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_PORT, portValue) }
                    }.layout(RowLayout.PARENT_GRID)

                    row {
                        label(RiderAzureBundle.message("settings.azurite.row.queue.host"))

                        var hostValue = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_HOST_DEFAULT)
                        textField()
                                .bindText(MutableProperty({ hostValue }, { hostValue = it }))
                                .validationOnInput { validationForIpAddress(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_HOST, hostValue) }

                        label(RiderAzureBundle.message("settings.azurite.row.queue.port"))

                        var portValue = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_PORT_DEFAULT)
                        textField()
                                .bindText(MutableProperty({ portValue }, { portValue = it }))
                                .validationOnInput { validationForPort(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_PORT, portValue) }
                    }.layout(RowLayout.PARENT_GRID)

                    row {
                        label(RiderAzureBundle.message("settings.azurite.row.table.host"))

                        var hostValue = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_TABLE_HOST_DEFAULT)
                        textField()
                                .bindText(MutableProperty({ hostValue }, { hostValue = it }))
                                .validationOnInput { validationForIpAddress(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_HOST, hostValue) }

                        label(RiderAzureBundle.message("settings.azurite.row.table.port"))

                        var portValue = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_TABLE_PORT_DEFAULT)
                        textField()
                                .bindText(MutableProperty({ portValue }, { portValue = it }))
                                .validationOnInput { validationForPort(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_PORT, portValue) }
                    }.layout(RowLayout.PARENT_GRID)
                }

                // Certificate settings
                group(RiderAzureBundle.message("settings.azurite.row.certificate")) {
                    row(RiderAzureBundle.message("settings.azurite.row.certificate.path")) {
                        var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PATH)?.let { FileUtil.toSystemDependentName(it) } ?: ""

                        textFieldWithBrowseButton(
                                IdeBundle.message("dialog.title.select.0", "*.pem"),
                                null,
                                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                        )
                            .bindText({ value }, { value = FileUtil.toSystemIndependentName(it) })
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PATH, FileUtil.toSystemIndependentName(value)) }
                            .validationOnInput { validationForPath(it) }
                            .comment(RiderAzureBundle.message("settings.azurite.row.certificate.path.comment"))
                            .align(AlignX.FILL)
                    }

                    row(RiderAzureBundle.message("settings.azurite.row.certificate.keypath")) {
                        var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_KEY_PATH)?.let { FileUtil.toSystemDependentName(it) } ?: ""

                        textFieldWithBrowseButton(
                                IdeBundle.message("dialog.title.select.0", "*.key"),
                                null,
                                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                        )
                            .bindText({ value }, { value = FileUtil.toSystemIndependentName(it) })
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_KEY_PATH, FileUtil.toSystemIndependentName(value)) }
                            .validationOnInput { validationForPath(it) }
                            .comment(RiderAzureBundle.message("settings.azurite.row.certificate.keypath.comment"))
                            .align(AlignX.FILL)
                    }

                    row(RiderAzureBundle.message("settings.azurite.row.certificate.password")) {
                        var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PASSWORD) ?: ""

                        passwordField()
                            .bindText({ value }, { value = it })
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PASSWORD, value) }
                            .comment(RiderAzureBundle.message("settings.azurite.row.certificate.password.comment"))
                    }
                }
            }

    private fun validationForIpAddress(textField: JBTextField) =
            if (textField.text.isNullOrEmpty() || !IpAddressInputValidator.instance.validateIpV4Address(textField.text)) {
                ValidationInfo(RiderAzureBundle.message("settings.azurite.validation.invalid.ip"), textField)
            } else {
                null
            }

    private fun validationForPort(textField: JBTextField) =
            if (textField.text.toIntOrNull() == null) {
                ValidationInfo(RiderAzureBundle.message("settings.azurite.validation.invalid.port"), textField)
            } else {
                null
            }

    private fun validationForPath(textField: TextFieldWithBrowseButton) =
            if (!textField.text.isNullOrEmpty() && !File(textField.text).exists()) {
                ValidationInfo(RiderAzureBundle.message("settings.azurite.validation.invalid.path"), textField)
            } else {
                null
            }

    override val panel = createPanel().apply {
        registerValidators(disposable)
        reset()
    }

    override val displayName: String = RiderAzureBundle.message("settings.azurite.name")
    override fun isModified() = panel.isModified()

    override fun doResetAction() = panel.reset()

    override fun doOKAction() {
        panel.apply()
        Disposer.dispose(disposable)
    }

    override fun dispose() {
        Disposer.dispose(disposable)
    }
}