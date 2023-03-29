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

import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.jetbrains.rider.build.BuildHost
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldContains
import com.jetbrains.rider.test.base.BaseTestWithSolution
import com.jetbrains.rider.test.enums.CoreVersion
import com.jetbrains.rider.test.scriptingApi.*
import org.jetbrains.plugins.azure.functions.buildTasks.BuildFunctionsProjectBeforeRunTaskProvider
import org.jetbrains.plugins.azure.functions.daemon.AzureRunnableProjectKinds
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfiguration
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationType
import org.jetbrains.plugins.azure.storage.azurite.AzuriteBeforeRunTaskProvider
import org.testng.annotations.Test
import kotlin.test.fail

abstract class FunctionHostConfigurationTestBase(
        private val solutionDirectoryName: String,
        val projectName: String,
        val projectTfm: String,
        val coreVersion: CoreVersion
) : BaseTestWithSolution() {

    override fun getSolutionDirectoryName(): String = solutionDirectoryName

    override val waitForCaches: Boolean = true

    override val restoreNuGetPackages: Boolean = true

    //region Configuration

    @Test(description = "Function App project should create a default run configuration to run function app.")
    fun testFunctionHost_DefaultRunConfigurations() {
        val allConfigurations = RunManagerEx.getInstanceEx(project).allConfigurationsList
                .filter { it.javaClass == AzureFunctionsHostConfiguration::class.java }

        allConfigurations.size.shouldBe(1)
        allConfigurations[0].javaClass.shouldBe(AzureFunctionsHostConfiguration::class.java)
        allConfigurations[0].name.shouldBe(projectName)
    }

    //endregion Configuration

    //region Before Run Tasks

    @Test(description = "Verifies Function Host run configuration has Build Function App before run task by default.")
    fun testFunctionHost_BeforeRunTasks_Defaults_BuildFunctionsProject() {
        val configuration = createRunConfiguration(
                name = "Run Function App: ${project.name}",
                configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        configuration.name.shouldBe("Run Function App: $projectName")

        configuration.beforeRunTasks.shouldContains { beforeRunTask ->
            beforeRunTask.providerId == BuildFunctionsProjectBeforeRunTaskProvider.providerId
        }
    }

    @Test(description = "Verifies Function Host run configuration has Start Azurite before run task by default.")
    fun testFunctionHost_BeforeRunTasks_Defaults_AzuriteBeforeRunTask() {
        val configuration = createRunConfiguration(
                name = "Run Function App: ${project.name}",
                configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        configuration.name.shouldBe("Run Function App: $projectName")

        configuration.beforeRunTasks.shouldContains { beforeRunTask ->
            beforeRunTask.providerId == AzuriteBeforeRunTaskProvider.providerId
        }
    }

    // TODO: Need to make sure function core tools is available on a server before enable.
    @Test(enabled = false, description = "Check before run task that should build a function app project and start Azurite.")
    fun testFunctionHost_BeforeRunTasks_BuildFunctionProject_StartAzurite() {
        addProject(arrayOf(projectName), "ConsoleApp1", ProjectTemplateIds.core(coreVersion).consoleApplication)

        val configuration = createRunConfiguration(
                name = "Run Function App: ${project.name}",
                configurationType = AzureFunctionsHostConfigurationType::class.java
        ) as AzureFunctionsHostConfiguration

        val projectToRun = project.solution.runnableProjectsModel.projects.valueOrNull?.find { it.name == projectName }
                ?: fail("Project to run is not found.")

        configuration.parameters.projectKind = AzureRunnableProjectKinds.AzureFunctions
        configuration.parameters.projectTfm = projectTfm
        configuration.parameters.projectFilePath = projectToRun.projectFilePath
        configuration.parameters.workingDirectory = projectToRun.projectOutputs.first().workingDirectory
        configuration.parameters.exePath = projectToRun.projectOutputs.first().exePath
        configuration.parameters.projectTfm = projectToRun.projectOutputs.first().tfm?.presentableName ?: ""

        configuration.beforeRunTasks.shouldContains { beforeRunTask ->
            beforeRunTask.providerId == BuildFunctionsProjectBeforeRunTaskProvider.providerId
        }

        configuration.beforeRunTasks.shouldContains { beforeRunTask ->
            beforeRunTask.providerId == AzuriteBeforeRunTaskProvider.providerId
        }

        checkCanExecuteRunConfiguration(configuration)

        val buildResult = dumpProjectBuildSession {
            executeBeforeRunTasksForSelectedConfiguration(project, DEFAULT_BUILD_TIMEOUT)
        }

        buildResult.size.shouldBe(1, "Expected to build $projectName project once, but got: ${buildResult.size}")
        assertBuildProjectSucceeded(projectName = projectName, buildResult = buildResult.first())
    }

    //endregion Before Run Tasks

    //region Run

    // TODO: Need to make sure function core tools is available on a server before enable.
    @Test(enabled = false, description = "Start a function app and verify output.")
    fun testFunctionHost_Run() { }

    //endregion Run

    private fun createRunConfiguration(name: String, type: Class<out ConfigurationType>): RunConfiguration {
        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val settings = runManagerEx.createConfiguration(name, type)
        runManagerEx.addConfiguration(settings)
        runManagerEx.selectedConfiguration = settings

        return settings.configuration
    }

    private fun assertBuildProjectSucceeded(projectName: String, buildResult: BuildSessionAssertion) {
        checkBuildResult(buildResult = buildResult.result.kind, errors = emptyList())
        BuildHost.getBuildTargetName(buildResult.target).shouldBe("Build")

        assertBuildCompleteMessage(projectName = projectName, buildOutput = buildResult.output)
    }

    private fun assertBuildCompleteMessage(projectName: String, buildOutput: String) {
        buildOutput.shouldContains("------- Started building project: $projectName")
        buildOutput.shouldContains("------- Finished building project: $projectName. Succeeded: True.")
        buildOutput.shouldContains("Build completed in")
    }
}
