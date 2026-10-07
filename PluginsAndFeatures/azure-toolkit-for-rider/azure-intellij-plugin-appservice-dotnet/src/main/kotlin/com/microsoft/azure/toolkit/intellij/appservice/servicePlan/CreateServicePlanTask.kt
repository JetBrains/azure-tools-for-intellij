/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.servicePlan

import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.common.task.AzureTask

class CreateServicePlanTask(private val config: AppServicePlanConfig) : AzureTask<AppServicePlan>() {
    override fun doExecute(): AppServicePlan {
        val plan = Azure.az(AzureAppService::class.java)
            .plans(config.subscriptionId)
            .getOrDraft(config.name, config.resourceGroupName)

        if (plan.isDraftForCreating) {
            val draft = (plan as? AppServicePlanDraft)?.apply {
                operatingSystem = config.os
                region = config.region
                pricingTier = config.pricingTier
            } ?: error("Unable to get app service plan draft")

            draft.createIfNotExist()
        }

        return plan
    }
}