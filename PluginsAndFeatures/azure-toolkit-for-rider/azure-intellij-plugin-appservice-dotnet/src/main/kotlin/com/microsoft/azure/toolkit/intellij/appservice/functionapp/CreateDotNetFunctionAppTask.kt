/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils
import com.microsoft.azure.toolkit.intellij.appservice.CreateAppServiceTask
import com.microsoft.azure.toolkit.intellij.appservice.servicePlan.CreateServicePlanTask
import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.function.*
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.Operation
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask
import com.microsoft.azure.toolkit.lib.storage.StorageAccount
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.Callable

class CreateDotNetFunctionAppTask(
    private val config: DotNetFunctionAppConfig,
    processHandlerMessager: RiderRunProcessHandlerMessager?
) : CreateAppServiceTask<FunctionAppBase<*, *, *>>(processHandlerMessager) {
    companion object {
        private const val FUNCTION_APP_NAME_PATTERN = "[^a-zA-Z0-9]"
    }

    private val functionAppRegex = Regex(FUNCTION_APP_NAME_PATTERN)

    private var appServicePlan: AppServicePlan? = null
    private var storageAccount: StorageAccount? = null
    private var deploymentContainerUrl: String? = null
    private var functionApp: FunctionAppBase<*, *, *>? = null

    init {
        val functionApp = Azure.az(AzureFunctions::class.java)
            .functionApps(config.subscriptionId)
            .getOrDraft(config.appName(), config.resourceGroup())

        if (functionApp.isDraftForCreating) {
            registerSubTask(createResourceGroupTask()) {}
            registerSubTask(createServicePlanTask()) { appServicePlan = it }
            registerSubTask(createStorageAccountTask()) { storageAccount = it }

            if (config.pricingTier.isFlexConsumption) {
                registerSubTask(createDeploymentContainerTask()) { deploymentContainerUrl = it }
            }

            val functionAppDraft = (functionApp as? FunctionAppDraft)?.toDotNetFunctionAppDraft()
                ?: error("Unable to get function app draft")
            registerSubTask(createFunctionAppTask(functionAppDraft)) {
                this@CreateDotNetFunctionAppTask.functionApp = it
            }
        } else if (!config.deploymentSlotName().isNullOrEmpty()) {
            if (requireNotNull(functionApp.appServicePlan).pricingTier.isFlexConsumption) {
                throw AzureToolkitRuntimeException("Deployment slot is not supported for function app with consumption plan")
            }

            val slot = functionApp
                .slots()
                .getOrDraft(config.deploymentSlotName(), config.resourceGroup())
            if (slot.isDraftForCreating) {
                val slotDraft = (slot as? FunctionAppDeploymentSlotDraft)?.toDotNetFunctionAppDeploymentSlotDraft()
                    ?: error("Unable to get function app deployment slot draft")
                registerSubTask(createFunctionSlotTask(slotDraft)) {
                    this@CreateDotNetFunctionAppTask.functionApp = it
                }
            } else {
                this@CreateDotNetFunctionAppTask.functionApp = slot
            }
        } else {
            this@CreateDotNetFunctionAppTask.functionApp = functionApp
        }
    }

    private fun FunctionAppDraft.toDotNetFunctionAppDraft(): DotNetFunctionAppDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetFunctionAppDraft(draftOrigin)
            else DotNetFunctionAppDraft(name, resourceGroupName, module as FunctionAppModule)

        return draft
    }

    private fun FunctionAppDeploymentSlotDraft.toDotNetFunctionAppDeploymentSlotDraft(): DotNetFunctionAppDeploymentSlotDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetFunctionAppDeploymentSlotDraft(draftOrigin)
            else DotNetFunctionAppDeploymentSlotDraft(name, module as FunctionAppDeploymentSlotModule)

        return draft
    }

    private fun createResourceGroupTask(): AzureTask<ResourceGroup> {
        val resourceGroupRegion = getNonStageRegion(config.region())
        return CreateResourceGroupTask(
            config.subscriptionId,
            config.resourceGroup(),
            resourceGroupRegion
        )
    }

    private fun createServicePlanTask(): AzureTask<AppServicePlan> =
        CreateServicePlanTask(AppServiceConfig.getServicePlanConfig(config))

    private fun createStorageAccountTask(): AzureTask<StorageAccount> {
        val storageResourceGroup = config.storageAccountResourceGroup() ?: config.resourceGroup()
        val storageAccountName = config.storageAccountName() ?: getDefaultStorageAccountName(config.appName())
        val storageAccountRegion = getNonStageRegion(config.region())

        return CreateStorageAccountTask(
            config.subscriptionId,
            storageResourceGroup,
            storageAccountName,
            storageAccountRegion
        )
    }

    private fun getDefaultStorageAccountName(functionAppName: String): String {
        val context = ResourceManagerUtils.InternalRuntimeContext()
        return context.randomResourceName(functionAppName.replace(functionAppRegex, ""), 20)
    }

    private fun getNonStageRegion(region: Region): Region {
        val regionName = region.name
        if (!regionName.contains("stage", true)) {
            return region
        }

        return regionName.let {
            var name = StringUtils.removeIgnoreCase(it, "(stage)")
            name = StringUtils.removeIgnoreCase(name, "stage")
            name = name.trim()
            return@let Region.fromName(name)
        }
    }

    private fun createDeploymentContainerTask(): AzureTask<String> {
        val configuration = config.flexConsumptionConfiguration()
        val deploymentResourceGroup = configuration.deploymentResourceGroup ?: config.resourceGroup()
        val deploymentAccountName = configuration.deploymentAccount ?: requireNotNull(storageAccount).name
        val deploymentContainer =
            configuration.deploymentContainer ?: getDefaultDeploymentContainerName(config.appName())

        return CreateDeploymentContainerTask(
            config.subscriptionId,
            deploymentResourceGroup,
            deploymentAccountName,
            deploymentContainer
        )
    }

    private fun getDefaultDeploymentContainerName(functionAppName: String): String {
        val context = ResourceManagerUtils.InternalRuntimeContext()
        return context.randomResourceName(functionAppName.replace(functionAppRegex, ""), 20)
    }

    private fun createFunctionAppTask(draft: DotNetFunctionAppDraft) =
        AzureTask(
            "Create new app(${config.appName()}) on subscription(${config.subscriptionId()})",
            Callable {
                with(draft) {
                    appServicePlan = this@CreateDotNetFunctionAppTask.appServicePlan
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    storageAccount = this@CreateDotNetFunctionAppTask.storageAccount
                    flexConsumptionConfiguration = config.flexConsumptionConfiguration()
                    deploymentAccount = this@CreateDotNetFunctionAppTask.storageAccount
                    deploymentContainerUrl = this@CreateDotNetFunctionAppTask.deploymentContainerUrl
                    appSettings = config.appSettings()

                    createIfNotExist()
                }
            })

    private fun createFunctionSlotTask(draft: DotNetFunctionAppDeploymentSlotDraft) =
        AzureTask(
            "Create new slot(${config.deploymentSlotName()}) on function app (${config.appName()})",
            Callable {
                with(draft) {
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    configurationSource = config.deploymentSlotConfigurationSource()
                    appSettings = config.appSettings()

                    createIfNotExist()
                }
            })

    override fun doExecute(): FunctionAppBase<*, *, *> {
        Operation.execute(AzureString.fromString("Creating or updating Function App"), {
            executeSubTasks()
        }, null)

        return requireNotNull(functionApp)
    }
}