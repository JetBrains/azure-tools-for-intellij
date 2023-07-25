package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.azuretools.telemetrywrapper.Operation
import org.apache.commons.lang3.StringUtils

class RiderWebAppRunState(project: Project, webAppConfiguration: RiderWebAppConfiguration)
    : AzureRunProfileState<WebAppBase<*, *, *>>(project) {

    private val webAppSettingModel: DotNetWebAppSettingModel = webAppConfiguration.webAppSettingModel

    override fun executeSteps(processHandler: RunProcessHandler, operation: Operation): WebAppBase<*, *, *> {
//        val publishableProject = webAppSettingModel.publishableProject
//                ?: throw RuntimeException("Project is not defined")
//        val artifact = prepareArtifact(project, publishableProject, webAppSettingModel.configuration, webAppSettingModel.platform, processHandler)
//        val deployTarget = getOrCreateDeployTargetFromAppSettingModel(processHandler)
//        AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(deployTarget, artifact, webAppSettingModel.isDeployToRoot, processHandler)
//
//        return deployTarget
        throw NotImplementedError()
    }

    override fun createOperation(): Operation {
        TODO("Not yet implemented")
    }

    override fun getTelemetryMap(): Map<String, String> {
        return emptyMap()
    }

    override fun onSuccess(result: WebAppBase<*, *, *>?, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()
    }

    private fun getOrCreateDeployTargetFromAppSettingModel(processHandler: RunProcessHandler): WebAppBase<*, *, *> {
        val azureAppService = Azure.az(AzureWebApp::class.java)
        val webApp = getOrCreateWebappFromAppSettingModel(azureAppService, processHandler)
        if (!isDeployToSlot()) return webApp

        return if (StringUtils.equals(webAppSettingModel.slotName, Constants.CREATE_NEW_SLOT)) {
            AzureWebAppMvpModel.getInstance().createDeploymentSlotFromSettingModel(webApp, webAppSettingModel)
        } else {
            webApp.slots().get(webAppSettingModel.slotName, webAppSettingModel.resourceGroup)
                    ?: throw NullPointerException("Failed to get deployment slot with name ${webAppSettingModel.slotName}")
        }
    }

    private fun getOrCreateWebappFromAppSettingModel(azureAppService: AzureAppService, processHandler: RunProcessHandler): WebApp {
        val name = webAppSettingModel.webAppName
        val rg = webAppSettingModel.resourceGroup
        val id = webAppSettingModel.webAppId
        val webapps = Azure.az(AzureWebApp::class.java).webApps(webAppSettingModel.subscriptionId)
        val webApp = if (StringUtils.isNotBlank(id)) webapps.get(id) else webapps.get(name, rg)
        if (webApp != null) return webApp

        if (webAppSettingModel.isCreatingNew) {
            processHandler.setText("Creating new web app...")
            return AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(webAppSettingModel)
        } else {
            processHandler.setText("Deployment failed!")
            throw Exception("Cannot get webapp for deploy.")
        }
    }

    private fun isDeployToSlot() = !webAppSettingModel.isCreatingNew && webAppSettingModel.isDeployToSlot
}