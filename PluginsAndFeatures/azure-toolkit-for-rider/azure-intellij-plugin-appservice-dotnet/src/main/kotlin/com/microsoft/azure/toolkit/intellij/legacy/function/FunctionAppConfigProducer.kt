/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.utils.Utils
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup

@Service
class FunctionAppConfigProducer {
    companion object {
        fun getInstance() = service<FunctionAppConfigProducer>()
    }

    fun generateDefaultConfig(group: ResourceGroup? = null): FunctionAppConfig {
        val subscription = group?.subscription
            ?: Azure.az(AzureAccount::class.java).account().selectedSubscriptions.firstOrNull()
        val appName = Utils.generateRandomResourceName("app", 32)
        val rgName = group?.name ?: "rg-$appName"
        val result = FunctionAppConfig.buildDefaultFunctionConfig(rgName, appName)
        result.appSettings = mutableMapOf()
        result.runtime = RuntimeConfig().apply { os = OperatingSystem.WINDOWS }
        subscription?.let { result.subscriptionId = it.id }
        group?.let { result.region = it.region }

        return result
    }
}