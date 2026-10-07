/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.storage

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.rider.azureFunctions.azurite.services.AzuriteService
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azure.toolkit.lib.storage.StorageAccount

class IntellijDotnetStorageActionsContributor : IActionsContributor {
    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, _ -> r is StorageAccount }) { r, e: AnActionEvent ->
            AzureTaskManager.getInstance().runLater {
                val dialog = ConnectorDialog(e.project)
                dialog.setResource(AzureServiceResource(r as StorageAccount, StorageAccountResourceDefinition.INSTANCE))
                dialog.show()
            }
        }

        am.registerHandler(
            StorageActionsContributor.START_AZURITE
        ) { _, e: AnActionEvent ->
            val project = e.project ?: return@registerHandler
            AzuriteService.getInstance().start(project)
        }

        am.registerHandler(
            StorageActionsContributor.STOP_AZURITE
        ) { _, _: AnActionEvent ->
            AzuriteService.getInstance().stop()
        }
    }
}