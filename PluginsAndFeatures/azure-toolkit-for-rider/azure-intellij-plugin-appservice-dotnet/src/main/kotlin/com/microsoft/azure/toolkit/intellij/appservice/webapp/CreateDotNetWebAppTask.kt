/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.intellij.appservice.CreateAppServiceTask
import com.microsoft.azure.toolkit.intellij.appservice.servicePlan.CreateServicePlanTask
import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.webapp.*
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.operation.Operation
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask
import java.util.concurrent.Callable

class CreateDotNetWebAppTask(
    private val config: DotNetAppServiceConfig,
    processHandlerMessager: RiderRunProcessHandlerMessager?
) : CreateAppServiceTask<WebAppBase<*, *, *>>(processHandlerMessager) {

    private var appServicePlan: AppServicePlan? = null
    private var webApp: WebAppBase<*, *, *>? = null

    init {
        val webApp = Azure.az(AzureWebApp::class.java)
            .webApps(config.subscriptionId())
            .getOrDraft(config.appName(), config.resourceGroup())

        if (webApp.isDraftForCreating) {
            registerSubTask(createResourceGroupTask()) {}
            registerSubTask(createServicePlanTask()) { appServicePlan = it }

            val webAppDraft = (webApp as? WebAppDraft)?.toDotNetWebAppDraft()
                ?: error("Unable to get web app draft")
            registerSubTask(createWebAppTask(webAppDraft)) { this@CreateDotNetWebAppTask.webApp = it }
        } else if (!config.deploymentSlotName().isNullOrEmpty()) {
            val slot = webApp
                .slots()
                .getOrDraft(config.deploymentSlotName(), config.resourceGroup())
            if (slot.isDraftForCreating) {
                val slotDraft = (slot as? WebAppDeploymentSlotDraft)?.toDotNetWebAppDeploymentSlotDraft()
                    ?: error("Unable to get web app deployment slot draft")
                registerSubTask(createWebAppSlotTask(slotDraft)) { this@CreateDotNetWebAppTask.webApp = it }
            } else {
                this@CreateDotNetWebAppTask.webApp = slot
            }
        } else {
            this@CreateDotNetWebAppTask.webApp = webApp
        }
    }

    private fun WebAppDraft.toDotNetWebAppDraft(): DotNetWebAppDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetWebAppDraft(draftOrigin)
            else DotNetWebAppDraft(name, resourceGroupName, module as WebAppModule)

        return draft
    }

    private fun WebAppDeploymentSlotDraft.toDotNetWebAppDeploymentSlotDraft(): DotNetWebAppDeploymentSlotDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetWebAppDeploymentSlotDraft(draftOrigin)
            else DotNetWebAppDeploymentSlotDraft(name, module as WebAppDeploymentSlotModule)

        return draft
    }

    private fun createResourceGroupTask(): AzureTask<ResourceGroup> =
        CreateResourceGroupTask(config.subscriptionId(), config.resourceGroup(), config.region())

    private fun createServicePlanTask(): AzureTask<AppServicePlan> =
        CreateServicePlanTask(AppServiceConfig.getServicePlanConfig(config))

    private fun createWebAppTask(draft: DotNetWebAppDraft) =
        AzureTask(
            "Create new app(${config.appName()}) on subscription(${config.subscriptionId()})",
            Callable {
                with(draft) {
                    appServicePlan = this@CreateDotNetWebAppTask.appServicePlan
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()

                    createIfNotExist()
                }
            }
        )

    private fun createWebAppSlotTask(draft: DotNetWebAppDeploymentSlotDraft) =
        AzureTask(
            "Create new slot(${config.deploymentSlotName()}) on web app (${config.appName()})",
            Callable {
                with(draft) {
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    configurationSource = config.deploymentSlotConfigurationSource()

                    createIfNotExist()
                }
            }
        )

    override fun doExecute(): WebAppBase<*, *, *> {
        Operation.execute(AzureString.fromString("Creating or updating Web App"), {
            executeSubTasks()
        }, null)

        return requireNotNull(webApp)
    }
}