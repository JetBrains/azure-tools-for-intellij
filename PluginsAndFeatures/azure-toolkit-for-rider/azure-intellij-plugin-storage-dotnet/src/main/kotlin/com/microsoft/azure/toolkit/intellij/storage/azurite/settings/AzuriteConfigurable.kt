/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.storage.azurite.settings

import com.intellij.execution.services.ServiceEventListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selectedValueIs
import com.intellij.util.application
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer
import com.microsoft.azure.toolkit.intellij.storage.azurite.services.AzuriteServiceViewContributor
import com.microsoft.azure.toolkit.lib.Azure
import java.io.File
import kotlin.io.path.absolutePathString

class AzuriteConfigurable(private val project: Project) : BoundConfigurable("Azurite") {
    companion object {
        private const val IP_ADDRESS_PATTERN = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\$"
    }

    private val ipAddressRegex = Regex(IP_ADDRESS_PATTERN)

    private val settings get() = AzuriteSettings.getInstance(project)

    private lateinit var workspaceComboBox: Cell<ComboBox<AzuriteLocationMode>>
    private lateinit var basicOAuthCheckBox: Cell<JBCheckBox>
    private lateinit var showAzuriteServiceCheckBox: Cell<JBCheckBox>

    override fun createPanel() = panel {
        group("Azurite Executable") {
            row("Azurite executable path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor().withTitle("Browse For Azurite Executable")
                )
                    .align(Align.FILL)
                    .bindText(settings::executablePath)
                    .validationOnInput { validationForPath(it) }
            }
        }
        group("General Settings") {
            row("Azurite workspace:") {
                workspaceComboBox =
                    comboBox(AzuriteLocationMode.entries, SimpleListCellRenderer.create("") { it.description })
                        .bindItem(settings::locationMode.toNullableProperty())
            }
            row("Path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Browse For Workspace Location"))
                    .align(Align.FILL)
                    .bindText(settings::workspacePath)
                    .validationOnInput { validationForPath(it) }
            }
                .visibleIf(workspaceComboBox.component.selectedValueIs(AzuriteLocationMode.Custom))
            row {
                checkBox("Enable loose mode (ignores unsupported headers and parameters)")
                    .bindSelected(settings::looseMode)
            }
            row {
                basicOAuthCheckBox = checkBox("Enable basic OAuth authentication for Azurite")
                    .comment("OAuth requires an HTTPS endpoint.")
                    .bindSelected(settings::basicOAuth)
            }
            row {
                showAzuriteServiceCheckBox = checkBox("Show Azurite in the Services tool window")
                    .bindSelected(settings::showAzuriteService)
                    .onApply {
                        application.messageBus
                            .syncPublisher(ServiceEventListener.TOPIC)
                            .handle(
                                ServiceEventListener.ServiceEvent.createResetEvent(
                                    AzuriteServiceViewContributor::class.java
                                )
                            )
                    }
            }
            row {
                checkBox("Check the Azurite executable before running a configuration")
                    .bindSelected(settings::checkAzuriteExecutable)
            }
        }
        group("Host/Port Settings") {
            row {
                label("Blob host:")
                textField()
                    .bindText(settings::blobHost)
                    .validationOnInput { validationForIpAddress(it) }
                label("Port:")
                intTextField(1024..65535)
                    .bindIntText(settings::blobPort)
            }.layout(RowLayout.PARENT_GRID)
            row {
                label("Queue host:")
                textField()
                    .bindText(settings::queueHost)
                    .validationOnInput { validationForIpAddress(it) }
                label("Port:")
                intTextField(1024..65535)
                    .bindIntText(settings::queuePort)
            }.layout(RowLayout.PARENT_GRID)
            row {
                label("Table host:")
                textField()
                    .bindText(settings::tableHost)
                    .validationOnInput { validationForIpAddress(it) }
                label("Port:")
                intTextField(1024..65535)
                    .bindIntText(settings::tablePort)
            }.layout(RowLayout.PARENT_GRID)
        }
        group("Certificate Settings") {
            row("Certificate path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select *.pem"))
                    .align(Align.FILL)
                    .comment("Path to a locally-trusted pem or pfx certificate file path to enable HTTPS mode.")
                    .bindText(settings::certificatePath)
                    .validationOnInput { validationForPath(it) }
                    .validationOnInput { validationForOAuth(it) }
            }
            row("Certificate key path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select *.key")
                )
                    .align(Align.FILL)
                    .comment("Path to a locally-trusted pem key file, required when cert points to a pem file.")
                    .bindText(settings::certificateKeyPath)
                    .validationOnInput { validationForPath(it) }
            }
            row("Certificate password:") {
                passwordField()
                    .align(Align.FILL)
                    .comment("Password for pfx file, required when cert points to a pfx file.")
                    .bindText(settings::certificatePassword)
            }
        }
    }

    private fun validationForPath(textField: TextFieldWithBrowseButton) =
        if (textField.text.isNotEmpty() && !File(textField.text).exists()) {
            ValidationInfo("Not a valid path.", textField)
        } else {
            null
        }

    private fun validationForOAuth(textField: TextFieldWithBrowseButton) =
        if (basicOAuthCheckBox.component.isSelected && textField.text.isEmpty()) {
            ValidationInfo("The path cannot be empty if OAuth is enabled.", textField)
        } else {
            null
        }

    private fun validationForIpAddress(textField: JBTextField) =
        if (textField.text.isNullOrEmpty() || !ipAddressRegex.matches(textField.text)) {
            ValidationInfo("Not a valid IP address.", textField)
        } else {
            null
        }

    override fun apply() {
        super.apply()

        val config = Azure.az().config()

        val settingsAzuriteExecutable = settings.getAzuriteExecutablePath()?.absolutePathString() ?: ""
        if (settingsAzuriteExecutable != config.azuritePath) {
            config.azuritePath = settingsAzuriteExecutable
        }

        val settingsAzuriteWorkspace = settings.getAzuriteWorkspacePath().absolutePathString()
        if (settingsAzuriteWorkspace != config.azuriteWorkspace) {
            config.azuriteWorkspace = settingsAzuriteWorkspace
        }

        AzureConfigInitializer.saveAzConfig()
    }
}