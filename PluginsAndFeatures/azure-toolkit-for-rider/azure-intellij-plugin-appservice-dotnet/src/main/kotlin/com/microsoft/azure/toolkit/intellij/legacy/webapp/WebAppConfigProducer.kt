/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.utils.Utils
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup

@Service
class WebAppConfigProducer {
    companion object {
        fun getInstance() = service<WebAppConfigProducer>()
    }

    fun generateDefaultConfig(group: ResourceGroup? = null): AppServiceConfig {
        val subscription = group?.subscription
            ?: Azure.az(AzureAccount::class.java).account().selectedSubscriptions.firstOrNull()
        val appName = Utils.generateRandomResourceName("app", 32)
        val rgName = group?.name ?: "rg-$appName"
        val result = AppServiceConfig.buildDefaultWebAppConfig(rgName, appName, "zip")
        result.appSettings = mutableMapOf()
        result.pricingTier = PricingTier.FREE_F1
        result.runtime = RuntimeConfig().apply { os = OperatingSystem.WINDOWS }
        subscription?.let { result.subscriptionId = it.id }
        group?.let { result.region = it.region }

        return result
    }
}