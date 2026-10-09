/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.docker.registry.DockerRegistryManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AbstractAppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.intellij.common.ContainerRegistryModel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class WebAppContainerSettingEditorViewModel(project: Project, parentCs: CoroutineScope) :
    AbstractAppServiceDeploymentViewModel<AppServiceConfig>(
        project,
        parentCs,
        { false }
    ) {
    companion object {
        private val LOG = logger<WebAppContainerSettingEditorViewModel>()
    }

    override val isNetFramework: StateFlow<Boolean> = MutableStateFlow(false)

    val selectedContainerRegistry = MutableStateFlow(findContainerRegistry(null))
    val imageRepository = MutableStateFlow("")
    val imageTag = MutableStateFlow("latest")
    val port = MutableStateFlow(80)

    fun setConfigFromOptions(state: WebAppContainerConfigurationOptions) {
        if (state.webAppName != null && state.resourceGroupName != null && state.subscriptionId != null) {
            val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
            val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
            val runtime = RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE)

            val webAppConfig = AppServiceConfig
                .builder()
                .appName(state.webAppName)
                .subscriptionId(state.subscriptionId)
                .resourceGroup(state.resourceGroupName)
                .region(region)
                .servicePlanName(state.appServicePlanName)
                .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
                .pricingTier(pricingTier)
                .runtime(runtime)
                .build()
            _selectedAppService.value = webAppConfig to null
        } else {
            _selectedAppService.value = null
        }

        val imageNameParts = state.imageRepository
            ?.split('/', limit = 2)
            ?.takeIf { it.size == 2 }
        selectedContainerRegistry.value = findContainerRegistry(imageNameParts?.first())
        imageRepository.value = imageNameParts?.get(1).orEmpty()
        imageTag.value = state.imageTag.orEmpty()
        port.value = state.port
    }

    fun applySelectedConfigToOptions(state: WebAppContainerConfigurationOptions) {
        val webAppConfig = selectedAppService.value?.first ?: return
        val registry = selectedContainerRegistry.value
        val repository = imageRepository.value
        val tag = imageTag.value
        val portValue = port.value

        state.apply {
            webAppName = webAppConfig.appName
            subscriptionId = webAppConfig.subscriptionId
            resourceGroupName = webAppConfig.resourceGroup
            region = webAppConfig.region?.toString()
            appServicePlanName = webAppConfig.servicePlanName
            appServicePlanResourceGroupName = webAppConfig.servicePlanResourceGroup
            pricingTier = webAppConfig.pricingTier?.tier
            pricingSize = webAppConfig.pricingTier?.size
            imageRepository = "${registry?.address}/$repository"
            imageTag = tag
            port = portValue
        }
    }

    private fun findContainerRegistry(address: String?): ContainerRegistryModel? {
        val registries = DockerRegistryManager.getInstance().registries
        val registry =
            if (address == null) registries.firstOrNull()
            else registries.firstOrNull { it.address == address }

        return registry?.let { ContainerRegistryModel(it.name, it.address, it.username, it.authConfig) }
    }

    override suspend fun loadListOfApps(): List<RemoteAppServiceModel<AppServiceConfig>> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            return emptyList()
        }

        loadRemoteResources()

        val webApps = Azure.az(AzureWebApp::class.java).webApps()

        return webApps
            .filter { it.runtime?.isWindows == false }
            .sortedBy { it.name }
            .map { webApp ->
                val config = convertAppServiceToConfig(webApp)
                RemoteAppServiceModel(
                    webApp.resourceGroupName,
                    config,
                    emptyList()
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
            webApps.forEach { webApp ->
                launch { webApp.remote }
            }
        }
    }

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
            runtime = RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE)
            appService.appServicePlan?.let { plan ->
                pricingTier = plan.pricingTier
                servicePlanName = plan.name
                servicePlanResourceGroup = plan.resourceGroupName
            }
        }
    }
}
