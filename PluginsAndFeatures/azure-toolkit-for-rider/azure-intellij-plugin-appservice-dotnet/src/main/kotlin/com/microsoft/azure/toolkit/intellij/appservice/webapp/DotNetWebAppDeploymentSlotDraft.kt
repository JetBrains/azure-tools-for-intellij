/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.azure.core.management.exception.ManagementException
import com.azure.resourcemanager.appservice.models.DeploymentSlot
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.getDotNetRuntime
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotModule
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource

class DotNetWebAppDeploymentSlotDraft : WebAppDeploymentSlot, AzResource.Draft<WebAppDeploymentSlot, DeploymentSlot> {
    companion object {
        const val CONFIGURATION_SOURCE_PARENT = "parent"
        const val CONFIGURATION_SOURCE_NEW = "new"
    }

    constructor(name: String, module: WebAppDeploymentSlotModule) : super(name, module) {
        origin = null
    }

    constructor(origin: WebAppDeploymentSlot) : super(origin) {
        this.origin = origin
    }

    private val origin: WebAppDeploymentSlot?

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

    override fun createResourceInAzure(): DeploymentSlot {
        val webApp = checkNotNull(parent.remote)

        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig
        val newConfigurationSource = configurationSource
        val source =
            if (newConfigurationSource.isNullOrEmpty()) CONFIGURATION_SOURCE_PARENT
            else newConfigurationSource.lowercase()

        val blank = webApp.deploymentSlots().define(name)
        val withCreate =
            when (source) {
                CONFIGURATION_SOURCE_NEW -> blank.withBrandNewConfiguration()
                CONFIGURATION_SOURCE_PARENT -> blank.withConfigurationFromParent()
                else -> {
                    try {
                        val sourceSlot = webApp.deploymentSlots().getByName(newConfigurationSource)
                        checkNotNull(sourceSlot) { "Target slot configuration source does not exists in current web app" }
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

        val messager = AzureMessager.getMessager()
        messager.info("Start creating Web App deployment slot ($name)...")

        val slot = withCreate.create()

        messager.success("Web App deployment slot ($name) is successfully created")

        return slot
    }

    override fun updateResourceInAzure(remote: DeploymentSlot): DeploymentSlot {
        throw AzureToolkitRuntimeException("Updating web app deployment slot is not supported")
    }

    var dotNetRuntime: DotNetRuntime?
        get() = config?.runtime ?: remote?.getDotNetRuntime()
        set(value) {
            ensureConfig().runtime = value
        }

    var configurationSource: String?
        get() = config?.configurationSource
        set(value) {
            ensureConfig().configurationSource = value
        }

    var dockerConfiguration: DockerConfiguration?
        get() = config?.dockerConfiguration
        set(value) {
            ensureConfig().dockerConfiguration = value
        }

    override fun getDiagnosticConfig() = config?.diagnosticConfig ?: super.getDiagnosticConfig()
    fun setDiagnosticConfig(value: DiagnosticConfig?) {
        ensureConfig().diagnosticConfig = value
    }

    data class Config(
        var runtime: DotNetRuntime? = null,
        var dockerConfiguration: DockerConfiguration? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var configurationSource: String? = null
    )
}