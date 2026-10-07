/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.azure.core.http.HttpHeaderName
import com.azure.core.http.HttpMethod
import com.azure.core.http.HttpRequest
import com.azure.core.management.serializer.SerializerFactory
import com.azure.core.util.serializer.SerializerEncoding
import com.azure.resourcemanager.appservice.fluent.models.SiteConfigInner
import com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages.*
import com.azure.resourcemanager.appservice.models.NameValuePair
import com.fasterxml.jackson.databind.node.ObjectNode
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.getDotNetRuntime
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppModule
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.StorageAuthenticationMethod
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.storage.StorageAccount

class DotNetFunctionAppDraft : FunctionApp,
    AzResource.Draft<FunctionApp, com.azure.resourcemanager.appservice.models.FunctionApp> {

    constructor(name: String, resourceGroupName: String, module: FunctionAppModule) :
            super(name, resourceGroupName, module) {
        origin = null
    }

    constructor(origin: FunctionApp) : super(origin) {
        this.origin = origin
    }

    private val origin: FunctionApp?

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

    override fun createResourceInAzure(): com.azure.resourcemanager.appservice.models.FunctionApp {
        val newRuntime = checkNotNull(dotNetRuntime) { "'runtime' is required to create a Function App" }
        val newPlan = checkNotNull(appServicePlan) { "'service plan' is required to create a Function App" }
        val os = newRuntime.operatingSystem
        if (os != newPlan.operatingSystem && newPlan.operatingSystem != OperatingSystem.DOCKER) {
            throw AzureToolkitRuntimeException("Could not create $os app service in ${newPlan.operatingSystem} service plan")
        }
        val newAppSettings = appSettings
        val newDiagnosticConfig = diagnosticConfig
        val newStorageAccount = storageAccount

        val isFlexConsumption = newPlan.pricingTier.isFlexConsumption

        val manager = checkNotNull(parent.remote)
        val blank = manager.functionApps().define(name)
        val withCreate =
            if (!newRuntime.isDocker) createFunctionApp(blank, os, newPlan, newRuntime)
            else createDockerFunctionApp(blank, newPlan)

        if (!newAppSettings.isNullOrEmpty())
            withCreate.withAppSettings(newAppSettings)
        if (newStorageAccount != null)
            withCreate.withExistingStorageAccount(newStorageAccount.remote)
        if (newDiagnosticConfig != null)
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig)

        val messager = AzureMessager.getMessager()
        messager.info(AzureString.format("Start creating Function App ({0})...", name))

        val functionApp = if (!isFlexConsumption) {
            withCreate.create()
        } else {
            createOrUpdateFlexConsumptionFunctionAppWithRawRequest(withCreate as com.azure.resourcemanager.appservice.models.FunctionApp)
        }

        messager.success(AzureString.format("Function App ({0}) is successfully created", name))

        return functionApp
    }

    private fun createFunctionApp(
        blank: Blank,
        os: OperatingSystem,
        plan: AppServicePlan,
        runtime: DotNetRuntime
    ): WithCreate = when (os) {
        OperatingSystem.LINUX -> {
            val functionStack = requireNotNull(runtime.functionStack) { "Unable to configure function runtime" }
            blank
                .withExistingLinuxAppServicePlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withBuiltInImage(functionStack)
        }

        OperatingSystem.WINDOWS -> {
            val functionStack = requireNotNull(runtime.functionStack) { "Unable to configure function runtime" }
            blank
                .withExistingAppServicePlan(plan.remote)
                .withExistingResourceGroup(resourceGroupName)
                .withRuntime(functionStack.runtime())
                .withRuntimeVersion(functionStack.version())
        }

        OperatingSystem.DOCKER -> throw AzureToolkitRuntimeException("Unsupported operating system $os")
    }

    private fun createDockerFunctionApp(
        blank: Blank,
        plan: AppServicePlan
    ): WithCreate {
        val dockerConfig =
            checkNotNull(dockerConfiguration) { "Docker configuration is required to create a docker based Azure Function App" }

        val withImage = blank
            .withExistingLinuxAppServicePlan(plan.remote)
            .withExistingResourceGroup(resourceGroupName)

        val draft =
            if (dockerConfig.userName.isNullOrEmpty() && dockerConfig.password.isNullOrEmpty())
                withImage.withPublicDockerHubImage(dockerConfig.image)
            else if (dockerConfig.registryUrl.isNullOrEmpty())
                withImage.withPrivateDockerHubImage(dockerConfig.image)
                    .withCredentials(dockerConfig.userName, dockerConfig.password)
            else
                withImage.withPrivateRegistryImage(dockerConfig.image, dockerConfig.registryUrl)
                    .withCredentials(dockerConfig.userName, dockerConfig.password)

        return draft
    }

    override fun updateResourceInAzure(remote: com.azure.resourcemanager.appservice.models.FunctionApp): com.azure.resourcemanager.appservice.models.FunctionApp {
        throw AzureToolkitRuntimeException("Updating function app is not supported")
    }

    private fun createOrUpdateFlexConsumptionFunctionAppWithRawRequest(functionApp: com.azure.resourcemanager.appservice.models.FunctionApp): com.azure.resourcemanager.appservice.models.FunctionApp {
        val flexConfig = flexConsumptionAppConfig
        updateSiteConfigurations(functionApp, flexConfig)

        val adapter = SerializerFactory.createDefaultManagementSerializerAdapter()
        val siteInner = functionApp.innerModel()
        val originContent = adapter.serializeRaw(siteInner)
        val jsonNode = adapter.deserialize<ObjectNode>(
            originContent,
            ObjectNode::class.java,
            SerializerEncoding.JSON
        )

        val configNode = adapter.deserialize<ObjectNode>(
            adapter.serializeRaw(flexConfig),
            ObjectNode::class.java,
            SerializerEncoding.JSON
        )
        val properties = jsonNode.get("properties") as ObjectNode
        properties.set<ObjectNode>("functionAppConfig", configNode)
        appServicePlan?.pricingTier?.let { properties.put("sku", it.tier) }

        val newContent = adapter.serializeRaw(jsonNode)
        val httpPipeline = functionApp.manager().httpPipeline()
        val targetUrl = getRawRequestEndpoint(functionApp)
        val request = HttpRequest(HttpMethod.PUT, targetUrl)
            .setHeader(HttpHeaderName.CONTENT_TYPE, "application/json")
            .setBody(newContent)

        try {
            val response = httpPipeline.send(request).block()
            if (response == null || response.statusCode >= 300 || response.statusCode < 200) {
                val content = if (response != null) response.bodyAsString.block() else ""
                throw AzureToolkitRuntimeException("Failed to create or update function app : $content")
            }

            val result = functionApp.manager()
                .functionApps()
                .getByResourceGroup(functionApp.resourceGroupName(), functionApp.name())

            result.refresh()

            return result
        } catch (e: Exception) {
            throw AzureToolkitRuntimeException(e)
        }
    }

    override fun getFlexConsumptionAppConfig(): FunctionAppConfig? {
        val configStorage = FunctionAppConfig.Storage().apply {
            authentication = FunctionAppConfig.Storage.Authentication.DEFAULT_AUTHENTICATION
            value = deploymentContainerUrl
        }
        val configDeployment = FunctionAppConfig.FunctionsDeployment().apply {
            storage = configStorage
        }

        val configRuntime = FunctionAppConfig.FunctionsRuntime().apply {
            name = "dotnet-isolated"
            version = requireNotNull(dotNetRuntime?.dotnetVersion)
        }

        val flexConfiguration = requireNotNull(flexConsumptionConfiguration)
        val configConcurrency = FunctionAppConfig.FunctionScaleAndConcurrency().apply {
            instanceMemoryMB = flexConfiguration.instanceSize
            maximumInstanceCount = 100
        }

        return FunctionAppConfig().apply {
            deployment = configDeployment
            runtime = configRuntime
            scaleAndConcurrency = configConcurrency
        }
    }

    private fun updateSiteConfigurations(
        functionApp: com.azure.resourcemanager.appservice.models.FunctionApp,
        flexConfig: FunctionAppConfig?
    ) {
        val settings = buildMap {
            getAppSettings()?.forEach { put(it.key, it.value) }

            storageAccount?.let { put("AzureWebJobsStorage", it.connectionString) }

            val authentication = flexConfig?.deployment?.storage?.authentication
            if (authentication?.type == StorageAuthenticationMethod.StorageAccountConnectionString) {
                deploymentAccount?.let { put(authentication.storageAccountConnectionStringName, it.connectionString) }
            }

            remove("FUNCTIONS_EXTENSION_VERSION")
            remove("FUNCTIONS_WORKER_RUNTIME")
            remove("FUNCTIONS_WORKER_RUNTIME_VERSION")
            remove("FUNCTIONS_MAX_HTTP_CONCURRENCY")
            remove("FUNCTIONS_WORKER_PROCESS_COUNT")
            remove("FUNCTIONS_WORKER_DYNAMIC_CONCURRENCY_ENABLED")
            remove("WEBSITE_CONTENTAZUREFILECONNECTIONSTRING")
            remove("WEBSITE_CONTENTSHARE")
        }.map { NameValuePair().withName(it.key).withValue(it.value) }

        val siteConfigInner = SiteConfigInner().apply {
            withAppSettings(settings)
            withHttp20Enabled(false)
            withFtpsState(null)
            withUse32BitWorkerProcess(null)
            withWindowsFxVersion(null)
            withLinuxFxVersion(null)
            withAlwaysOn(null)
            withPreWarmedInstanceCount(null)
            withFunctionAppScaleLimit(null)
            withJavaVersion(null)
        }

        functionApp.innerModel().apply {
            withSiteConfig(siteConfigInner)
            withHttpsOnly(false)
            withIsXenon(null)
            withContainerSize(null)
            withReserved(null)
        }
    }

    var dotNetRuntime: DotNetRuntime?
        get() = config?.runtime ?: remote?.getDotNetRuntime()
        set(value) {
            ensureConfig().runtime = value
        }
    var storageAccount: StorageAccount?
        get() = config?.storageAccount
        set(value) {
            ensureConfig().storageAccount = value
        }
    var dockerConfiguration: DockerConfiguration?
        get() = config?.dockerConfiguration
        set(value) {
            ensureConfig().dockerConfiguration = value
        }
    var deploymentAccount: StorageAccount?
        get() = config?.deploymentAccount
        set(value) {
            ensureConfig().deploymentAccount = value
        }
    var deploymentContainerUrl: String?
        get() = config?.deploymentContainerUrl
        set(value) {
            ensureConfig().deploymentContainerUrl = value
        }

    override fun getAppServicePlan() =
        config?.plan ?: super.getAppServicePlan()

    fun setAppServicePlan(value: AppServicePlan?) {
        ensureConfig().plan = value
    }

    override fun getAppSettings() =
        config?.appSettings ?: super.getAppSettings()

    fun setAppSettings(value: Map<String, String>?) {
        ensureConfig().appSettings = value
    }

    override fun getDiagnosticConfig() =
        config?.diagnosticConfig ?: super.getDiagnosticConfig()

    fun setDiagnosticConfig(value: DiagnosticConfig?) {
        ensureConfig().diagnosticConfig = value
    }

    override fun getFlexConsumptionConfiguration() =
        config?.flexConsumptionConfiguration ?: super.getFlexConsumptionConfiguration()

    fun setFlexConsumptionConfiguration(value: FlexConsumptionConfiguration?) {
        ensureConfig().flexConsumptionConfiguration = value
    }

    data class Config(
        var runtime: DotNetRuntime? = null,
        var plan: AppServicePlan? = null,
        var storageAccount: StorageAccount? = null,
        var enableDistributedTracing: Boolean? = null,
        var dockerConfiguration: DockerConfiguration? = null,
        var diagnosticConfig: DiagnosticConfig? = null,
        var flexConsumptionConfiguration: FlexConsumptionConfiguration? = null,
        var deploymentAccount: StorageAccount? = null,
        var deploymentContainerUrl: String? = null,
        var appSettings: Map<String, String>? = null
    )
}