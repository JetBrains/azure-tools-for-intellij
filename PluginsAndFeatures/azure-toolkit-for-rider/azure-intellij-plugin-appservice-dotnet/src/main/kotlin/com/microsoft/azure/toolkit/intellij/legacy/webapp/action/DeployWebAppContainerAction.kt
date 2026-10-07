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
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer.WebAppContainerConfigurationType
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

class DeployWebAppContainerAction : AnAction() {
    companion object {
        private val configType =
            ConfigurationTypeUtil.findConfigurationType(WebAppContainerConfigurationType::class.java)

        private fun deploy(project: Project) {
            val settings = getOrCreateRunConfigurationSettings(project)
            runConfiguration(project, settings)
        }

        private fun getOrCreateRunConfigurationSettings(project: Project): RunnerAndConfigurationSettings {
            val manager = RunManagerEx.getInstanceEx(project)
            val runConfigurationName = "${configType.name}: ${project.name}"
            return manager.findConfigurationByName(runConfigurationName)
                    ?: manager.createConfiguration(runConfigurationName, configType)
        }

        private fun runConfiguration(project: Project, settings: RunnerAndConfigurationSettings) {
            val manager = RunManagerEx.getInstanceEx(project)
            AzureTaskManager.getInstance().runLater {
                if (RunDialog.editConfiguration(project, settings, "Deploy Image to Web App", DefaultRunExecutor.getRunExecutorInstance())) {
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