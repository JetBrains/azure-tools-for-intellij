/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.docker.registry.DockerRegistryManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AbstractAppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.intellij.common.ContainerRegistryModel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.model.Region
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class FunctionAppContainerSettingEditorViewModel(project: Project, parentCs: CoroutineScope) :
    AbstractAppServiceDeploymentViewModel<FunctionAppConfig>(
        project,
        parentCs,
        { false }
    ) {
    companion object {
        private val LOG = logger<FunctionAppContainerSettingEditorViewModel>()
    }

    override val isNetFramework: StateFlow<Boolean> = MutableStateFlow(false)

    val selectedContainerRegistry = MutableStateFlow(findContainerRegistry(null))
    val imageRepository = MutableStateFlow("")
    val imageTag = MutableStateFlow("latest")

    fun setConfigFromOptions(state: FunctionAppContainerConfigurationOptions) {
        if (state.functionAppName != null && state.resourceGroupName != null && state.subscriptionId != null) {
            val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
            val pricingTier = PricingTier(state.pricingTier, state.pricingSize)
            val runtime = RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE)

            val functionAppConfig = FunctionAppConfig
                .builder()
                .appName(state.functionAppName)
                .subscriptionId(state.subscriptionId)
                .resourceGroup(state.resourceGroupName)
                .region(region)
                .servicePlanName(state.appServicePlanName)
                .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
                .pricingTier(pricingTier)
                .runtime(runtime)
                .storageAccountName(state.storageAccountName)
                .storageAccountResourceGroup(state.storageAccountResourceGroup)
                .build()
            _selectedAppService.value = functionAppConfig to null
        } else {
            _selectedAppService.value = null
        }

        val imageNameParts = state.imageRepository
            ?.split('/', limit = 2)
            ?.takeIf { it.size == 2 }
        selectedContainerRegistry.value = findContainerRegistry(imageNameParts?.first())
        imageRepository.value = imageNameParts?.get(1).orEmpty()
        imageTag.value = state.imageTag.orEmpty()
    }

    fun applySelectedConfigToOptions(state: FunctionAppContainerConfigurationOptions) {
        val functionAppConfig = selectedAppService.value?.first
        val registry = selectedContainerRegistry.value
        val repository = imageRepository.value
        val tag = imageTag.value

        state.apply {
            functionAppName = functionAppConfig?.appName
            subscriptionId = functionAppConfig?.subscriptionId
            resourceGroupName = functionAppConfig?.resourceGroup
            region = functionAppConfig?.region?.toString()
            appServicePlanName = functionAppConfig?.servicePlanName
            appServicePlanResourceGroupName = functionAppConfig?.servicePlanResourceGroup
            pricingTier = functionAppConfig?.pricingTier?.tier
            pricingSize = functionAppConfig?.pricingTier?.size
            storageAccountName = functionAppConfig?.storageAccountName
            storageAccountResourceGroup = functionAppConfig?.storageAccountResourceGroup
            imageRepository = "${registry?.address}/$repository"
            imageTag = tag
        }
    }

    private fun findContainerRegistry(address: String?): ContainerRegistryModel? {
        val registries = DockerRegistryManager.getInstance().registries
        val registry =
            if (address == null) registries.firstOrNull()
            else registries.firstOrNull { it.address == address }

        return registry?.let { ContainerRegistryModel(it.name, it.address, it.username, it.authConfig) }
    }

    override suspend fun loadListOfApps(): List<RemoteAppServiceModel<FunctionAppConfig>> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            return emptyList()
        }

        loadRemoteResources()

        val functionApps = Azure.az(AzureFunctions::class.java).functionApps()

        return functionApps
            .filter { it.runtime?.isWindows == false }
            .sortedBy { it.name }
            .map { functionApp ->
                val config = convertAppServiceToConfig(functionApp)
                RemoteAppServiceModel(
                    functionApp.resourceGroupName,
                    config,
                    emptyList()
                )
            }
    }

    private suspend fun loadRemoteResources() {
        LOG.trace("Loading function apps and app service plans from Azure")

        coroutineScope {
            launch { loadAppServicePlans() }
            launch { loadFunctionApps() }
        }
    }

    private suspend fun loadFunctionApps() {
        val functionApps = Azure.az(AzureFunctions::class.java).functionApps()
        coroutineScope {
            functionApps.forEach { functionApp ->
                launch { functionApp.remote }
            }
        }
    }

    override fun invalidateAppCache() {
        try {
            Azure.az(AzureFunctions::class.java).refresh()
        } catch (e: Exception) {
            LOG.warn("Error while invalidating function app cache", e)
        }
    }

    private fun convertAppServiceToConfig(appService: AppServiceAppBase<*, *, *>): FunctionAppConfig {
        return FunctionAppConfig().apply {
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
