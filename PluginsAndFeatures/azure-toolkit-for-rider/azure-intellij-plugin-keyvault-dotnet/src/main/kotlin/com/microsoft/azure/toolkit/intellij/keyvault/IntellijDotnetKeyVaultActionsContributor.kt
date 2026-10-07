/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.keyvault

import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog
import com.microsoft.azure.toolkit.intellij.keyvault.connection.KeyVaultResourceDefinition
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault

class IntellijDotnetKeyVaultActionsContributor : IActionsContributor {
    override fun registerHandlers(am: AzureActionManager) {
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, _ -> r is KeyVault }) { r, e: AnActionEvent ->
            AzureTaskManager.getInstance().runLater {
                val dialog = ConnectorDialog(e.project)
                dialog.setResource(AzureServiceResource(r as KeyVault, KeyVaultResourceDefinition.INSTANCE))
                dialog.show()
            }
        }
    }
}