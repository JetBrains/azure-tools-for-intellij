package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import org.jdom.Element


class RiderWebAppConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    RiderAzureRunConfigurationBase<DotNetWebAppSettingModel>(project, factory, name), IWebAppRunConfiguration,
    IConnectionAware {

    val webAppSettingModel = DotNetWebAppSettingModel()

    var webAppId: String?
        get() = webAppSettingModel.webAppId
        set(value) {
            webAppSettingModel.webAppId = value
        }
    var webAppName: String
        get() = webAppSettingModel.webAppName
        set(value) {
            webAppSettingModel.webAppName = value
        }
    var subscriptionId: String
        get() = webAppSettingModel.subscriptionId
        set(value) {
            webAppSettingModel.subscriptionId = value
        }
    var region: String
        get() = webAppSettingModel.region
        set(value) {
            webAppSettingModel.region = value
        }
    var resourceGroup: String
        get() = webAppSettingModel.resourceGroup
        set(value) {
            webAppSettingModel.resourceGroup = value
        }
    var pricing: String
        get() = webAppSettingModel.pricing
        set(value) {
            webAppSettingModel.pricing = value
        }
    val operatingSystem: OperatingSystem?
        get() = OperatingSystem.fromString(webAppSettingModel.operatingSystem)
    var appServicePlanName: String?
        get() = webAppSettingModel.appServicePlanName
        set(value) {
            webAppSettingModel.appServicePlanName = value
        }
    var appServicePlanResourceGroupName: String?
        get() = webAppSettingModel.appServicePlanResourceGroupName
        set(value) {
            webAppSettingModel.appServicePlanResourceGroupName = value
        }
    var isCreatingAppServicePlan: Boolean
        get() = webAppSettingModel.isCreatingAppServicePlan
        set(value) {
            webAppSettingModel.isCreatingAppServicePlan = value
        }
    var isCreatingNew: Boolean
        get() = webAppSettingModel.isCreatingNew
        set(value) {
            webAppSettingModel.isCreatingNew = value
        }
    val artifactIdentifier: String
        get() = webAppSettingModel.artifactIdentifier
    var isOpenBrowserAfterDeployment: Boolean
        get() = webAppSettingModel.isOpenBrowserAfterDeployment
        set(value) {
            webAppSettingModel.isOpenBrowserAfterDeployment = value
        }
    var isSlotPanelVisible: Boolean
        get() = webAppSettingModel.slotPanelVisible
        set(value) {
            webAppSettingModel.slotPanelVisible = value
        }
    var appSettingsKey: String
        get() = webAppSettingModel.appSettingsKey
        set(value) {
            webAppSettingModel.appSettingsKey = value
        }

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState {
        return RiderWebAppRunState(project, this)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        RiderWebAppSettingEditor(project, this)

    override fun getModel() = webAppSettingModel

    override fun setApplicationSettings(env: Map<String, String>) {
        webAppSettingModel.appSettings = env
    }

    override fun getApplicationSettings(): Map<String, String> = webAppSettingModel.appSettings

    override fun getModule(): Module? = null

    fun saveRuntime(runtime: Runtime?) {
        webAppSettingModel.saveRuntime(runtime)
    }

    fun saveProject(project: PublishableProjectModel) {
        webAppSettingModel.publishableProject = project
        webAppSettingModel.artifactIdentifier = project.projectModelId.toString()
    }

    override fun readExternal(element: Element) {
        super<RiderAzureRunConfigurationBase>.readExternal(element)
    }
}