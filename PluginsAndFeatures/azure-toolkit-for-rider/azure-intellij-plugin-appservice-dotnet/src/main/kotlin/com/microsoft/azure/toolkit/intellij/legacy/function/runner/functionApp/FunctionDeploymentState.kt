/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.execution.ExecutionException
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.appservice.DotNetAppServiceDeployer
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.CreateDotNetFunctionAppTask
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppDeploymentSlotDraft
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureDeploymentState
import com.microsoft.azure.toolkit.intellij.legacy.getFunctionStack
import com.microsoft.azure.toolkit.intellij.legacy.getStackAndVersion
import com.microsoft.azure.toolkit.intellij.legacy.utils.APPLICATION_SLOT_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.APPLICATION_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.RESOURCE_GROUP_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidApplicationName
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidApplicationSlotName
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidResourceGroupName
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.jvm.java

class FunctionDeploymentState(
    project: Project,
    scope: CoroutineScope,
    private val functionDeploymentConfiguration: FunctionDeploymentConfiguration
) : AzureDeploymentState<FunctionAppBase<*, *, *>>(project, scope) {
    companion object {
        private const val SCM_DO_BUILD_DURING_DEPLOYMENT = "SCM_DO_BUILD_DURING_DEPLOYMENT"
        private const val WEBSITE_RUN_FROM_PACKAGE = "WEBSITE_RUN_FROM_PACKAGE"
        private const val FUNCTIONS_INPROC_NET8_ENABLED = "FUNCTIONS_INPROC_NET8_ENABLED"
    }

    override suspend fun executeSteps(processHandler: RunProcessHandler): FunctionAppBase<*, *, *> {
        processHandlerMessenger?.info("Start Function App deployment...")

        val options = requireNotNull(functionDeploymentConfiguration.state)
        val publishableProjectPath = options.publishableProjectPath
            ?: throw ExecutionException("Project is not defined")
        val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
            .firstOrNull { it.projectFilePath == publishableProjectPath }
            ?: throw ExecutionException("Project is not defined")

        validateOptions(options)

        checkCanceled()

        val config = creatDotNetFunctionAppConfig(publishableProject, options)
        val createTask = CreateDotNetFunctionAppTask(config, processHandlerMessenger)
        val deployTarget = createTask.execute()

        if (deployTarget is AzResource.Draft<*, *>) {
            deployTarget.reset()
        }

        checkCanceled()

        DotNetAppServiceDeployer
            .getInstance(project)
            .deploy(
                deployTarget,
                publishableProject,
                options.projectConfiguration,
                options.projectPlatform,
            ) { processHandlerMessenger?.info(it) }
            .getOrThrow()

        return deployTarget
    }

    private suspend fun validateOptions(options: FunctionDeploymentConfigurationOptions) = with(options) {
        val functionApp = withContext(Dispatchers.IO) {
            Azure.az(AzureFunctions::class.java)
                .functionApps(requireNotNull(subscriptionId))
                .get(requireNotNull(functionAppName), requireNotNull(resourceGroupName))
        }
        if (functionApp == null) {
            //Validate names only for the new Function Apps
            if (!isValidApplicationName(functionAppName))
                throw ExecutionException(APPLICATION_VALIDATION_MESSAGE)
            if (!isValidResourceGroupName(resourceGroupName))
                throw ExecutionException(RESOURCE_GROUP_VALIDATION_MESSAGE)
            if (!isValidApplicationName(appServicePlanName))
                throw ExecutionException("App Service plan names only allow alphanumeric characters and hyphens, cannot start or end in a hyphen, and must be less than 60 chars")
            if (!isValidResourceGroupName(appServicePlanResourceGroupName))
                throw ExecutionException(RESOURCE_GROUP_VALIDATION_MESSAGE)
        } else {
            if (isDeployToSlot) {
                val slot = withContext(Dispatchers.IO) {
                    functionApp.slots().get(requireNotNull(slotName), requireNotNull(resourceGroupName))
                }
                if (slot == null) {
                    //Validate slot name only for the new Deployment Slots
                    if (!isValidApplicationSlotName(slotName))
                        throw ExecutionException(APPLICATION_SLOT_VALIDATION_MESSAGE)
                }
            }
        }
    }

    private suspend fun creatDotNetFunctionAppConfig(
        publishableProject: PublishableProjectModel,
        options: FunctionDeploymentConfigurationOptions
    ) = DotNetFunctionAppConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.functionAppName)
        deploymentSlotName(options.slotName)
        val configurationSource = when (options.slotConfigurationSource) {
            "Do not clone settings" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_NEW
            "parent" -> DotNetFunctionAppDeploymentSlotDraft.CONFIGURATION_SOURCE_PARENT
            null -> null
            else -> options.slotConfigurationSource
        }
        deploymentSlotConfigurationSource(configurationSource)
        storageAccountName(options.storageAccountName)
        storageAccountResourceGroup(options.storageAccountResourceGroup)
        val os = OperatingSystem.fromString(options.operatingSystem)
        runtime = createRuntimeConfig(os)
        val dotnetRuntimeConfig = createDotNetRuntimeConfig(publishableProject, os)
        dotnetRuntime = dotnetRuntimeConfig
        flexConsumptionConfiguration = createFlexConsumptionConfiguration(options)
        appSettings(configureAppSettings(pricingTier, runtime, dotnetRuntimeConfig))
    }

    private fun createRuntimeConfig(os: OperatingSystem) =
        RuntimeConfig().apply {
            this.os = os
        }

    private suspend fun createDotNetRuntimeConfig(publishableProject: PublishableProjectModel, os: OperatingSystem) =
        DotNetRuntimeConfig().apply {
            os(os)
            isDocker = false
            val stackAndVersion = publishableProject.getStackAndVersion(project, os, true)
            stack = stackAndVersion?.runtimeStack
            dotnetVersion = stackAndVersion?.dotnetVersion
            frameworkVersion = stackAndVersion?.frameworkVersion
            functionStack = publishableProject.getFunctionStack(project, os)
        }

    private fun createFlexConsumptionConfiguration(options: FunctionDeploymentConfigurationOptions) =
        FlexConsumptionConfiguration().apply {
            deploymentResourceGroup = options.deploymentResourceGroup
            deploymentAccount = options.deploymentAccountName
            instanceSize = options.instanceSize
        }

    private fun configureAppSettings(
        pricingTier: PricingTier,
        runtime: RuntimeConfig,
        dotnetRuntime: DotNetRuntimeConfig
    ) = buildMap {
        //Controls remote build behavior during deployment.
        //see: https://learn.microsoft.com/en-us/azure/azure-functions/functions-app-settings#scm_do_build_during_deployment
        if (pricingTier == PricingTier.CONSUMPTION && runtime.os == OperatingSystem.LINUX) {
            put(SCM_DO_BUILD_DURING_DEPLOYMENT, "0")
        }

        //Enables your function app to run from a package file, which can be locally mounted or deployed to an external URL.
        //see: https://learn.microsoft.com/en-us/azure/azure-functions/run-functions-from-deployment-package
        if (pricingTier != PricingTier.FLEX_CONSUMPTION &&
            (runtime.os == OperatingSystem.WINDOWS || (runtime.os == OperatingSystem.LINUX && pricingTier != PricingTier.CONSUMPTION))
        ) {
            put(WEBSITE_RUN_FROM_PACKAGE, "1")
        }

        //Indicates whether an app can use .NET 8 on the in-process model.
        //see: https://learn.microsoft.com/en-us/azure/azure-functions/functions-dotnet-class-library?tabs=v4%2Ccmd#updating-to-target-net-8
        if (dotnetRuntime.functionStack?.runtime() == "DOTNET" &&
            (dotnetRuntime.stack?.version() == "8.0" || dotnetRuntime.frameworkVersion?.toString() == "v8.0")
        ) {
            put(FUNCTIONS_INPROC_NET8_ENABLED, "1")
        }
    }

    override fun onSuccess(result: FunctionAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val options = requireNotNull(functionDeploymentConfiguration.state)

        updateConfigurationDataModel(result)
        processHandlerMessenger?.info("Deployment was successful, but the app may still be starting.")

        val url = "https://${result.hostName}"
        processHandlerMessenger?.info("URL: $url")
        if (options.openBrowser) {
            BrowserUtil.open(url)
        }

        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: FunctionAppBase<*, *, *>) {
        functionDeploymentConfiguration.state?.apply {
            if (app is FunctionAppDeploymentSlot) {
                slotName = app.name
                slotConfigurationSource = null
            }
        }
    }
}