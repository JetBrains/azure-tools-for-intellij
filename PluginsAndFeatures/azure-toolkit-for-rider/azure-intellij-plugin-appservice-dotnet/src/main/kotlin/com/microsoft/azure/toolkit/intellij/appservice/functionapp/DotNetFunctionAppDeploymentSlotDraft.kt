/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.azure.core.management.exception.ManagementException
import com.azure.resourcemanager.appservice.fluent.models.SitePatchResourceInner
import com.azure.resourcemanager.appservice.models.FunctionApp
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.getDotNetRuntime
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotModule
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource

class DotNetFunctionAppDeploymentSlotDraft : FunctionAppDeploymentSlot,
    AzResource.Draft<FunctionAppDeploymentSlot, FunctionDeploymentSlot> {
    companion object {
        const val CONFIGURATION_SOURCE_PARENT = "parent"
        const val CONFIGURATION_SOURCE_NEW = "new"
    }

    constructor(name: String, module: FunctionAppDeploymentSlotModule) : super(name, module) {
        origin = null
    }

    constructor(origin: FunctionAppDeploymentSlot) : super(origin) {
        this.origin = origin
    }

    private val origin: FunctionAppDeploymentSlot?

    private var config: Config? = null

    private val lock = Any()

    override fun getOrigin() = origin

    override fun reset() {
        config = null
    }

    private fun ensureConfig(): Config {
        synchronized(lock) {
            val localConfig = config ?: Config()
            config = localConfig
            return localConfig
        }
    }

    override fun isModified() = false

    override fun createResourceInAzure(): FunctionDeploymentSlot {
        val functionApp = checkNotNull(parent.remote)

        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig
        val newConfigurationSource = configurationSource
        val newFlexConsumptionConfiguration = flexConsumptionConfiguration
        val source =
            if (newConfigurationSource.isNullOrEmpty()) CONFIGURATION_SOURCE_PARENT else newConfigurationSource.lowercase()

        val blank = functionApp.deploymentSlots().define(name)
        val withCreate =
            when (source) {
                CONFIGURATION_SOURCE_NEW -> blank.withBrandNewConfiguration()
                CONFIGURATION_SOURCE_PARENT -> blank.withConfigurationFromParent()
                else -> {
                    try {
                        val sourceSlot = functionApp.deploymentSlots().getByName(newConfigurationSource)
                        checkNotNull(sourceSlot) { "Target slot configuration source does not exists in current app" }
                        blank.withConfigurationFromDeploymentSlot(sourceSlot)
                    } catch (e: ManagementException) {
                        throw AzureToolkitRuntimeException("Failed to get configuration source slot", e)
                    }
                }
            }

        if (!newAppSettings.isNullOrEmpty())
            withCreate.withAppSettings(newAppSettings)
        if (newDiagnosticConfig != null)
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig)
        val updateFlexConsumptionConfiguration =
            newFlexConsumptionConfiguration != null && parent.appServicePlan?.pricingTier?.isFlexConsumption == true
        if (updateFlexConsumptionConfiguration) {
            (withCreate as? FunctionApp)
                ?.innerModel()
                ?.withContainerSize(newFlexConsumptionConfiguration.instanceSize)
        }

        val messager = AzureMessager.getMessager()
        messager.info("Start creating Function App deployment slot ($name)...")

        val slot = withCreate.create()
        if (updateFlexConsumptionConfiguration) {
            updateFlexConsumptionConfiguration(slot, newFlexConsumptionConfiguration)
        }

        messager.success("Function App deployment slot ($name) is successfully created")

        return slot
    }

    override fun updateResourceInAzure(remote: FunctionDeploymentSlot): FunctionDeploymentSlot {
        throw AzureToolkitRuntimeException("Updating function app deployment slot is not supported")
    }

    private fun updateFlexConsumptionConfiguration(
        slot: FunctionDeploymentSlot,
        flexConfiguration: FlexConsumptionConfiguration
    ) {
        val name = "${slot.parent().name()}/slots/${slot.name()}"
        val webApps = slot.manager().serviceClient().webApps
        if (flexConfiguration.maximumInstances != null || flexConfiguration.alwaysReadyInstances != null) {
            val configuration = webApps.getConfiguration(slot.resourceGroupName(), name)
            if (flexConfiguration.maximumInstances != configuration.functionAppScaleLimit() ||
                flexConfiguration.alwaysReadyInstances.size != configuration.minimumElasticInstanceCount()
            ) {
                configuration.withFunctionAppScaleLimit(flexConfiguration.maximumInstances)
                webApps.updateConfiguration(slot.resourceGroupName(), name, configuration)
            }
        }
        if (slot.innerModel().containerSize() != flexConfiguration.instanceSize) {
            val patch = SitePatchResourceInner()
                .withContainerSize(flexConfiguration.instanceSize)
            webApps
                .updateWithResponseAsync(slot.resourceGroupName(), name, patch)
                .block()
        }
    }

    var dotNetRuntime: DotNetRuntime?
        get() = config?.runtime ?: remote?.getDotNetRuntime()
        set(value) {
            ensureConfig().runtime = value
        }
    var dockerConfiguration: DockerConfiguration?
        get() = config?.dockerConfiguration
        set(value) {
            ensureConfig().dockerConfiguration = value
        }
    var configurationSource: String?
        get() = config?.configurationSource
        set(value) {
            ensureConfig().configurationSource = value
        }

    override fun getAppSettings() = config?.appSettings ?: super.getAppSettings()
    fun setAppSettings(value: Map<String, String>?) {
        ensureConfig().appSettings = value
    }

    override fun getDiagnosticConfig() = config?.diagnosticConfig ?: super.getDiagnosticConfig()
    fun setDiagnosticConfig(value: DiagnosticConfig?) {
        ensureConfig().diagnosticConfig = value
    }

    override fun getFlexConsumptionConfiguration() =
        config?.flexConsumptionConfiguration ?: super.getFlexConsumptionConfiguration()

    data class Config(
        var runtime: DotNetRuntime? = null,
        var dockerConfiguration: DockerConfiguration? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var configurationSource: String? = null,
        var flexConsumptionConfiguration: FlexConsumptionConfiguration? = null,
        var appSettings: Map<String, String>? = null
    )
}