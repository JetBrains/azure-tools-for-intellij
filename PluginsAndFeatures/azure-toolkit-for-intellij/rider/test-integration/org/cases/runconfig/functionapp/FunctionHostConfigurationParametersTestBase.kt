/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
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

package org.cases.runconfig.functionapp

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.EnvironmentVariable
import com.jetbrains.rider.model.*
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.test.base.BaseTestWithSolution
import org.jetbrains.plugins.azure.functions.daemon.AzureRunnableProjectKinds
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationParameters
import org.testng.annotations.Test
import java.io.File

abstract class FunctionHostConfigurationParametersTestBase(
        private val solutionDirectoryName: String,
        val projectName: String,
        val projectTfm: String
): BaseTestWithSolution() {

    override fun getSolutionDirectoryName(): String = solutionDirectoryName

    override val waitForCaches: Boolean = true

    override val restoreNuGetPackages: Boolean = true

    //region Invalid

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Solution is loading, please wait for a few seconds\\.")
    fun testValidate_Solution_NotLoaded() {
        val parameters = createParameters(project = project, projectTfm = projectTfm)
        project.solution.isLoaded.set(false)
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not specified\\.")
    fun testValidate_RunnableProjects_NoRunnableProjects() {
        val parameters = createParameters(project = project, projectTfm = projectTfm)
        project.solution.runnableProjectsModel.projects.set(emptyList())
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not specified\\.")
    fun testValidate_RunnableProjects_NoFunctionProjectType() {
        val parameters = createParameters(project = project, projectTfm = projectTfm)
        project.solution.runnableProjectsModel.projects.set(listOf(createRunnableProject(kind = RunnableProjectKinds.DotNetCore)))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Project is not specified\\.")
    fun testValidate_RunnableProjects_MultipleRunnableProjectsMatch() {
        val projectFilePath = File("/project/file/path").absolutePath
        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(kind = RunnableProjectKinds.DotNetCore, projectFilePath = projectFilePath),
                createRunnableProject(kind = RunnableProjectKinds.DotNetCore, projectFilePath = projectFilePath)
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Function App is not loaded properly\\.")
    fun testValidate_RunnableProjects_ProjectWithProblems() {
        val projectFilePath = File("/project/file/path").absolutePath
        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = "Function App is not loaded properly."
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid exe path: '/not/existing/path'\\.")
    fun testValidate_TrackProjectExePath_NotExists() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = false,
                exePath = "/not/existing/path",
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid exe path: '<empty>'\\.")
    fun testValidate_TrackProjectExePath_EmptyExePath() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = false,
                exePath = "",
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid exe path:\\s.+\\.")
    fun testValidate_TrackProjectExePath_NotAFile() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = false,
                exePath = tempTestDirectory.path,
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid working directory: '/not/existing/path'\\.")
    fun testValidate_TrackProjectWorkingDirectory_NotExists() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = false,
                workingDirectory = "/not/existing/path",
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid working directory: '<empty>'\\.")
    fun testValidate_TrackProjectWorkingDirectory_EmptyDirectory() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = false,
                workingDirectory = "",
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Invalid working directory:\\s.+\\.")
    fun testValidate_TrackProjectWorkingDirectory_NotADirectory() {
        val projectFilePath = File("/project/file/path").absolutePath

        val workingDirectoryFile = tempTestDirectory.resolve("$projectName.dll")
        workingDirectoryFile.createNewFile()

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = false,
                workingDirectory = workingDirectoryFile.path,
                projectTfm = projectTfm
        )

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null
                )
        ))
        parameters.validate(createHost())
    }

    //endregion Invalid

    //region Valid

    @Test
    fun testValidate_Valid_NetCoreRuntime() {
        val projectFilePath = File("/project/file/path").absolutePath

        val parameters = createParameters(
                project = project,
                projectFilePath = projectFilePath,
                trackProjectExePath = true,
                trackProjectWorkingDirectory = true,
                projectTfm = projectTfm
        ).apply { useMonoRuntime = false }

        val host = createHost()

        project.solution.runnableProjectsModel.projects.set(listOf(
                createRunnableProject(
                        kind = AzureRunnableProjectKinds.AzureFunctions,
                        projectFilePath = projectFilePath,
                        problems = null)
        ))

        parameters.validate(host)
    }

    //endregion Valid

    private fun createParameters(
            project: Project = this.project,
            exePath: String = "",
            programParameters: String = "host start --pause-on-error",
            workingDirectory: String = File("/working/path").absolutePath,
            envs: Map<String, String> = emptyMap(),
            isPassParentEnvs: Boolean = true,
            useExternalConsole: Boolean = false,
            projectFilePath: String = "",
            trackProjectExePath: Boolean = false,
            trackProjectArguments: Boolean = false,
            trackProjectWorkingDirectory: Boolean = false,
            projectKind: RunnableProjectKind = AzureRunnableProjectKinds.AzureFunctions,
            projectTfm: String,
            functionNames: String = "",
            startBrowserParameters: DotNetStartBrowserParameters = DotNetStartBrowserParameters()
    ): AzureFunctionsHostConfigurationParameters =
            AzureFunctionsHostConfigurationParameters(
                    project = project,
                    exePath = exePath,
                    programParameters = programParameters,
                    workingDirectory = workingDirectory,
                    envs = envs,
                    isPassParentEnvs = isPassParentEnvs,
                    useExternalConsole = useExternalConsole,
                    projectFilePath = projectFilePath,
                    trackProjectExePath = trackProjectExePath,
                    trackProjectArguments = trackProjectArguments,
                    trackProjectWorkingDirectory = trackProjectWorkingDirectory,
                    projectKind = projectKind,
                    projectTfm = projectTfm,
                    functionNames = functionNames,
                    startBrowserParameters = startBrowserParameters
            )

    fun createHost() = RiderDotNetActiveRuntimeHost.getInstance(project)

    fun createRunnableProject(
            name: String = "TestName",
            fullName: String = "TestFullName",
            projectFilePath: String = File("/project/file/path").absolutePath,
            kind: RunnableProjectKind = AzureRunnableProjectKinds.AzureFunctions,
            projectOutputs: List<ProjectOutput> = emptyList(),
            environmentVariables: List<EnvironmentVariable> = emptyList(),
            problems: String? = null,
            customAttributes: List<CustomAttribute> = emptyList()
    ): RunnableProject =
        RunnableProject(
                name = name,
                fullName = fullName,
                projectFilePath = projectFilePath,
                kind = kind,
                projectOutputs = projectOutputs,
                environmentVariables = environmentVariables,
                problems = problems,
                customAttributes = customAttributes
        )
}
