/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("InvalidBundleOrProperty")

package com.microsoft.azure.toolkit.intellij.storage.storage

import com.intellij.icons.AllIcons
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.action.Action
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.jvm.java
import kotlin.let

class StorageAccountComboBox : AzureComboBox<StorageAccountConfig>() {
    private var subscriptionId: String? = null
    private var resourceGroupName: String? = null
    private val drafts = mutableListOf<StorageAccountConfig>()

    fun setSubscription(newSubscription: String?) {
        if (newSubscription == subscriptionId) return

        subscriptionId = newSubscription
        reloadItems()
    }

    fun setResourceGroup(newResourceGroup: String?) {
        if (newResourceGroup == resourceGroupName) return

        resourceGroupName = newResourceGroup
        reloadItems()
    }

    override fun setValue(value: StorageAccountConfig?, fixed: Boolean?) {
        if (value != null && isDraftResource(value)) {
            drafts.remove(value)
            drafts.add(0, value)
            reloadItems()
        }

        super.setValue(value, fixed)
    }

    override fun getItemText(item: Any?): String {
        if (item == null || item !is StorageAccountConfig) return EMPTY_ITEM

        val name = item.name
        return if (isDraftResource(item)) "(New) $name" else name
    }

    override fun loadItems(): List<StorageAccountConfig> {
        val sid = subscriptionId ?: return emptyList()

        val result = mutableListOf<StorageAccountConfig>()

        if (drafts.isNotEmpty()) {
            drafts
                .filter { it.subscriptionId == sid }
                .sortedBy { it.name }
                .forEach { result.add(it) }
        }

        val remoteAccounts = Azure.az(AzureStorageAccount::class.java)
            .accounts(sid)
            .list()
            .filter { it.resourceGroupName.equals(resourceGroupName, true) }
            .sortedBy { it.name }
            .map { StorageAccountConfig(it.subscriptionId, it.name) }

        result.addAll(remoteAccounts)

        return result
    }

    override fun refreshItems() {
        subscriptionId?.let {
            Azure.az(AzureStorageAccount::class.java).accounts(it).refresh()
        }
        super.refreshItems()
    }

    override fun getExtensions(): List<Extension?> {
        val extensions = super.getExtensions() as MutableList<Extension?>

        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK)
        val tooltip = "Create new storage account ${KeymapUtil.getKeystrokeText(keyStroke)}"
        val addEx = Extension.create(AllIcons.General.Add, tooltip, ::showStorageAccountCreationPopup)
        registerShortcut(keyStroke, addEx)
        extensions.add(addEx)

        return extensions
    }

    private fun showStorageAccountCreationPopup() {
        val sid = subscriptionId ?: return
        val storageAccountName = (selectedItem as? StorageAccountConfig)?.name
        val dialog = StorageAccountCreationDialog(storageAccountName, sid)
        val actionId = Action.Id.of<StorageAccountConfig>("user/storage.create_account.group")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(StorageAccountConfig::name)
                .withSource { it }
                .withAuthRequired(false)
                .withHandler { setValue(it) }
        )
        dialog.show()
    }

    private fun isDraftResource(value: StorageAccountConfig) =
        Azure.az(AzureStorageAccount::class.java)
            .accounts(value.subscriptionId)
            .exists(value.name, resourceGroupName)
            .not()
}
