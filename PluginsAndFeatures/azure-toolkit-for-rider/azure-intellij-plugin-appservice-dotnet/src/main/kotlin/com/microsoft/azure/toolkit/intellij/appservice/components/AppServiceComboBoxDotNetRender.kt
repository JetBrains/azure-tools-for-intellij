/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.components

import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.SimpleListCellRenderer
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceModule
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import javax.swing.JList

class AppServiceComboBoxDotNetRender : SimpleListCellRenderer<AppServiceConfig>() {
    companion object {
        private val LOG = logger<AppServiceComboBoxDotNetRender>()
    }

    override fun customize(
        list: JList<out AppServiceConfig>,
        config: AppServiceConfig?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ) {
        try {
            if (config == null) return

            text = if (index >= 0) {
                getAppServiceLabel(config)
            } else {
                config.appName
            }
            accessibleContext?.accessibleDescription = config.appName
        } catch (e: Exception) {
            LOG.error("Error while customizing label", e)
            throw e
        }
    }

    private fun getAppServiceLabel(config: AppServiceConfig): String {
        val module = getModule(config)
        val isDraft = module?.exists(config.appName, config.resourceGroup) == false
        val appName = config.appName
        val resourceGroup = config.resourceGroup
        val app = module?.get(config.appName, config.resourceGroup)
        val linuxFxVersion = app?.linuxFxVersion
        val os = config.runtime?.os ?: "unknown"

        return buildString {
            append("<html><div>")
            if (isDraft) {
                append("(New) ")
            }
            append(appName)
            append("</div>")
            append("<small>")
            append("OS: ")
            append(os)
            if (!linuxFxVersion.isNullOrEmpty()) {
                append(" | Runtime: ")
                append(linuxFxVersion)
            }
            if (!resourceGroup.isNullOrEmpty()) {
                append(" | Resource Group: ")
                append(resourceGroup)
            }
            append("</small></html>")
        }
    }

    private fun getModule(config: AppServiceConfig): AppServiceResourceModule<out AppServiceAppBase<*, AppServiceServiceSubscription?, *>?, AppServiceServiceSubscription?, *>? {
        if (config.subscriptionId.isNullOrEmpty() || !Azure.az(AzureAccount::class.java).isLoggedIn) {
            return null
        }

        return if (config is FunctionAppConfig) Azure.az(AzureFunctions::class.java).functionApps(config.subscriptionId)
        else Azure.az(AzureWebApp::class.java).webApps(config.subscriptionId)
    }
}