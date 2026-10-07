/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.actions

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionDeploymentConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionDeploymentConfigurationType
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

class DeployFunctionAppAction : AnAction() {
    companion object {
        private val configType =
            ConfigurationTypeUtil.findConfigurationType(FunctionDeploymentConfigurationType::class.java)

        fun deploy(webApp: FunctionApp?, project: Project?) {
            if (webApp == null || project == null) return
            val settings = getOrCreateRunConfigurationSettings(project, webApp)
            runConfiguration(project, settings)
        }

        private fun deploy(project: Project) {
            val settings = getOrCreateRunConfigurationSettings(project, null)
            runConfiguration(project, settings)
        }

        private fun getOrCreateRunConfigurationSettings(
            project: Project,
            functionApp: FunctionApp?
        ): RunnerAndConfigurationSettings {
            val manager = RunManagerEx.getInstanceEx(project)
            val name = functionApp?.name ?: ""
            val type = configType
            val runConfigurationName = "${type.name}: ${project.name} $name"
            val settings = manager.findConfigurationByName(runConfigurationName)
                ?: manager.createConfiguration(runConfigurationName, type)
            val runConfiguration = settings.configuration
            if (runConfiguration is FunctionDeploymentConfiguration && functionApp != null) {
                runConfiguration.setFunctionApp(functionApp)
            }
            return settings
        }

        private fun runConfiguration(project: Project, settings: RunnerAndConfigurationSettings) {
            val manager = RunManagerEx.getInstanceEx(project)
            AzureTaskManager.getInstance().runLater {
                if (RunDialog.editConfiguration(project, settings, "Deploy To Function App")) {
                    settings.storeInLocalWorkspace()
                    manager.addConfiguration(settings)
                    manager.selectedConfiguration = settings
                    ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
                }
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        AzureTaskManager.getInstance().runLater {
            AzureLoginHelper.requireSignedIn(project) { deploy(project) }
        }
    }
}