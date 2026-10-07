/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class FunctionAppContainerConfigurationOptions : LocatableRunConfigurationOptions() {
    var functionAppName by string()
    var subscriptionId by string()
    var resourceGroupName by string()
    var region by string()
    var appServicePlanName by string()
    var appServicePlanResourceGroupName by string()
    var pricingTier by string()
    var pricingSize by string()
    var imageRepository by string()
    var imageTag by string()
    var storageAccountName by string()
    var storageAccountResourceGroup by string()
}