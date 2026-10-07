/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.storage

import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.AzureDialog
import com.microsoft.azure.toolkit.lib.common.form.AzureForm

class StorageAccountCreationDialog(
    storageAccountName: String?,
    private var subscriptionId: String
) : AzureDialog<StorageAccountConfig>(), AzureForm<StorageAccountConfig> {

    private val accountNameTextField = AccountNameTextField(subscriptionId).apply {
        text = storageAccountName
    }

    private val mainPanel = panel {
        row("Name:") {
            cell(accountNameTextField)
                .align(Align.FILL)
        }
    }

    init {
        super.init()
        super.pack()
    }

    override fun getForm(): AzureForm<StorageAccountConfig> = this

    override fun getDialogTitle() = "New storage account"

    override fun createCenterPanel() = mainPanel

    override fun getValue(): StorageAccountConfig {
        val name = accountNameTextField.value
        return StorageAccountConfig(subscriptionId, name)
    }

    override fun setValue(data: StorageAccountConfig) {
        subscriptionId = data.subscriptionId
        accountNameTextField.value = data.name
    }

    override fun getInputs() = listOf(accountNameTextField)
}