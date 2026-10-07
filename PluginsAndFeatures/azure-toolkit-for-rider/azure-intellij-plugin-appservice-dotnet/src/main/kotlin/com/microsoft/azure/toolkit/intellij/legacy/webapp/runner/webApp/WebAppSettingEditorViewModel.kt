/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AbstractAppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class WebAppSettingEditorViewModel(project: Project, parentCs: CoroutineScope) :
    AbstractAppServiceDeploymentViewModel<AppServiceConfig>(
        project,
        parentCs,
        { it.isWeb && (it.isDotNetCore || SystemInfo.isWindows) }
    ) {
    companion object {
        private val LOG = logger<WebAppSettingEditorViewModel>()
    }

    override val isNetFramework: StateFlow<Boolean> by lazy {
        selectedProject.map { sp ->
            if (sp == null) return@map false
            !sp.isDotNetCore
        }.stateIn(cs, SharingStarted.Eagerly, false)
    }

    fun setConfigFromOptions(state: WebAppConfigurationOptions) {
        if (state.webAppName != null && state.resourceGroupName != null && state.subscriptionId != null) {
            val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
            val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
            val operatingSystem = OperatingSystem.fromString(state.operatingSystem)

            val webAppConfig = AppServiceConfig
                .builder()
                .appName(state.webAppName)
                .subscriptionId(state.subscriptionId)
                .resourceGroup(state.resourceGroupName)
                .region(region)
                .servicePlanName(state.appServicePlanName)
                .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
                .pricingTier(pricingTier)
                .runtime(RuntimeConfig().apply { os = operatingSystem })
                .build()
            val deploymentSlotName = if (state.isDeployToSlot) state.slotName else null
            _selectedAppService.value = webAppConfig to deploymentSlotName
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

    fun applySelectedConfigToOptions(state: WebAppConfigurationOptions) {
        val webAppValue = selectedAppService.value ?: return
        val webAppConfig = webAppValue.first
        val slotNameConfig = webAppValue.second
        val projectPath = selectedProject.value?.projectFilePath
        val cap = selectedConfigurationAndPlatform.value
        val openBrowserValue = openBrowserAfterDeployment.value

        state.apply {
            webAppName = webAppConfig.appName
            subscriptionId = webAppConfig.subscriptionId
            resourceGroupName = webAppConfig.resourceGroup
            region = webAppConfig.region?.toString()
            appServicePlanName = webAppConfig.servicePlanName
            appServicePlanResourceGroupName = webAppConfig.servicePlanResourceGroup
            pricingTier = webAppConfig.pricingTier?.tier
            pricingSize = webAppConfig.pricingTier?.size
            operatingSystem = webAppConfig.runtime?.os?.toString()
            if (slotNameConfig != null) {
                isDeployToSlot = true
                slotName = slotNameConfig
            } else {
                isDeployToSlot = false
                slotName = null
            }

            publishableProjectPath = projectPath
            projectConfiguration = cap?.configuration
            projectPlatform = cap?.platform

            openBrowser = openBrowserValue
        }
    }

    override suspend fun loadListOfApps(): List<RemoteAppServiceModel<AppServiceConfig>> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            LOG.trace("User is not logged in, skipping web app loading")
            return emptyList()
        }

        loadRemoteResources()

        val webApps = Azure.az(AzureWebApp::class.java).webApps()

        return webApps
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
     * This method loads remote web apps and app service plans in parallel.
     * The loaded web apps will be saved in the cache, so the further calls won't load them from Azure again.
     */
    private suspend fun loadRemoteResources() {
        LOG.trace("Loading web apps and app service plans from Azure")

        coroutineScope {
            launch { loadAppServicePlans() }
            launch { loadWebApps() }
        }
    }

    private suspend fun loadWebApps() {
        val webApps = Azure.az(AzureWebApp::class.java).webApps()
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
     * This method invalidates the cache and reloads web apps from Azure.
     */
    override fun invalidateAppCache() {
        try {
            Azure.az(AzureWebApp::class.java).refresh()
        } catch (e: Exception) {
            LOG.warn("Error while invalidating web app cache", e)
        }
    }

    private fun convertAppServiceToConfig(appService: AppServiceAppBase<*, *, *>): AppServiceConfig {
        return AppServiceConfig().apply {
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
