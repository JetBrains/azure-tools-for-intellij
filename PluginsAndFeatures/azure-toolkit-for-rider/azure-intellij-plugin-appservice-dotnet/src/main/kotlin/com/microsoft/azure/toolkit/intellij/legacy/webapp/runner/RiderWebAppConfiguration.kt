package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration
import com.microsoft.azure.toolkit.intellij.connector.IConnectionAware
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase

class RiderWebAppConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
    RiderAzureRunConfigurationBase<DotNetWebAppSettingModel>(project, factory, name), IWebAppRunConfiguration,
    IConnectionAware {

    val webAppSettingModel = DotNetWebAppSettingModel()

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState {
        return RiderWebAppRunState(project, this)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        RiderWebAppSettingEditor(project, this)

    override fun setApplicationSettings(env: Map<String, String>) {
        webAppSettingModel.appSettings = env
        //FunctionUtils.saveAppSettingsToSecurityStorage()
    }

    override fun getApplicationSettings(): Map<String, String> = webAppSettingModel.appSettings

    override fun getModule(): Module? = null
}