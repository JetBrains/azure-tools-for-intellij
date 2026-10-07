/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action

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
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppConfigurationType
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

class DeployWebAppAction : AnAction() {
    companion object {
        private val configType = ConfigurationTypeUtil.findConfigurationType(WebAppConfigurationType::class.java)

        fun deploy(webApp: WebApp?, project: Project?) {
            if (webApp == null || project == null) return
            val settings = getOrCreateRunConfigurationSettings(project, webApp)
            runConfiguration(project, settings)
        }

        private fun deploy(project: Project) {
            val settings = getOrCreateRunConfigurationSettings(project, null)
            runConfiguration(project, settings)
        }

        private fun getOrCreateRunConfigurationSettings(project: Project, webApp: WebApp?): RunnerAndConfigurationSettings {
            val manager = RunManagerEx.getInstanceEx(project)
            val name = webApp?.name ?: ""
            val type = configType
            val runConfigurationName = "${type.name}: ${project.name} $name"
            val settings = manager.findConfigurationByName(runConfigurationName)
                    ?: manager.createConfiguration(runConfigurationName, type)
            val runConfiguration = settings.configuration
            if (runConfiguration is WebAppConfiguration && webApp != null) {
                runConfiguration.setWebApp(webApp)
            }
            return settings
        }

        private fun runConfiguration(project: Project, settings: RunnerAndConfigurationSettings) {
            val manager = RunManagerEx.getInstanceEx(project)
            AzureTaskManager.getInstance().runLater {
                if (RunDialog.editConfiguration(project, settings, "Deploy To Web App", DefaultRunExecutor.getRunExecutorInstance())) {
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