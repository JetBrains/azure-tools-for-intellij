/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.settings

import com.azure.core.management.AzureEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.AzureCloud
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils
import javax.swing.JList
import kotlin.io.path.Path
import kotlin.io.path.exists

class AzureConfigurable : BoundConfigurable("Azure") {
    private val config = Azure.az().config()

    private lateinit var environmentComboBox: Cell<ComboBox<AzureEnvironment>>

    override fun createPanel() = panel {
        group("General") {
            row("Azure Environment:") {
                val environments = Azure.az(AzureCloud::class.java).list() ?: emptyList()
                environmentComboBox = comboBox(environments, object : SimpleListCellRenderer<AzureEnvironment>() {
                    override fun customize(
                        list: JList<out AzureEnvironment?>,
                        value: AzureEnvironment?,
                        index: Int,
                        selected: Boolean,
                        hasFocus: Boolean
                    ) {
                        if (value != null) {
                            val name = AzureEnvironmentUtils.getCloudName(value).removeSuffix("Cloud")
                            val adEndpoint = value.activeDirectoryEndpoint
                            text = "$name - $adEndpoint"
                        } else {
                            text = "None"
                        }
                    }
                })
                    .align(Align.FILL)
                    .bindItem({
                        AzureEnvironmentUtils.stringToAzureEnvironment(config.cloud) ?: AzureEnvironment.AZURE
                    }, {
                        config.cloud = AzureEnvironmentUtils.azureEnvironmentToString(it ?: AzureEnvironment.AZURE)
                    })
            }
            row("Azure CLI path:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Browse For Azure CLI")
                )
                    .align(Align.FILL)
                    .validationOnInput { validateAzureCliPath(it) }
                    .bindText({
                        config.azureCliPath ?: ""
                    }, {
                        config.azureCliPath = it
                    })
            }
        }
        group("Other") {
            row {
                checkBox("Enable authentication cache")
                    .bindSelected({
                        config.isAuthPersistenceEnabled
                    }, {
                        config.isAuthPersistenceEnabled = it
                    })
            }
        }
    }

    private fun validateAzureCliPath(textField: TextFieldWithBrowseButton): ValidationInfo? {
        val textPath = textField.text
        if (textPath.isEmpty()) {
            return null
        }
        val path = Path(textPath)
        if (!path.exists()) {
            return ValidationInfo("Target file does not exist", textField)
        }

        return null
    }

    override fun apply() {
        var cloudIsChanged = false
        val currentEnv = Azure.az(AzureCloud::class.java).orDefault
        val selectedEnv = environmentComboBox.component.selectedItem as? AzureEnvironment
        if (currentEnv != selectedEnv) {
            cloudIsChanged = true
        }

        if (IdeAzureAccount.getInstance().isLoggedIn && cloudIsChanged) {
            Azure.az(AzureAccount::class.java).logout()
        }

        super.apply()

        if (cloudIsChanged) {
            Azure.az(AzureCloud::class.java).setByName(config.cloud)
        }
        AzureConfigInitializer.saveAzConfig()
    }
}