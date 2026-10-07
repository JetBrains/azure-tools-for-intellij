/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerDraft

class CreateDeploymentContainerTask(
    private val subscriptionId: String,
    private val resourceGroupName: String,
    private val storageAccountName: String,
    private val deploymentContainer: String,
) : AzureTask<String>() {
    override fun doExecute(): String {
        try {
            val account = Azure.az(AzureStorageAccount::class.java)
                .accounts(subscriptionId)
                .get(storageAccountName, resourceGroupName)
            requireNotNull(account)

            val container = account.blobContainerModule.getOrDraft(deploymentContainer, resourceGroupName)

            if (container.isDraftForCreating) {
                val draft = (container as? BlobContainerDraft) ?: error("Unable to get blob container draft")

                draft.createIfNotExist()
            }

            return container.url
        } catch (_: Exception) {
            AzureMessager
                .getMessager()
                .warning("Failed to get/create deployment container $deploymentContainer for function app, please make sure the container has been created successfully.")
            return "https://$storageAccountName.blob.core.windows.net/$deploymentContainer"
        }
    }
}