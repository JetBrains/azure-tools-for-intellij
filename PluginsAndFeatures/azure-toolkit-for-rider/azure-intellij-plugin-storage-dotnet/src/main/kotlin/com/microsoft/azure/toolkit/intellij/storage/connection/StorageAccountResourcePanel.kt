/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection

import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.connector.Resource
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount
import com.microsoft.azure.toolkit.lib.storage.ConnectionStringStorageAccount
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount
import org.apache.commons.codec.digest.DigestUtils
import java.awt.event.ItemEvent
import javax.swing.JPanel

class StorageAccountResourcePanel : AzureFormJPanel<Resource<IStorageAccount>> {
    private val subscriptionComboBox = SubscriptionComboBox()
    private val accountComboBox: AzureComboBox<IStorageAccount>
    private val connectionStringText = AzurePasswordFieldInput(JBPasswordField())
    private lateinit var azureRadioButton: Cell<JBRadioButton>
    private lateinit var localRadioButton: Cell<JBRadioButton>
    private lateinit var connectionStringAzureRadioButton: Cell<JBRadioButton>

    init {
        val loader = {
            val subscriptionId = subscriptionComboBox.value?.id
            if (subscriptionId != null) Azure.az(AzureStorageAccount::class.java).accounts(subscriptionId).list()
            else emptyList()
        }
        accountComboBox = object : AzureComboBox<IStorageAccount>(loader) {
            override fun doGetDefaultValue(): IStorageAccount? {
                return CacheManager.getUsageHistory(IStorageAccount::class.java).peek {
                    val subscription = subscriptionComboBox.value
                    subscription == it.subscription
                }
            }

            override fun getItemText(item: Any?) = (item as? IStorageAccount)?.name ?: ""

            override fun refreshItems() {
                val subscriptionId = subscriptionComboBox.value?.id
                if (subscriptionId != null) {
                    Azure.az(AzureStorageAccount::class.java).accounts(subscriptionId).refresh()
                }
                super.refreshItems()
            }
        }

        subscriptionComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                accountComboBox.reloadItems()
            } else if (it.stateChange == ItemEvent.DESELECTED) {
                accountComboBox.clear()
            }
        }
    }

    private val panel: JPanel = panel {
        buttonsGroup("Method:") {
            row {
                azureRadioButton = radioButton("Select resource under signed-in account")
            }
            row {
                localRadioButton = radioButton("Use connection string")
            }
            row {
                connectionStringAzureRadioButton = radioButton("Connect local Azurite emulator")
            }
        }
        row("Subscription:") {
            cell(subscriptionComboBox)
        }
            .visibleIf(azureRadioButton.selected)
        row("Account:") {
            cell(accountComboBox)
        }
            .visibleIf(azureRadioButton.selected)
        row("Connection String:") {
            cell(connectionStringText)
        }
            .visibleIf(connectionStringAzureRadioButton.selected)
    }

    override fun setValue(data: Resource<IStorageAccount>?) {
        val account = data?.data
        if (account != null) {
            when (account) {
                is AzuriteStorageAccount -> {
                    localRadioButton.selected(true)
                }

                is ConnectionStringStorageAccount -> {
                    connectionStringText.value = account.connectionString
                    connectionStringAzureRadioButton.selected(true)
                }

                else -> {
                    subscriptionComboBox.value = account.subscription
                    accountComboBox.value = account
                    azureRadioButton.selected(true)
                }
            }
        }
    }

    override fun getValue(): Resource<IStorageAccount>? {
        val info = getValidationInfo(true)
        if (!info.isValid) return null

        val connectionString = connectionStringText.value
        val account =
            if (azureRadioButton.component.isSelected) accountComboBox.value
            else if (localRadioButton.component.isSelected) AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT
            else Azure.az(AzureStorageAccount::class.java).getOrInitByConnectionString(connectionString)
        val predefinedId = if (connectionString.isNotBlank()) DigestUtils.md5Hex(connectionString) else null
        if (account is ConnectionStringStorageAccount && !predefinedId.isNullOrBlank() && connectionString.isNotBlank()) {
            IntelliJSecureStore
                .getInstance()
                .savePassword(StorageAccountResourceDefinition::class.java.name, predefinedId, null, connectionString)
        }

        return StorageAccountResourceDefinition.INSTANCE.define(account, predefinedId)
    }

    override fun getContentPanel() = panel

    override fun getInputs() = listOf<AzureFormInput<*>>(subscriptionComboBox, accountComboBox, connectionStringText)
}