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
import com.intellij.ui.layout.*
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
                titledRow(RiderAzureBundle.message("settings.azurite.row.package")) {
                    // Node interpreter
                    val nodeInterpreterField = NodeJsInterpreterField(project, false)
                    row(JLabel(NodeJsInterpreterField.getLabelTextForComponent()).apply { labelFor = nodeInterpreterField }) {
                        var value = NodeJsInterpreterRef.create(properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_INTERPRETER) ?: "project")

                        nodeInterpreterField().withBinding(
                                { it.interpreterRef },
                                { nodeJsInterpreterField, interpreterRef -> nodeJsInterpreterField.interpreterRef = interpreterRef },
                                PropertyBinding(
                                        { value },
                                        { value = it })
                        )
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_INTERPRETER, value.referenceName) }
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

                        packageField().withBinding(
                                { it.selectedRef },
                                { nodePackageField, nodePackageRef -> nodePackageField.selectedRef = nodePackageRef },
                                PropertyBinding(
                                        { value },
                                        { value = it })
                        )
                            .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE, value.constantPackage!!.systemDependentPath) }
                            .withValidationOnInput {
                                val selected = it.selected
                                if (selected.version != null && selected.version!!.major < 3) {
                                    ValidationInfo(RiderAzureBundle.message("settings.azurite.validation.invalid.package_version"), it)
                                } else {
                                    null
                                }
                            }
                    }
                }

                titledRow(RiderAzureBundle.message("settings.azurite.row.general")) {
                    // Workspace folder
                    lateinit var workspaceLocationCombo: CellBuilder<ComboBox<AzureRiderSettings.AzuriteLocationMode>>
                    row(RiderAzureBundle.message("settings.azurite.row.general.workspace")) {
                        cell {
                            var value: AzureRiderSettings.AzuriteLocationMode? = AzureRiderSettings.getAzuriteWorkspaceMode(properties)

                            workspaceLocationCombo = comboBox(
                                    DefaultComboBoxModel(arrayOf(AzureRiderSettings.AzuriteLocationMode.Managed, AzureRiderSettings.AzuriteLocationMode.Project, AzureRiderSettings.AzuriteLocationMode.Custom)),
                                    { value },
                                    { value = it },
                                    object : ColoredListCellRenderer<AzureRiderSettings.AzuriteLocationMode>() {
                                        override fun customizeCellRenderer(list: JList<out AzureRiderSettings.AzuriteLocationMode>, value: AzureRiderSettings.AzuriteLocationMode, index: Int, selected: Boolean, hasFocus: Boolean) {
                                            this.clear()
                                            this.append(value.description)
                                        }
                                    }).onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_LOCATION_MODE, value?.name ?: AzureRiderSettings.AzuriteLocationMode.Managed.name ) }
                        }
                    }
                    row("") {
                        cell {
                            var value = if (AzureRiderSettings.getAzuriteWorkspaceMode(properties) == AzureRiderSettings.AzuriteLocationMode.Custom) {
                                FileUtil.toSystemDependentName(AzureRiderSettings.getAzuriteWorkspacePath(properties, project).absolutePath)
                            } else {
                                ""
                            }

                            textFieldWithBrowseButton(
                                    { value },
                                    { value = it },
                                    RiderAzureBundle.message("settings.azurite.row.general.workspace.browse"),
                                    null,
                                    FileChooserDescriptorFactory.createSingleFolderDescriptor())
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_LOCATION, FileUtil.toSystemIndependentName(value)) }
                                .visibleIf(workspaceLocationCombo.component.selectedValueIs(AzureRiderSettings.AzuriteLocationMode.Custom))
                                .withValidationOnInput { validationForPath(it) }
                        }
                    }

                    // Loose mode
                    row {
                        cell {
                            var value = properties.getBoolean(AzureRiderSettings.PROPERTY_AZURITE_LOOSE_MODE)

                            checkBox(RiderAzureBundle.message("settings.azurite.row.general.loosemode"),
                                    { value },
                                    { value = it })
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_LOOSE_MODE, value) }
                        }
                    }
                }

                // Host/port settings
                titledRow(RiderAzureBundle.message("settings.azurite.row.host")) {
                    row {
                        val blobHostLabel = JLabel(RiderAzureBundle.message("settings.azurite.row.blob.host"))
                        blobHostLabel()

                        cell {
                            var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_HOST_DEFAULT)

                            textField(
                                    { value },
                                    { value = it })
                                    .withValidationOnInput { validationForIpAddress(it) }
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_HOST, value) }
                                    .growPolicy(GrowPolicy.SHORT_TEXT)
                                    .component.apply { blobHostLabel.labelFor = this }
                        }

                        val blobPortLabel = JLabel(RiderAzureBundle.message("settings.azurite.row.blob.port"))
                        blobPortLabel()

                        cell {
                            var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_PORT_DEFAULT)

                            textField(
                                    { value },
                                    { value = it })
                                    .withValidationOnInput { validationForPort(it) }
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_PORT, value) }
                                    .growPolicy(GrowPolicy.SHORT_TEXT)
                                    .component.apply { blobPortLabel.labelFor = this }
                        }
                    }

                    row {
                        val queueHostLabel = JLabel(RiderAzureBundle.message("settings.azurite.row.queue.host"))
                        queueHostLabel()

                        cell {
                            var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_HOST_DEFAULT)

                            textField(
                                    { value },
                                    { value = it })
                                    .withValidationOnInput { validationForIpAddress(it) }
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_HOST, value) }
                                    .growPolicy(GrowPolicy.SHORT_TEXT)
                                    .component.apply { queueHostLabel.labelFor = this }
                        }

                        val queuePortLabel = JLabel(RiderAzureBundle.message("settings.azurite.row.queue.port"))
                        queuePortLabel()

                        cell {
                            var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_PORT_DEFAULT)

                            textField(
                                    { value },
                                    { value = it })
                                    .withValidationOnInput { validationForPort(it) }
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_PORT, value) }
                                    .growPolicy(GrowPolicy.SHORT_TEXT)
                                    .component.apply { queuePortLabel.labelFor = this }
                        }
                    }

                    row {
                        val tableHostLabel = JLabel(RiderAzureBundle.message("settings.azurite.row.table.host"))
                        tableHostLabel()

                        cell {
                            var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_TABLE_HOST_DEFAULT)

                            textField(
                                    { value },
                                    { value = it })
                                    .withValidationOnInput { validationForIpAddress(it) }
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_HOST, value) }
                                    .growPolicy(GrowPolicy.SHORT_TEXT)
                                    .component.apply { tableHostLabel.labelFor = this }
                        }

                        val tablePortLabel = JLabel(RiderAzureBundle.message("settings.azurite.row.table.port"))
                        tablePortLabel()

                        cell {
                            var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_TABLE_PORT_DEFAULT)
                            textField(
                                    { value },
                                    { value = it })
                                    .withValidationOnInput { validationForPort(it) }
                                    .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_PORT, value) }
                                    .growPolicy(GrowPolicy.SHORT_TEXT)
                                    .component.apply { tablePortLabel.labelFor = this }
                        }
                    }
                }

                // Certificate settings
                titledRow(RiderAzureBundle.message("settings.azurite.row.certificate")) {
                    row(RiderAzureBundle.message("settings.azurite.row.certificate.path")) {
                        var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PATH)?.let { FileUtil.toSystemDependentName(it) } ?: ""

                        textFieldWithBrowseButton(
                                { value },
                                { value = it },
                                IdeBundle.message("dialog.title.select.0", "*.pem"),
                                null,
                                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
                                .comment(RiderAzureBundle.message("settings.azurite.row.certificate.path.comment"))
                                .withValidationOnInput { validationForPath(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PATH, FileUtil.toSystemIndependentName(value)) }
                    }

                    row(RiderAzureBundle.message("settings.azurite.row.certificate.keypath")) {
                        var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_KEY_PATH)?.let { FileUtil.toSystemDependentName(it) } ?: ""

                        textFieldWithBrowseButton(
                                { value },
                                { value = it },
                                IdeBundle.message("dialog.title.select.0", "*.key"),
                                null,
                                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
                                .comment(RiderAzureBundle.message("settings.azurite.row.certificate.keypath.comment"))
                                .withValidationOnInput { validationForPath(it) }
                                .onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_KEY_PATH, FileUtil.toSystemIndependentName(value)) }
                    }

                    val certPasswordField = JPasswordField()
                    row(JLabel(RiderAzureBundle.message("settings.azurite.row.certificate.password")).apply { labelFor = certPasswordField }) {
                        var value = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PASSWORD)

                        certPasswordField(growPolicy = GrowPolicy.SHORT_TEXT, comment = RiderAzureBundle.message("settings.azurite.row.certificate.password.comment")).withBinding(
                                { String(it.password) },
                                { _, _ -> { } },
                                PropertyBinding(
                                        { value },
                                        { value = it })
                        ).onApply { properties.setValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PASSWORD, value) }
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

    override fun doOKAction() {
        panel.apply()
        Disposer.dispose(disposable)
    }

    override fun dispose() {
        Disposer.dispose(disposable)
    }
}