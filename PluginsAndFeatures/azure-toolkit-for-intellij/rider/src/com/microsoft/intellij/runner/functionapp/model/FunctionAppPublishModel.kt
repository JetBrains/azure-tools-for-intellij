/**
 * Copyright (c) 2019-2023 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functionapp.model

import com.intellij.database.util.common.isNotNullOrEmpty
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.application
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.storage.SkuName
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import org.jdom.Element

class FunctionAppPublishModel {

    companion object {
        val defaultOperatingSystem = OperatingSystem.WINDOWS
        val dynamicPricingTier = PricingTier("Dynamic", "Y1")
        val defaultPricingTier = dynamicPricingTier

        val standardLocalRedundantStorage = StorageAccountSkuType.STANDARD_LRS
        val defaultStorageAccountType = standardLocalRedundantStorage

        val defaultFunctionRuntimeStack = FunctionRuntimeStack(
                "dotnet","~4", "dotnet|6.0", "dotnet|6.0")

        private const val AZURE_FUNCTION_APP_PROJECT                    = "AZURE_FUNCTION_APP_PROJECT"
        private const val AZURE_FUNCTION_APP_CONFIGURATION              = "AZURE_FUNCTION_APP_CONFIGURATION"
        private const val AZURE_FUNCTION_APP_PLATFORM                   = "AZURE_FUNCTION_APP_PLATFORM"
        private const val AZURE_FUNCTION_APP_SUBSCRIPTION_ID            = "AZURE_FUNCTION_APP_SUBSCRIPTION_ID"
        private const val AZURE_FUNCTION_APP_IS_CREATE_APP              = "AZURE_FUNCTION_APP_IS_CREATE_APP"
        private const val AZURE_FUNCTION_APP_ID                         = "AZURE_FUNCTION_APP_ID"
        private const val AZURE_FUNCTION_APP_NAME                       = "AZURE_FUNCTION_APP_NAME"
        private const val AZURE_FUNCTION_APP_IS_CREATE_RESOURCE_GROUP   = "AZURE_FUNCTION_APP_IS_CREATE_RESOURCE_GROUP"
        private const val AZURE_FUNCTION_APP_RESOURCE_GROUP_NAME        = "AZURE_FUNCTION_APP_RESOURCE_GROUP_NAME"
        private const val AZURE_FUNCTION_APP_IS_CREATE_APP_SERVICE_PLAN = "AZURE_FUNCTION_APP_IS_CREATE_APP_SERVICE_PLAN"
        private const val AZURE_FUNCTION_APP_SERVICE_PLAN_ID            = "AZURE_FUNCTION_APP_SERVICE_PLAN_ID"
        private const val AZURE_FUNCTION_APP_SERVICE_PLAN_NAME          = "AZURE_FUNCTION_APP_SERVICE_PLAN_NAME"
        private const val AZURE_FUNCTION_APP_LOCATION                   = "AZURE_FUNCTION_APP_LOCATION"
        private const val AZURE_FUNCTION_APP_PRICING_TIER               = "AZURE_FUNCTION_APP_PRICING_TIER"
        private const val AZURE_FUNCTION_APP_OPERATING_SYSTEM           = "AZURE_FUNCTION_APP_OPERATING_SYSTEM"
        private const val AZURE_FUNCTION_APP_RUNTIME                    = "AZURE_FUNCTION_APP_RUNTIME"
        private const val AZURE_FUNCTION_APP_RUNTIMEVERSION             = "AZURE_FUNCTION_APP_RUNTIMEVERSION"
        private const val AZURE_FUNCTION_APP_FXVERSION                  = "AZURE_FUNCTION_APP_FXVERSION"
        private const val AZURE_FUNCTION_APP_IS_CREATE_STORAGE_ACCOUNT  = "AZURE_FUNCTION_APP_IS_CREATE_STORAGE_ACCOUNT"
        private const val AZURE_FUNCTION_APP_STORAGE_ACCOUNT_ID         = "AZURE_FUNCTION_APP_STORAGE_ACCOUNT_ID"
        private const val AZURE_FUNCTION_APP_STORAGE_ACCOUNT_NAME       = "AZURE_FUNCTION_APP_STORAGE_ACCOUNT_NAME"
        private const val AZURE_FUNCTION_APP_STORAGE_ACCOUNT_TYPE       = "AZURE_FUNCTION_APP_STORAGE_ACCOUNT_TYPE"
        private const val AZURE_FUNCTION_APP_IS_DEPLOY_TO_SLOT          = "AZURE_FUNCTION_APP_IS_DEPLOY_TO_SLOT"
        private const val AZURE_FUNCTION_APP_SLOT_NAME                  = "AZURE_FUNCTION_APP_SLOT_NAME"
        private const val AZURE_FUNCTION_APP_OPEN_BROWSER_PUBLISH       = "AZURE_FUNCTION_APP_OPEN_BROWSER_PUBLISH"
    }

    var publishableProject: PublishableProjectModel? = null
    var configuration = ""
    var platform = ""

    var subscription: Subscription? = null

    var isCreatingNewApp = false
    var appId = ""
    var appName = ""

    var isCreatingResourceGroup = false
    var resourceGroupName = ""

    var isCreatingAppServicePlan = false
    var appServicePlanId: String = ""
    var appServicePlanName = ""
    var operatingSystem = defaultOperatingSystem
    var location = AzureDefaults.location
    var pricingTier = defaultPricingTier

    var functionRuntimeStack = defaultFunctionRuntimeStack

    var isCreatingStorageAccount = false
    var storageAccountId = ""
    var storageAccountName = ""
    var storageAccountType: StorageAccountSkuType = defaultStorageAccountType

    var isDeployToSlot = false
    var slotName = ""

    var openInBrowserAfterPublish = false

    /**
     * Reset the model with values after creating a new instance
     */
    fun resetOnPublish(functionApp: WebAppBase) {
        isCreatingNewApp = false
        if (functionApp is FunctionDeploymentSlot) {
            appId = functionApp.parent().id()
        } else {
            appId = functionApp.id()
        }
        appName = ""

        isCreatingResourceGroup = false
        resourceGroupName = ""

        isCreatingAppServicePlan = false
        appServicePlanName = ""

        isCreatingStorageAccount = false
        storageAccountName = ""
    }

    fun readExternal(project: Project, element: Element) {

        val projectPath = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_PROJECT) ?: ""
        application.invokeLater {
            project.solution.publishableProjectsModel.publishableProjects.advise(project.createLifetime()) {
                if (it.newValueOpt?.projectFilePath == projectPath)
                    publishableProject = it.newValueOpt
            }
        }

        configuration = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_CONFIGURATION) ?: ""
        platform = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_PLATFORM) ?: ""

        val subscriptionId = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_SUBSCRIPTION_ID) ?: ""
        subscription = AzureMvpModel.getInstance().selectedSubscriptions.find { it.subscriptionId() == subscriptionId }

        isCreatingNewApp = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_IS_CREATE_APP) == "1"

        appId = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_ID) ?: ""
        appName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_NAME) ?: ""

        isCreatingResourceGroup = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_IS_CREATE_RESOURCE_GROUP) == "1"
        resourceGroupName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_RESOURCE_GROUP_NAME) ?: ""

        val osString = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_OPERATING_SYSTEM) ?: defaultOperatingSystem.name
        operatingSystem = OperatingSystem.fromString(osString)

        isCreatingAppServicePlan = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_IS_CREATE_APP_SERVICE_PLAN) == "1"
        appServicePlanId = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_SERVICE_PLAN_ID) ?: ""
        appServicePlanName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_SERVICE_PLAN_NAME) ?: ""

        val locationName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_LOCATION) ?: AzureDefaults.location.name()
        location = Region.fromName(locationName)

        val pricingTierName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_PRICING_TIER) ?: defaultPricingTier.toSkuDescription().name()
        val skuDescription = SkuDescription().withName(pricingTierName)
        pricingTier = PricingTier.fromSkuDescription(skuDescription)

        isCreatingStorageAccount = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_IS_CREATE_STORAGE_ACCOUNT) == "1"
        storageAccountId = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_STORAGE_ACCOUNT_ID) ?: ""
        storageAccountName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_STORAGE_ACCOUNT_NAME) ?: ""

        val storageAccountTypeString = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_STORAGE_ACCOUNT_TYPE) ?: storageAccountType.name().toString()
        val skuName = SkuName.fromString(storageAccountTypeString)
        storageAccountType = StorageAccountSkuType.fromSkuName(skuName)

        val runtime = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_RUNTIME) ?: "dotnet"
        val runtimeVersion = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_RUNTIMEVERSION)
        val runtimeFxVersion = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_FXVERSION)
        if (runtimeVersion.isNotNullOrEmpty && runtimeFxVersion.isNotNullOrEmpty) {
            functionRuntimeStack = FunctionRuntimeStack(runtime, runtimeVersion, runtimeFxVersion, runtimeFxVersion)
        } else {
            functionRuntimeStack = defaultFunctionRuntimeStack
        }

        isDeployToSlot = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_IS_DEPLOY_TO_SLOT) == "1"
        slotName = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_SLOT_NAME) ?: ""

        openInBrowserAfterPublish = JDOMExternalizerUtil.readField(element, AZURE_FUNCTION_APP_OPEN_BROWSER_PUBLISH, "0") == "1"
    }

    fun writeExternal(element: Element) {
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_PROJECT, publishableProject?.projectFilePath ?: "")
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_CONFIGURATION, configuration)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_PLATFORM, platform)

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_SUBSCRIPTION_ID, subscription?.subscriptionId() ?: "")

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_IS_CREATE_APP, if (isCreatingNewApp) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_ID, appId)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_NAME, appName)

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_IS_CREATE_RESOURCE_GROUP, if (isCreatingResourceGroup) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_RESOURCE_GROUP_NAME, resourceGroupName)

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_IS_CREATE_APP_SERVICE_PLAN, if (isCreatingAppServicePlan) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_SERVICE_PLAN_ID, appServicePlanId)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_SERVICE_PLAN_NAME, appServicePlanName)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_LOCATION, location.name())
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_PRICING_TIER, pricingTier.toSkuDescription().name())

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_OPERATING_SYSTEM, operatingSystem.name)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_RUNTIME, functionRuntimeStack.runtime())
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_RUNTIMEVERSION, functionRuntimeStack.version())
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_FXVERSION, functionRuntimeStack.linuxFxVersionForDedicatedPlan)

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_IS_CREATE_STORAGE_ACCOUNT, if (isCreatingStorageAccount) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_STORAGE_ACCOUNT_ID, storageAccountId)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_STORAGE_ACCOUNT_NAME, storageAccountName)
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_STORAGE_ACCOUNT_TYPE, storageAccountType.name().toString())

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_IS_DEPLOY_TO_SLOT, if (isDeployToSlot) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_SLOT_NAME, slotName)

        JDOMExternalizerUtil.writeField(element, AZURE_FUNCTION_APP_OPEN_BROWSER_PUBLISH, if (openInBrowserAfterPublish) "1" else "0")
    }
}