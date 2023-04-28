package com.microsoft.intellij.serviceexplorer.azure

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_IN
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_OUT
import com.microsoft.azure.toolkit.intellij.common.auth.AzureSignInAction
import com.microsoft.azuretools.authmanage.IdeAzureAccount
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule

class SignInOutAction(azureModule: AzureModule) : NodeAction(azureModule, "Sign In/Out") {
    init {
        addListener(object : NodeActionListener() {
            override fun actionPerformed(e: NodeActionEvent) {
                AzureSignInAction.authActionPerformed(azureModule.project as? Project)
            }

            override fun afterActionPerformed(e: NodeActionEvent) {
                azureModule.load(false)
            }
        })
    }

    override fun getName(): String {
        return try {
            if (IdeAzureAccount.getInstance().isLoggedIn) "Sign Out" else "Sign In"
        } catch (e: Exception) {
            ""
        }
    }

    override fun getIconSymbol(): AzureIcon {
        return try {
            if (IdeAzureAccount.getInstance().isLoggedIn) SIGN_OUT else SIGN_IN
        } catch (e: Exception) {
            return SIGN_IN
        }
    }
}