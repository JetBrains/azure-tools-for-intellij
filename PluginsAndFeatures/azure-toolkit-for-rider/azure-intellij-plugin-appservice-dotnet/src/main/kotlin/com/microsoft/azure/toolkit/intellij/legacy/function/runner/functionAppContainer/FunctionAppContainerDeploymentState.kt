/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.CreateDotNetFunctionAppTask
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.DotNetFunctionAppConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureDeploymentState
import com.microsoft.azure.toolkit.intellij.legacy.utils.APPLICATION_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.RESOURCE_GROUP_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidApplicationName
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidResourceGroupName
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FunctionAppContainerDeploymentState(
    project: Project,
    scope: CoroutineScope,
    private val functionAppContainerConfiguration: FunctionAppContainerConfiguration
) : AzureDeploymentState<FunctionAppBase<*, *, *>>(project, scope) {

    override suspend fun executeSteps(processHandler: RunProcessHandler): FunctionAppBase<*, *, *> {
        processHandlerMessenger?.info("Start Function App Container deployment...")

        val options = requireNotNull(functionAppContainerConfiguration.state)

        validateOptions(options)

        //push image

        //create function app
        val config = createDotNetFunctionAppConfig(options)
        val task = CreateDotNetFunctionAppTask(config, processHandlerMessenger)
        return task.execute()
    }

    private suspend fun validateOptions(options: FunctionAppContainerConfigurationOptions) = with(options) {
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
        }
    }

    private fun createDotNetFunctionAppConfig(
        options: FunctionAppContainerConfigurationOptions
    ) = DotNetFunctionAppConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.functionAppName)
        storageAccountName(options.storageAccountName)
        storageAccountResourceGroup(options.storageAccountResourceGroup)
        runtime = createRuntimeConfig(options)
        dotnetRuntime = createDotNetRuntimeConfig(options)
        appSettings(
            mapOf(
                "WEBSITES_ENABLE_APP_SERVICE_STORAGE" to "false"
            )
        )
    }

    private fun createRuntimeConfig(options: FunctionAppContainerConfigurationOptions) =
        RuntimeConfig().apply {
            os(OperatingSystem.DOCKER)
            image("${options.imageRepository}:${options.imageTag}")
        }

    private fun createDotNetRuntimeConfig(options: FunctionAppContainerConfigurationOptions) =
        DotNetRuntimeConfig().apply {
            os(OperatingSystem.LINUX)
            image("${options.imageRepository}:${options.imageTag}")
            isDocker = true
        }

    override fun onSuccess(result: FunctionAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val options = requireNotNull(functionAppContainerConfiguration.state)
        val imageRepository = options.imageRepository
        val imageTag = options.imageTag
        processHandlerMessenger?.info("Image $imageRepository:$imageTag has been deployed to Function App ${result.name}")

        val url = "https://${result.name}.azurewebsites.net/"
        processHandlerMessenger?.info("URL: $url")

        processHandler.notifyComplete()
    }
}