/**
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.getSelectedProject

class AzureFunctionsHostRunConfigurationProducer
    : LazyRunConfigurationProducer<AzureFunctionsHostConfiguration>() {

    override fun getConfigurationFactory() =
            ConfigurationTypeUtil.findConfigurationType(AzureFunctionsHostConfigurationType::class.java)
                    .configurationFactories
                    .single()

    override fun isConfigurationFromContext(
            configuration: AzureFunctionsHostConfiguration,
            context: ConfigurationContext
    ) : Boolean {
        val project = context.getSelectedProject() ?: return false
        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false

        val selectedProjectFilePath = FileUtil.toSystemIndependentName(project.getFile()?.path ?: "")
        val runnableProject = projects.firstOrNull {
            it.kind == RunnableProjectKind.AzureFunctions &&
            it.projectFilePath == selectedProjectFilePath &&
            configuration.parameters.projectFilePath == selectedProjectFilePath
        }

        return runnableProject != null
    }

    override fun setupConfigurationFromContext(
            configuration: AzureFunctionsHostConfiguration,
            context: ConfigurationContext,
            ref: Ref<PsiElement>
    ): Boolean {
        val project = context.getSelectedProject() ?: return false
        val projects = context.project.solution.runnableProjectsModel.projects.valueOrNull ?: return false

        val selectedProjectFilePath = FileUtil.toSystemIndependentName(project.getFile()?.path ?: "")
        val runnableProject = projects.firstOrNull {
            it.kind == RunnableProjectKind.AzureFunctions &&
            it.projectFilePath == selectedProjectFilePath
        }

        configuration.parameters.apply {
            projectFilePath = selectedProjectFilePath

            runnableProject?.let {
                projectTfm = it.projectOutputs.firstOrNull()?.tfm ?: projectTfm
            }
        }

        return true
    }
}