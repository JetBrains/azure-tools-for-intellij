package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.resource.AzureResources
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel

fun AzureWebAppMvpModel.createWebAppFromDotNetSettingModel(model: DotNetWebAppSettingModel): WebApp {
    Azure.az(AzureResources::class.java).groups(model.subscriptionId).createResourceGroupIfNotExist(model.resourceGroup, Region.fromName(model.region))
    val servicePlanConfig = AppServicePlanConfig.builder()
            .subscriptionId(model.subscriptionId)
            .name(model.appServicePlanName)
            .resourceGroupName(model.resourceGroup)
            .region(Region.fromName(model.region))
            .os(OperatingSystem.fromString(model.operatingSystem))
            .pricingTier(PricingTier.fromString(model.pricing))
            .build()
    val appServicePlan = getOrCreateAppServicePlan(servicePlanConfig)
    throw NotImplementedError()
}

private fun getOrCreateAppServicePlan(servicePlanConfig: AppServicePlanConfig): AppServicePlan {
    val rg = servicePlanConfig.resourceGroupName
    val name = servicePlanConfig.name
    val appService = Azure.az(AzureAppService::class.java)
    val appServicePlan = appService.plans(servicePlanConfig.subscriptionId).getOrDraft(name, rg)
    return if (appServicePlan.exists()) {
        appServicePlan
    } else {
        val draft = appServicePlan as AppServicePlanDraft
        draft.region = servicePlanConfig.region
        draft.pricingTier = servicePlanConfig.pricingTier
        draft.operatingSystem = servicePlanConfig.os
        draft.createIfNotExist()
    }
}
