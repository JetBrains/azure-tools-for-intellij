/**
 * Copyright (c) 2023 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.util

import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution

object FrameworkUtil {
    private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
    private val netAppVersionRegex = Regex("net([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
    private val netFxAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

    fun getCurrentFrameworkId(project: Project, publishableProject: PublishableProjectModel): String? {
        val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
        return targetFramework?.currentTargetFrameworkId?.valueOrNull?.framework?.id
    }

    fun getProjectNetCoreFrameworkVersion(project: Project, publishableProject: PublishableProjectModel, defaultVersion: String = "6.0"): String {
        val currentFramework = getCurrentFrameworkId(project, publishableProject) ?: return defaultVersion
        return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value // netX.Y
                ?: netCoreAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value // .NETCoreApp,version=vX.Y
                ?: defaultVersion
    }

    fun getProjectNetFrameworkVersion(project: Project, publishableProject: PublishableProjectModel, defaultVersion: String = "4.7"): String {
        val currentFramework = getCurrentFrameworkId(project, publishableProject) ?: return defaultVersion
        return netFxAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value ?: defaultVersion
    }
}