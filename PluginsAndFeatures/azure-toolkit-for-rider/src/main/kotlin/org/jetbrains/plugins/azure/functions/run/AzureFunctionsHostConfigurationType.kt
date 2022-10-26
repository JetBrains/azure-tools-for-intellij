/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.run.AutoGeneratedRunConfigurationManager
import com.jetbrains.rider.run.configurations.IRunConfigurationWithDefault
import com.jetbrains.rider.run.configurations.IRunnableProjectConfigurationType
import com.jetbrains.rider.run.configurations.RunConfigurationHelper.hasConfigurationForNameAndTypeId
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.daemon.AzureRunnableProjectKinds

class AzureFunctionsHostConfigurationType : ConfigurationTypeBase(
        id = "AzureFunctionsHost",
        displayName = message("run_config.run_function_app.form.function_app.type_name"),
        description = message("run_config.run_function_app.form.function_app.type_description"),
        icon = CommonIcons.AzureFunctions.FunctionAppRunConfiguration
), IRunnableProjectConfigurationType, IRunConfigurationWithDefault {

    companion object {
        val instance get() = runConfigurationType<AzureFunctionsHostConfigurationType>()

        fun isTypeApplicable(kind: RunnableProjectKind): Boolean =
                kind == AzureRunnableProjectKinds.AzureFunctions
    }

    val factory: AzureFunctionsHostConfigurationFactory = AzureFunctionsHostConfigurationFactory(this)

    init {
        addFactory(factory)
    }

    override fun isApplicable(kind: RunnableProjectKind) = isTypeApplicable(kind)

    override fun tryCreateDefault(
            project: Project,
            lifetime: Lifetime,
            projects: List<RunnableProject>,
            autoGeneratedRunConfigurationManager: AutoGeneratedRunConfigurationManager,
            runManager: RunManager
    ): List<Pair<RunnableProject, RunnerAndConfigurationSettings>> {

        val defaultSettingsList = mutableListOf<Pair<RunnableProject, RunnerAndConfigurationSettings>>()

        val applicableProjects = projects.filter {
            isApplicable(it.kind)
                    // Don't create "default" config if launchSettings.json is available for any project
                    && !(it.kind == RunnableProjectKinds.LaunchSettings && LaunchSettingsJsonService.getLaunchSettingsFileForProject(it)?.exists() == true)
                    && (!runManager.hasConfigurationForNameAndTypeId(it.name, this.id)
                        || !autoGeneratedRunConfigurationManager.hasRunConfigurationEverBeenGenerated(it.projectFilePath))
        }

        applicableProjects.forEach { applicableProject ->
            val defaultSettings =
                    runManager.createConfiguration(name = applicableProject.name, factory = factory)

            val configuration = defaultSettings.configuration as AzureFunctionsHostConfiguration
            configuration.parameters.projectFilePath = applicableProject.projectFilePath

            runManager.addConfiguration(defaultSettings)
            defaultSettingsList.add(Pair(applicableProject, defaultSettings))

            autoGeneratedRunConfigurationManager.markProjectAsAutoGenerated(applicableProject.projectFilePath)
        }

        return defaultSettingsList.toList()
    }
}