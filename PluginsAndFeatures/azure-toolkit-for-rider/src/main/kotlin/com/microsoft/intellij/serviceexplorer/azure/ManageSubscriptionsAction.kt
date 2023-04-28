package com.microsoft.intellij.serviceexplorer.azure

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.subscription.SelectSubscriptionsAction
import com.microsoft.azuretools.authmanage.IdeAzureAccount
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule

class ManageSubscriptionsAction(azureModule: AzureModule) : NodeAction(azureModule, "Select Subscriptions") {
    init {
        addListener(object : NodeActionListener() {
            override fun actionPerformed(e: NodeActionEvent) {
                SelectSubscriptionsAction.selectSubscriptions(azureModule.project as? Project)
            }
        })
    }

    override fun getIconSymbol(): AzureIcon = AzureIcons.Common.SELECT_SUBSCRIPTIONS

    override fun isEnabled(): Boolean {
        return try {
            super.isEnabled() && IdeAzureAccount.getInstance().isLoggedIn
        } catch (e: Exception) {
            false
        }
    }
}