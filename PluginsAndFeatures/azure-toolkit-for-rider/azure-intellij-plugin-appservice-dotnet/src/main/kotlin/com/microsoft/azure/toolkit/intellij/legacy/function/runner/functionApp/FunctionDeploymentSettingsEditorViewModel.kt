/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AbstractAppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FunctionDeploymentSettingsEditorViewModel(project: Project, parentCs: CoroutineScope) :
    AbstractAppServiceDeploymentViewModel<FunctionAppConfig>(
        project,
        parentCs,
        { it.isAzureFunction }
    ) {
    companion object {
        private val LOG = logger<FunctionDeploymentSettingsEditorViewModel>()
    }

    override val isNetFramework: StateFlow<Boolean> by lazy {
        selectedProject.map { sp ->
            if (sp == null) return@map false
            !sp.isDotNetCore || sp.projectOutputs.all { it.tfmInMsbuildFormat == "net48" }
        }.stateIn(cs, SharingStarted.Eagerly, false)
    }

    fun setConfigFromOptions(state: FunctionDeploymentConfigurationOptions) {
        if (state.functionAppName != null && state.resourceGroupName != null && state.subscriptionId != null) {
            val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
            val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
            val operatingSystem = OperatingSystem.fromString(state.operatingSystem)

            val flexConsumptionConfiguration = if (pricingTier.isFlexConsumption) {
                FlexConsumptionConfiguration.builder()
                    .deploymentResourceGroup(state.deploymentResourceGroup)
                    .deploymentAccount(state.deploymentAccountName)
                    .instanceSize(state.instanceSize)
                    .build()
            } else null

            val functionAppConfig = FunctionAppConfig
                .builder()
                .appName(state.functionAppName)
                .subscriptionId(state.subscriptionId)
                .resourceGroup(state.resourceGroupName)
                .region(region)
                .servicePlanName(state.appServicePlanName)
                .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
                .pricingTier(pricingTier)
                .runtime(RuntimeConfig().apply { os = operatingSystem })
                .storageAccountName(state.storageAccountName)
                .storageAccountResourceGroup(state.storageAccountResourceGroup)
                .flexConsumptionConfiguration(flexConsumptionConfiguration)
                .build()
            val deploymentSlotName = if (state.isDeployToSlot) state.slotName else null
            _selectedAppService.value = functionAppConfig to deploymentSlotName
        }

        val publishableProject = _publishableProjects.value
            .firstOrNull { it.projectFilePath == state.publishableProjectPath }
        if (publishableProject != null) {
            selectedProject.value = publishableProject
        }

        val config = state.projectConfiguration
        val platform = state.projectPlatform
        if (config != null && platform != null) {
            selectedConfigurationAndPlatform.value = ConfigurationAndPlatform(config, platform)
        }

        _openBrowserAfterDeployment.value = state.openBrowser
    }

    fun applySelectedConfigToOptions(state: FunctionDeploymentConfigurationOptions) {
        val functionAppValue = selectedAppService.value ?: return
        val functionAppConfig = functionAppValue.first
        val slotNameConfig = functionAppValue.second
        val projectPath = selectedProject.value?.projectFilePath
        val cap = selectedConfigurationAndPlatform.value
        val openBrowserValue = openBrowserAfterDeployment.value

        state.apply {
            functionAppName = functionAppConfig.appName
            subscriptionId = functionAppConfig.subscriptionId
            resourceGroupName = functionAppConfig.resourceGroup
            region = functionAppConfig.region?.toString()
            appServicePlanName = functionAppConfig.servicePlanName
            appServicePlanResourceGroupName = functionAppConfig.servicePlanResourceGroup
            pricingTier = functionAppConfig.pricingTier?.tier
            pricingSize = functionAppConfig.pricingTier?.size
            operatingSystem = functionAppConfig.runtime?.os?.toString()
            if (slotNameConfig != null) {
                isDeployToSlot = true
                slotName = slotNameConfig
            } else {
                isDeployToSlot = false
                slotName = null
            }
            storageAccountName = functionAppConfig.storageAccountName
            storageAccountResourceGroup = functionAppConfig.storageAccountResourceGroup
            deploymentAccountName = functionAppConfig.flexConsumptionConfiguration?.deploymentAccount
            deploymentResourceGroup = functionAppConfig.flexConsumptionConfiguration?.deploymentResourceGroup
            instanceSize = functionAppConfig.flexConsumptionConfiguration?.instanceSize ?: 0

            publishableProjectPath = projectPath
            projectConfiguration = cap?.configuration
            projectPlatform = cap?.platform

            openBrowser = openBrowserValue
        }
    }

    override suspend fun loadListOfApps(): List<RemoteAppServiceModel<FunctionAppConfig>> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            LOG.trace("User is not logged in, skipping web app loading")
            return emptyList()
        }

        loadRemoteResources()

        val functionApps = Azure.az(AzureFunctions::class.java).functionApps()

        return functionApps
            .sortedBy { it.name }
            .map { webApp ->
                val config = convertAppServiceToConfig(webApp)
                val deploymentSlots = webApp.slots().list().map { it.name }
                RemoteAppServiceModel(
                    webApp.resourceGroupName,
                    config,
                    deploymentSlots
                )
            }
    }

    /**
     * This method loads remote function apps and app service plans in parallel.
     * The loaded function apps will be saved in the cache, so the further calls won't load them from Azure again.
     */
    private suspend fun loadRemoteResources() {
        LOG.trace("Loading web apps and app service plans from Azure")

        coroutineScope {
            launch { loadAppServicePlans() }
            launch { loadFunctionApps() }
        }
    }

    private suspend fun loadFunctionApps() {
        val webApps = Azure.az(AzureFunctions::class.java).functionApps()
        coroutineScope {
            webApps.forEach {
                launch {
                    it.remote
                }
                launch {
                    it.slots().list()
                }
            }
        }
    }

    /**
     * This method invalidates the cache and reloads function apps from Azure.
     */
    override fun invalidateAppCache() {
        try {
            Azure.az(AzureFunctions::class.java).refresh()
        } catch (e: Exception) {
            LOG.warn("Error while invalidating web app cache", e)
        }
    }

    private fun convertAppServiceToConfig(appService: AppServiceAppBase<*, *, *>): FunctionAppConfig {
        return FunctionAppConfig().apply {
            subscriptionId = appService.subscriptionId
            resourceGroup = appService.resourceGroupName
            appName = appService.name
            region = appService.region
            runtime = RuntimeConfig().apply {
                os = OperatingSystem.fromString(appService.remote?.operatingSystem()?.name)
            }
            val servicePlan = appService.appServicePlan
            servicePlan?.also {
                pricingTier = it.pricingTier
                servicePlanName = it.name
                servicePlanResourceGroup = it.resourceGroupName
            }
        }
    }
}