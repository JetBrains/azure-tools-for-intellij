/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.actions

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunDialog
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.jetbrains.rd.util.threading.coroutines.launch
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionDeploymentConfigurationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CreateFunctionAppAction {
    fun openDialog(project: Project) {
        project.lifetime.launch {
            val runManager = RunManager.getInstance(project)
            val configurationType =
                ConfigurationTypeUtil.findConfigurationType(FunctionDeploymentConfigurationType::class.java)
            val configurationFactory = configurationType.configurationFactories.first()
            val uniqueName = runManager.suggestUniqueName(project.name, configurationType)
            val settings = runManager.createConfiguration(uniqueName, configurationFactory)
            val result = withContext(Dispatchers.EDT) {
                RunDialog.editConfiguration(project, settings, "Create a Function App")
            }
            if (!result) return@launch

            settings.storeInLocalWorkspace()
            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings

            ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}