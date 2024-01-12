/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.rider.build.tasks.BuildProjectBeforeRunTask
import com.jetbrains.rider.run.configurations.DotNetConfigurationFactoryBase
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.function.buildTasks.BuildFunctionsProjectBeforeRunTaskProvider
import javax.swing.Icon

class FunctionRunConfigurationFactory(type: ConfigurationType) :
    DotNetConfigurationFactoryBase<FunctionRunConfiguration>(type) {
    companion object {
        private const val FACTORY_ID = "Azure - Run Function"
        private const val FACTORY_NAME = "Run Function"
    }

    override fun getId() = FACTORY_ID

    override fun getIcon(): Icon = IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.RUN)

    override fun getName() = FACTORY_NAME

    override fun createTemplateConfiguration(project: Project) =
        FunctionRunConfiguration(
            project,
            this,
            project.name,
            FunctionRunConfigurationParameters.createDefault(project)
        )

    override fun createConfiguration(name: String?, template: RunConfiguration) =
        FunctionRunConfiguration(
            template.project,
            this,
            name,
            FunctionRunConfigurationParameters.createDefault(template.project)
        )

    override fun configureBeforeRunTaskDefaults(
        providerID: Key<out BeforeRunTask<BeforeRunTask<*>>>?,
        task: BeforeRunTask<out BeforeRunTask<*>>?
    ) {
        // TODO: Switch to use BuildProjectBeforeRunTaskProvider that supports building custom project
        if (providerID == BuildFunctionsProjectBeforeRunTaskProvider.ID && task is BuildProjectBeforeRunTask) {
            task.isEnabled = true
        }
    }
}