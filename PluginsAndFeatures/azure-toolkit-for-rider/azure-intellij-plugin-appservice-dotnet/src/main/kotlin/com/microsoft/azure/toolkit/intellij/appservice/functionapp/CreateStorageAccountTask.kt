/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import com.microsoft.azure.toolkit.lib.storage.StorageAccount
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft
import com.microsoft.azure.toolkit.lib.storage.model.Kind
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy

class CreateStorageAccountTask(
    private val subscriptionId: String,
    private val resourceGroupName: String,
    private val storageAccountName: String,
    private val storageAccountRegion: Region
) : AzureTask<StorageAccount>() {
    override fun doExecute(): StorageAccount {
        val account = Azure.az(AzureStorageAccount::class.java)
            .accounts(subscriptionId)
            .getOrDraft(storageAccountName, resourceGroupName)

        if (account.isDraftForCreating) {
            val draft = (account as? StorageAccountDraft)?.apply {
                setRegion(storageAccountRegion)
                setKind(Kind.STORAGE_V2)
                setRedundancy(Redundancy.STANDARD_LRS)
            } ?: error("Unable to get storage account draft")

            draft.createIfNotExist()
        }

        return account
    }
}