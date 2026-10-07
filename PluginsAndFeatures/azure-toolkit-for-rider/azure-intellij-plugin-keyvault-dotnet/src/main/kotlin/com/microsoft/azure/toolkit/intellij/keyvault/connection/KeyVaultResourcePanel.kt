/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection

import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.connector.Resource
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault
import java.awt.event.ItemEvent
import javax.swing.JPanel

class KeyVaultResourcePanel : AzureFormJPanel<Resource<KeyVault>> {
    private val subscriptionComboBox = SubscriptionComboBox()
    private val vaultComboBox: AzureComboBox<KeyVault>

    init {
        val loader = {
            val subscriptionId = subscriptionComboBox.value?.id
            if (subscriptionId != null) Azure.az(AzureKeyVault::class.java).keyVaults(subscriptionId).list()
            else emptyList()
        }
        vaultComboBox = object : AzureComboBox<KeyVault>(loader) {
            override fun doGetDefaultValue(): KeyVault? {
                return CacheManager.getUsageHistory(KeyVault::class.java).peek {
                    val subscription = subscriptionComboBox.value
                    subscription == it.subscription
                }
            }

            override fun getItemText(item: Any?) = (item as? KeyVault)?.name ?: ""

            override fun refreshItems() {
                val subscriptionId = subscriptionComboBox.value?.id
                if (subscriptionId != null) {
                    Azure.az(AzureKeyVault::class.java).keyVaults(subscriptionId).refresh()
                }
                super.refreshItems()
            }
        }
        vaultComboBox.isRequired = true

        subscriptionComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                vaultComboBox.reloadItems()
            } else if (it.stateChange == ItemEvent.DESELECTED) {
                vaultComboBox.clear()
            }
        }
    }

    private val panel: JPanel = panel {
        row("Subscription:") {
            cell(subscriptionComboBox)
        }
        row("Key Vault:") {
            cell(vaultComboBox)
        }
    }

    override fun setValue(data: Resource<KeyVault>?) {
        val keyVault = data?.data
        if (keyVault != null) {
            subscriptionComboBox.value = keyVault.subscription
            vaultComboBox.value = keyVault
        }
    }

    override fun getValue(): Resource<KeyVault>? {
        val info = getValidationInfo(true)
        if (!info.isValid) return null

        val cache = vaultComboBox.value
        return KeyVaultResourceDefinition.INSTANCE.define(cache)
    }

    override fun getContentPanel() = panel

    override fun getInputs() = listOf(subscriptionComboBox, vaultComboBox)
}