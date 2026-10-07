/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureDeploymentState
import com.microsoft.azure.toolkit.intellij.legacy.utils.APPLICATION_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.RESOURCE_GROUP_VALIDATION_MESSAGE
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidApplicationName
import com.microsoft.azure.toolkit.intellij.legacy.utils.isValidResourceGroupName
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebAppContainerDeploymentState(
    project: Project,
    scope: CoroutineScope,
    private val webAppContainerConfiguration: WebAppContainerConfiguration
) : AzureDeploymentState<AppServiceAppBase<*, *, *>>(project, scope) {
    companion object {
        private const val WEBSITES_PORT = "WEBSITES_PORT"
    }

    override suspend fun executeSteps(processHandler: RunProcessHandler): AppServiceAppBase<*, *, *> {
        processHandlerMessenger?.info("Start Web App Container deployment...")

        val options = requireNotNull(webAppContainerConfiguration.state)

        validateOptions(options)

        //push image


        //create web app
        val config = createDotNetAppServiceConfig(options)
        val task = CreateDotNetWebAppTask(config, processHandlerMessenger)
        return task.execute()
    }

    private suspend fun validateOptions(options: WebAppContainerConfigurationOptions) = with(options) {
        val webApp = withContext(Dispatchers.IO) {
            Azure.az(AzureWebApp::class.java)
                .webApps(requireNotNull(subscriptionId))
                .get(requireNotNull(webAppName), requireNotNull(resourceGroupName))
        }
        if (webApp == null) {
            //Validate names only for the new Web Apps
            if (!isValidApplicationName(webAppName))
                throw ExecutionException(APPLICATION_VALIDATION_MESSAGE)
            if (!isValidResourceGroupName(resourceGroupName))
                throw ExecutionException(RESOURCE_GROUP_VALIDATION_MESSAGE)
            if (!isValidApplicationName(appServicePlanName))
                throw ExecutionException("App Service plan names only allow alphanumeric characters and hyphens, cannot start or end in a hyphen, and must be less than 60 chars")
            if (!isValidResourceGroupName(appServicePlanResourceGroupName))
                throw ExecutionException(RESOURCE_GROUP_VALIDATION_MESSAGE)
        }
    }

    private fun createDotNetAppServiceConfig(
        options: WebAppContainerConfigurationOptions
    ) = DotNetAppServiceConfig().apply {
        subscriptionId(options.subscriptionId)
        resourceGroup(options.resourceGroupName)
        region(Region.fromName(requireNotNull(options.region)))
        servicePlanName(options.appServicePlanName)
        servicePlanResourceGroup(options.appServicePlanResourceGroupName)
        val pricingTier = PricingTier(options.pricingTier, options.pricingSize)
        pricingTier(pricingTier)
        appName(options.webAppName)
        runtime = createRuntimeConfig(options)
        dotnetRuntime = createDotNetRuntimeConfig(options)
        appSettings(
            mapOf(
                WEBSITES_PORT to options.port.toString()
            )
        )
    }

    private fun createRuntimeConfig(options: WebAppContainerConfigurationOptions) =
        RuntimeConfig().apply {
            os(OperatingSystem.DOCKER)
            image("${options.imageRepository}:${options.imageTag}")
        }

    private fun createDotNetRuntimeConfig(options: WebAppContainerConfigurationOptions) =
        DotNetRuntimeConfig().apply {
            os(OperatingSystem.LINUX)
            image("${options.imageRepository}:${options.imageTag}")
            isDocker = true
        }

    override fun onSuccess(result: AppServiceAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val options = requireNotNull(webAppContainerConfiguration.state)
        val imageRepository = options.imageRepository
        val imageTag = options.imageTag
        processHandlerMessenger?.info("Image $imageRepository:$imageTag has been deployed to Web App ${result.name}")

        val url = "https://${result.name}.azurewebsites.net/"
        processHandlerMessenger?.info("URL: $url")

        processHandler.notifyComplete()
    }
}