/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class WebAppConfigurationOptions : LocatableRunConfigurationOptions() {
    var webAppName by string()
    var subscriptionId by string()
    var resourceGroupName by string()
    var region by string()
    var appServicePlanName by string()
    var appServicePlanResourceGroupName by string()
    var pricingTier by string()
    var pricingSize by string()
    var operatingSystem by string()
    var isDeployToSlot by property(false)
    var slotName by string()
    var slotConfigurationSource by string()
    var projectConfiguration by string()
    var projectPlatform by string()
    var publishableProjectPath by string()
    var openBrowser by property(false)
}