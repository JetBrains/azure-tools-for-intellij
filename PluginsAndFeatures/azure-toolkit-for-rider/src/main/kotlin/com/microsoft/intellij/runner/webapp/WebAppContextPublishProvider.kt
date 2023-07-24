///**
// * Copyright (c) 2018-2021 JetBrains s.r.o.
// *
// * All rights reserved.
// *
// * MIT License
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// *
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package com.microsoft.intellij.runner.webapp
//
//import com.intellij.execution.configurations.ConfigurationFactory
//import com.intellij.execution.configurations.ConfigurationTypeUtil
//import com.intellij.execution.configurations.RunConfiguration
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.util.SystemInfo
//import com.jetbrains.rider.model.PublishableProjectModel
//import com.jetbrains.rider.model.publishableProjectsModel
//import com.jetbrains.rider.projectView.solution
//import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
//import com.jetbrains.rider.run.configurations.publishing.RiderContextPublishProvider
//import com.microsoft.intellij.runner.webapp.config.RiderWebAppConfiguration
//import com.microsoft.intellij.runner.webapp.config.RiderWebAppConfigurationType
//import icons.CommonIcons
//import org.jetbrains.plugins.azure.RiderAzureBundle.message
//import javax.swing.Icon
//
//class WebAppContextPublishProvider : RiderContextPublishProvider {
//
//    companion object {
//        private const val RUN_CONFIG_PROJECT_NAME = "Publish %s to Azure"
//    }
//
//    override val icon: Icon
//        get() = CommonIcons.Azure
//
//    override val name: String
//        get() = message("context_menu.publish.web_app.publish_to_azure")
//
//    override fun getConfigurationForNode(project: Project,
//                                         entity: ProjectModelEntity): Pair<RunConfiguration, ConfigurationFactory> {
//
//        val projectData = RiderContextPublishProvider.getProjectDataRecursive(project, entity)
//                ?: error("Unexpected project node type. Cannot get project data for entity ${entity.url}")
//
//        val factory = ConfigurationTypeUtil.findConfigurationType(RiderWebAppConfigurationType::class.java).configurationFactories.single()
//        val configuration = RiderWebAppConfiguration(project, factory, String.format(RUN_CONFIG_PROJECT_NAME, projectData.value.projectName))
//
//        configuration.model.webAppModel.publishableProject = projectData.value
//
//        return Pair(configuration, factory)
//    }
//
//    private fun isProjectPublishable(projectData: PublishableProjectModel?) =
//            projectData != null && (projectData.isWeb && (projectData.isDotNetCore || SystemInfo.isWindows))
//
//    override fun isAvailable(project: Project, entity: ProjectModelEntity): Boolean {
//        val projectData = RiderContextPublishProvider.getProjectData(project, entity)
//        return isProjectPublishable(projectData?.value)
//    }
//
//    override fun solutionHasAnyPublishableProjects(project: Project): Boolean {
//        return project.solution.publishableProjectsModel.publishableProjects.entries.any { isProjectPublishable(it.value) }
//    }
//}