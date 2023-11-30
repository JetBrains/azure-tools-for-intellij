/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.microsoft.azure.toolkit.intellij.appservice.webapp.CreateOrUpdateDotNetWebAppTask
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetAppServiceConfig
import com.microsoft.azure.toolkit.intellij.appservice.webapp.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunProfileState
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.Constants
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppArtifactService
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.*
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask
import com.microsoft.azure.toolkit.lib.appservice.webapp.*
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import org.apache.commons.lang3.StringUtils
import java.awt.Desktop
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL

class WebAppRunState(project: Project, private val webAppConfiguration: WebAppConfiguration) :
    RiderAzureRunProfileState<WebAppBase<*, *, *>>(project) {

    private val publishModel: WebAppPublishModel = webAppConfiguration.getModel()

    override fun executeSteps(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        OperationContext.current().setMessager(processHandlerMessenger)

        val publishableProject = publishModel.publishableProject
            ?: throw RuntimeException("Project is not defined")
        val zipFile = WebAppArtifactService.getInstance(project)
            .prepareArtifact(
                publishableProject,
                publishModel.projectConfiguration,
                publishModel.projectPlatform,
                processHandler
            )

        val config = createDotNetAppServiceConfig()
        val createTask = CreateOrUpdateDotNetWebAppTask(config)
        val deployTarget = createTask.execute()
//        updateApplicationSettings(deployTarget, processHandler)

        val artifact = WebAppArtifact.builder()
            .file(zipFile)
            .deployType(DeployType.ZIP)
            .build()

        val deployTask = DeployWebAppTask(deployTarget, listOf(artifact), true, false, false)
        deployTask.execute()

        FileUtil.delete(zipFile)

        return deployTarget
    }

    private fun createDotNetAppServiceConfig(): DotNetAppServiceConfig {
        return DotNetAppServiceConfig().apply {
            subscriptionId(publishModel.subscriptionId)
            resourceGroup(publishModel.resourceGroup)
            region(Region.fromName(publishModel.region))
            servicePlanName(publishModel.appServicePlanName)
            servicePlanResourceGroup(publishModel.appServicePlanResourceGroupName)
            pricingTier(PricingTier.fromString(publishModel.pricing))
            appName(publishModel.webAppName)
            runtime(createRuntimeConfig())
            dotnetRuntime = createDotNetRuntimeConfig()
            appSettings(publishModel.appSettings)
        }
    }

    private fun createRuntimeConfig() = RuntimeConfig().apply {
        os(OperatingSystem.fromString(publishModel.operatingSystem))
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
    }

    private fun createDotNetRuntimeConfig() = DotNetRuntimeConfig().apply {
        val operatingSystem = OperatingSystem.fromString(publishModel.operatingSystem)
        os(operatingSystem)
        javaVersion(JavaVersion.OFF)
        webContainer(WebContainer.JAVA_OFF)
        isDocker = false
        val stackAndVersion = publishModel.publishableProject?.getStackAndVersion(project, operatingSystem)
        stack = stackAndVersion?.first
        frameworkVersion = stackAndVersion?.second
    }

    private fun getOrCreateDeployTargetFromAppSettingModel(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        val webApp = getOrCreateWebappFromAppSettingModel(processHandler)
        if (!isDeployToSlot()) return webApp

        return if (StringUtils.equals(publishModel.slotName, Constants.CREATE_NEW_SLOT)) {
            AzureWebAppMvpModel.getInstance().createDeploymentSlotFromSettingModel(webApp, publishModel)
        } else {
            webApp.slots().get(publishModel.slotName, publishModel.resourceGroup)
                ?: throw NullPointerException("Failed to get deployment slot with name ${publishModel.slotName}")
        }
    }

    private fun getOrCreateWebappFromAppSettingModel(processHandler: RunProcessHandler): WebApp {
        val name = publishModel.webAppName
        val rg = publishModel.resourceGroup
        val id = publishModel.webAppId
        val webapps = Azure.az(AzureWebApp::class.java).webApps(publishModel.subscriptionId)
        val webApp = if (StringUtils.isNotBlank(id)) webapps.get(id) else webapps.get(name, rg)
        if (webApp != null) return webApp

        if (publishModel.isCreatingNew) {
            processHandler.setText("Creating new web app...")
            return AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(publishModel)
        } else {
            processHandler.setText("Deployment failed!")
            throw Exception("Cannot get webapp for deploy.")
        }
    }

    private fun isDeployToSlot() = !publishModel.isCreatingNew && publishModel.isDeployToSlot

    private fun updateApplicationSettings(deployTarget: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        val applicationSettings = webAppConfiguration.applicationSettings.toMutableMap()
        val appSettingsToRemove =
            if (webAppConfiguration.isCreatingNew) emptySet()
            else getAppSettingsToRemove(deployTarget, applicationSettings)
        if (applicationSettings.isEmpty()) return

        if (deployTarget is WebApp) {
            processHandler.setText("Updating application settings...")
            val draft = deployTarget.update() as? WebAppDraft ?: return
            appSettingsToRemove.forEach { draft.removeAppSetting(it) }
            draft.appSettings = applicationSettings
            draft.updateIfExist()
            processHandler.setText("Update application settings successfully.")
        } else if (deployTarget is WebAppDeploymentSlot) {
            processHandler.setText("Updating deployment slot application settings...")
            val draft = deployTarget.update() as? WebAppDeploymentSlotDraft ?: return
            appSettingsToRemove.forEach { draft.removeAppSetting(it) }
            draft.appSettings = applicationSettings
            draft.updateIfExist()
            processHandler.setText("Update deployment slot application settings successfully.")
        }
    }

    private fun getAppSettingsToRemove(target: WebAppBase<*, *, *>, applicationSettings: Map<String, String>) =
        target.appSettings
            ?.keys
            ?.asSequence()
            ?.filter { !applicationSettings.containsKey(it) }
            ?.toSet()
            ?: emptySet()

    override fun onSuccess(result: WebAppBase<*, *, *>, processHandler: RunProcessHandler) {
        updateConfigurationDataModel(result)
        processHandler.setText("Deployment was successful, but the app may still be starting.")
        val url = "https://${result.hostName}"
        processHandler.setText("URL: $url")
        if (publishModel.isOpenBrowserAfterDeployment) {
            openWebAppInBrowser(url, processHandler)
        }
        processHandler.notifyComplete()
    }

    private fun updateConfigurationDataModel(app: WebAppBase<*, *, *>) {
        webAppConfiguration.isCreatingNew = false
        if (app is WebAppDeploymentSlot) {
            webAppConfiguration.slotName = app.name
            webAppConfiguration.newSlotConfigurationSource = "Don't clone configuration from an existing slot"
            webAppConfiguration.newSlotName = ""
            webAppConfiguration.webAppId = app.parent.id
        } else {
            webAppConfiguration.webAppId = app.id
        }
        webAppConfiguration.applicationSettings = app.appSettings ?: emptyMap()
        webAppConfiguration.webAppName = app.name
        webAppConfiguration.resourceGroup = ""
        webAppConfiguration.appServicePlanName = ""
        webAppConfiguration.appServicePlanResourceGroupName = ""
    }

    private fun openWebAppInBrowser(url: String, processHandler: RunProcessHandler) {
        try {
            Desktop.getDesktop().browse(URL(url).toURI())
        } catch (ex: Exception) {
            when (ex) {
                is IOException,
                is URISyntaxException -> processHandler.println(ex.message, ProcessOutputTypes.STDERR)

                else -> throw ex
            }
        }
    }
}