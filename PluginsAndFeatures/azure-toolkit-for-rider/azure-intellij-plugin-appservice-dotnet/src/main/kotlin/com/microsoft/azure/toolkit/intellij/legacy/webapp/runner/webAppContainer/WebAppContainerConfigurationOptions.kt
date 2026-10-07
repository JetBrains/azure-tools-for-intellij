/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class WebAppContainerConfigurationOptions : LocatableRunConfigurationOptions() {
    var webAppName by string()
    var subscriptionId by string()
    var resourceGroupName by string()
    var region by string()
    var appServicePlanName by string()
    var appServicePlanResourceGroupName by string()
    var pricingTier by string()
    var pricingSize by string()
    var imageRepository by string()
    var imageTag by string()
    var port by property(80)
}