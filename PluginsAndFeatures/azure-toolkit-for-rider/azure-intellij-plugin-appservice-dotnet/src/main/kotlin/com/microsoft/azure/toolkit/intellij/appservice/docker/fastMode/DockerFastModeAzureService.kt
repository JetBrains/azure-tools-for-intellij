/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.docker.fastMode

import com.intellij.execution.CantRunException
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.forEachWithProgress
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.build.tasks.BuildTaskThrottler
import com.jetbrains.rider.ijent.extensions.toRd
import com.jetbrains.rider.model.BuildTarget
import com.jetbrains.rider.model.RdProjectFastModePropertiesResponse
import com.jetbrains.rider.model.RdProjectPropertiesRequest
import com.jetbrains.rider.model.dockerModel
import com.jetbrains.rider.plugins.appender.docker.RiderDockerBundle
import com.jetbrains.rider.plugins.appender.docker.common.DockerContainerType
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentFromFileModel
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentModel
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentTransformer.RiderDockerDeploymentParameters
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentEnvironmentVariable
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentVolume
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.solutionDirectoryPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

/**
 * Prepares Fast-mode info for Azure Functions projects.
 *
 * Fast-mode behavior:
 * - build on host
 * - run only base stage (no publish/final)
 * - mount sources/output into /home/site/wwwroot
 * - set AzureWebJobsScriptRoot + worker runtime env vars
 */
@Service(Service.Level.PROJECT)
internal class DockerFastModeAzureService(private val project: Project) : LifetimedService() {
    companion object {
        private const val FUNCTIONS_SCRIPT_ROOT = "/home/site/wwwroot"
        private const val DEFAULT_SOLUTION_PATH = "/src"
        private const val ROOT_NUGET_PACKAGES_PATH = "/root/.nuget/packages"
        private const val APP_NUGET_PACKAGES_PATH = "/home/app/.nuget/packages"
        private const val AZURE_WEBJOBS_SCRIPT_ROOT = "AzureWebJobsScriptRoot"
        private const val DOTNET_USE_POLLING_FILE_WATCHER = "DOTNET_USE_POLLING_FILE_WATCHER"
        private const val AZURE_FUNCTIONS_JOB_HOST_LOGGING = "AzureFunctionsJobHost__Logging__Console__IsEnabled"

        private val LOG = logger<DockerFastModeAzureService>()

        internal fun getInstance(project: Project): DockerFastModeAzureService = project.service()
    }

    internal suspend fun prepareFastMode(
        deploymentParams: RiderDockerDeploymentParameters,
    ): DockerFastModeAzureInfo? {
        LOG.trace("Preparing Azure Functions Fast mode for Dockerfile deployment")

        val fastModeInfo = getFastModeInfo(deploymentParams) ?: return null
        buildProject(fastModeInfo.projectFilePath)

        return fastModeInfo
    }

    internal suspend fun prepareFastMode(
        deploymentParams: Map<String, RiderDockerDeploymentParameters>,
    ): Map<String, DockerFastModeAzureInfo> {
        LOG.trace("Preparing Azure Functions Fast mode for Docker Compose deployment")

        val fastModeInfos = mutableMapOf<String, DockerFastModeAzureInfo>()

        deploymentParams.entries.forEachWithProgress { (service, serviceDeploymentParams) ->
            val fastModeInfo = getFastModeInfo(serviceDeploymentParams) ?: return@forEachWithProgress
            fastModeInfos[service] = fastModeInfo
        }

        if (fastModeInfos.isEmpty()) return fastModeInfos

        buildProjects(fastModeInfos.values.map { it.projectFilePath }.distinct())

        return fastModeInfos
    }

    private suspend fun getFastModeInfo(
        deploymentParams: RiderDockerDeploymentParameters,
    ): DockerFastModeAzureInfo? {
        val dockerDeploymentModel = deploymentParams.deploymentModel

        val containerOs = dockerDeploymentModel.getContainerOs()
        if (containerOs == DockerContainerType.Windows) {
            LOG.warn("Azure Functions Fast mode isn't available for Windows containers")
            return null
        }

        if (dockerDeploymentModel !is RiderDockerDeploymentFromFileModel) {
            LOG.warn("Azure Functions Fast mode is available only for Dockerfile deployment")
            return null
        }

        val projectFilePath = deploymentParams.projectFilePath
        if (projectFilePath == null) {
            LOG.warn("Project file path for the Dockerfile is null")
            return null
        }

        val request = RdProjectPropertiesRequest(projectFilePath.absolute().toRd())
        val projectProperties = withContext(Dispatchers.EDT) {
            project.solution.dockerModel.getProjectFastModeProperties.startSuspending(request)
        }
        if (projectProperties == null || !projectProperties.isProjectTypeAvailableForFastMode) {
            LOG.warn("Unable to receive project properties or project is not available for Fast mode")
            return null
        }

        val targetStage = getTargetStage(dockerDeploymentModel, projectProperties)
        val isRootless = dockerDeploymentModel.isRootless(targetStage)

        val volumes = getFastModeVolumes(
            deploymentModel = dockerDeploymentModel,
            solutionFolderPath = project.solutionDirectoryPath,
            projectFilePath = projectFilePath,
            projectProperties = projectProperties,
            isRootless = isRootless,
        )

        val environmentVariables = getFastModeEnvironmentVariables(
            deploymentModel = dockerDeploymentModel,
            targetStage = targetStage,
        )

        val fastModeInfo = DockerFastModeAzureInfo(
            projectFilePath = projectFilePath,
            projectName = projectProperties.projectName,
            stage = targetStage,
            isRootless = isRootless,
            fastModeVolumes = volumes,
            environmentVariables = environmentVariables,
        )

        LOG.debug { "Fast mode info for Dockerfile ${dockerDeploymentModel.dockerfilePath.absolutePathString()}: $fastModeInfo" }

        return fastModeInfo
    }

    private fun getFastModeVolumes(
        deploymentModel: RiderDockerDeploymentModel,
        solutionFolderPath: Path,
        projectFilePath: Path,
        projectProperties: RdProjectFastModePropertiesResponse,
        isRootless: Boolean,
    ): DockerFastModeAzureVolumes {
        val existingVolumes = deploymentModel.getContainerVolumes()

        val projectOutputPath = projectFilePath.parent.resolve(projectProperties.projectRelativeOutputPath ?: "").parent
        val scriptRootVolume = TransformedDeploymentVolume(
            containerPath = FUNCTIONS_SCRIPT_ROOT, hostPath = projectOutputPath.absolutePathString(), readOnly = false
        )

        val solutionContainerPath = findAvailablePathInsideContainer(existingVolumes, DEFAULT_SOLUTION_PATH)
        val solutionVolume = if (!existingVolumes.contains(solutionContainerPath)) {
            TransformedDeploymentVolume(
                containerPath = solutionContainerPath,
                hostPath = solutionFolderPath.absolutePathString(),
                readOnly = false
            )
        } else null

        val nugetPath = if (isRootless) APP_NUGET_PACKAGES_PATH else ROOT_NUGET_PACKAGES_PATH
        val nugetPackagesFolder = if (!existingVolumes.contains(nugetPath)) {
            TransformedDeploymentVolume(nugetPath, projectProperties.nugetGlobalPackagesFolder, true)
        } else null

        return DockerFastModeAzureVolumes(
            scriptRootFolder = scriptRootVolume,
            solutionFolder = solutionVolume,
            nugetPackagesFolder = nugetPackagesFolder,
        )
    }

    private fun getFastModeEnvironmentVariables(
        deploymentModel: RiderDockerDeploymentModel,
        targetStage: String?,
    ): DockerFastModeAzureEnvironmentVariables {
        val existingVariableKeys = deploymentModel.getEnvironmentVariables(targetStage).keys

        val scriptRootVar = TransformedDeploymentEnvironmentVariable(AZURE_WEBJOBS_SCRIPT_ROOT, FUNCTIONS_SCRIPT_ROOT)

        val consoleLoggingVar = if (!existingVariableKeys.contains(AZURE_FUNCTIONS_JOB_HOST_LOGGING)) {
            TransformedDeploymentEnvironmentVariable(AZURE_FUNCTIONS_JOB_HOST_LOGGING, "true")
        } else null

        val dotnetUsePollingFileWatcherVariable = if (!existingVariableKeys.contains(DOTNET_USE_POLLING_FILE_WATCHER)) {
            TransformedDeploymentEnvironmentVariable(DOTNET_USE_POLLING_FILE_WATCHER, "true")
        } else null

        return DockerFastModeAzureEnvironmentVariables(
            azureWebJobsScriptRoot = scriptRootVar,
            azureFunctionsJobHostConsoleLoggingEnabled = consoleLoggingVar,
            dotnetUsePollingFileWatcherVariable = dotnetUsePollingFileWatcherVariable
        )
    }

    private fun getTargetStage(
        dockerDeploymentModel: RiderDockerDeploymentFromFileModel,
        projectProperties: RdProjectFastModePropertiesResponse,
    ): String? {
        val projectTargetStage = projectProperties.projectFastModeStage
        val dockerfileStages = dockerDeploymentModel.getDockerfileStages()

        if (!projectTargetStage.isNullOrEmpty() && dockerfileStages.any { it.equals(projectTargetStage, true) }) {
            return projectTargetStage
        }

        if (dockerfileStages.size == 1 || dockerfileStages.all { it.isEmpty() }) {
            return null
        }

        return dockerfileStages.firstOrNull { it.isNotEmpty() }
    }

    @Suppress("SameParameterValue")
    private fun findAvailablePathInsideContainer(existingVolumes: Set<String>, defaultPath: String): String {
        var path = defaultPath
        var suffix = 1
        while (existingVolumes.contains(path)) {
            path = "$defaultPath-$suffix"
            suffix++
        }
        return path
    }

    private suspend fun buildProject(projectFilePath: Path) {
        ThreadingAssertions.assertBackgroundThread()

        LOG.trace("Building Azure Functions project for Fast mode")

        val buildParameters = BuildParameters(
            BuildTarget(), listOf(projectFilePath.absolutePathString()), silentMode = true
        )

        val status = BuildTaskThrottler.getInstance(project).buildSequentially(buildParameters)
        if (!status.msBuildStatus) throw CantRunException(RiderDockerBundle.message("rider.docker.fast.mode.can.not.build.exception.message"))
    }

    private suspend fun buildProjects(projectFilePaths: Collection<Path>) {
        ThreadingAssertions.assertBackgroundThread()

        LOG.trace("Building Azure Functions projects for Fast mode")

        val buildParameters = BuildParameters(
            BuildTarget(), projectFilePaths.map { it.absolutePathString() }, silentMode = true
        )

        val status = BuildTaskThrottler.getInstance(project).buildSequentially(buildParameters)
        if (!status.msBuildStatus) throw CantRunException(RiderDockerBundle.message("rider.docker.fast.mode.can.not.build.exception.message"))
    }
}