/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.docker.debug

import com.intellij.docker.agent.DockerAgentDeploymentConfig
import com.intellij.docker.agent.compose.beans.DockerComposeServiceBase
import com.intellij.docker.agent.compose.beans.v2.DependsOn
import com.intellij.docker.agent.compose.beans.v2.DockerComposeBuildV2
import com.intellij.docker.agent.compose.beans.v2.DockerComposeServiceV2
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.remoteServer.runtime.deployment.DeploymentTask
import com.jetbrains.rider.model.RdProjectDescriptor
import com.jetbrains.rider.model.RdProjectType
import com.jetbrains.rider.plugins.appender.docker.RiderDockerBundle
import com.jetbrains.rider.plugins.appender.docker.debug.RiderDockerDebugConnectorInfo
import com.jetbrains.rider.plugins.appender.docker.deployment.DeploymentTransformedParameters
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDebugConnectorKeys.COMPOSE_DEBUG_CONNECTOR_INFO_KEY
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDebugConnectorKeys.DOCKERFILE_DEBUG_CONNECTOR_INFO_KEY
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentModel
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentTransformer
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentTransformer.RiderDockerDeploymentParameters
import com.jetbrains.rider.plugins.appender.docker.deployment.ServicePatchedParameters
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity

/**
 * Transforms Docker deployment configuration to enable debugging for Azure Functions.
 *
 * Overrides the container entrypoint to start both `func start` and the Rider debugger worker.
 * `func start` is configured to emit JSON output and write the spawned .NET worker PID to a file.
 * The debugger worker then reads this PID and attaches to the running process.
 *
 * Applies the required ports, volumes, environment variables, and build arguments
 * for both Dockerfile and Docker Compose deployments.
 */
internal class DockerDebugModeAzureTransformer : RiderDockerDeploymentTransformer {
    companion object {
        private const val COMPOSE_DEBUG_LABEL = "com.jetbrains.rider.debug"
        private const val SERVICE_HEALTHY_DEPENDENCY = "service_healthy"
        private const val SERVICE_COMPLETED_SUCCESSFULLY_DEPENDENCY = "service_completed_successfully"

        private val LOG = logger<DockerDebugModeAzureTransformer>()
    }

    override fun getPriority() = 2

    override fun progressText() = RiderDockerBundle.message("rider.docker.apply.debug.progress")

    override suspend fun transformConfig(
        config: DockerAgentDeploymentConfig,
        deploymentTask: DeploymentTask<*>,
        deploymentParams: RiderDockerDeploymentParameters,
        transformedParams: DeploymentTransformedParameters,
    ): DeploymentTransformedParameters {
        LOG.info("Applying Azure Functions Debug mode deployment transformation for Dockerfile deployment")

        if (!isApplicable(deploymentTask, deploymentParams.projectModelEntity)) {
            LOG.trace("Azure Debug mode is not applicable for the deployment task")
            return transformedParams
        }

        val debugModeInfo = DockerDebugModeAzureService
            .getInstance(deploymentTask.project)
            .prepareDebugMode(deploymentParams)

        if (debugModeInfo == null) {
            LOG.trace("Unable to prepare Azure Debug mode information. Skip transformation")
            return transformedParams
        }

        saveDebugInfoToEnvironment(
            debugModeInfo,
            transformedParams,
            deploymentParams.deploymentModel,
            deploymentTask.executionEnvironment
        )

        val ports = transformedParams.portBindings + debugModeInfo.getDebugModePorts()
        val volumes = transformedParams.volumeBindings + debugModeInfo.getDebugModeVolumes()
        val envVars = transformedParams.environmentVariables + debugModeInfo.getDebugModeEnvVars()
        val buildArgs = transformedParams.buildArgs + debugModeInfo.getDebugModeBuildArgs()

        val overriddenEntrypoint = debugModeInfo.getEntrypoint(deploymentParams.deploymentModel.getContainerOs())

        val transformedConfig = transformedParams.copy(
            portBindings = ports,
            volumeBindings = volumes,
            environmentVariables = envVars,
            buildArgs = buildArgs,
            cmd = emptyList(),
            entrypoint = overriddenEntrypoint
        )

        LOG.debug("Transformed configuration: $transformedConfig")

        return transformedConfig
    }

    override suspend fun patchComposeServices(
        services: List<Pair<String, DockerComposeServiceBase>>,
        allServices: List<Pair<String, DockerComposeServiceBase>>,
        deploymentTask: DeploymentTask<*>,
        deploymentParams: Map<String, RiderDockerDeploymentParameters>,
        patchedParams: Map<String, ServicePatchedParameters>,
    ): Map<String, ServicePatchedParameters> {
        LOG.info("Applying Azure Functions Debug mode deployment transformation for Docker Compose deployment")

        if (!deploymentTask.isDebugMode) {
            LOG.trace("Skip: not a Debug mode deployment task")
            return patchedParams
        }

        val servicesToProcess = buildMap {
            for ((serviceName, serviceInstance) in services) {
                if (serviceInstance is DockerComposeServiceV2 && serviceInstance.labels != null) {
                    val label = serviceInstance.labels.envs[COMPOSE_DEBUG_LABEL]
                    if (label?.equals("false", true) == true) {
                        LOG.trace { "Azure Debug mode disabled for service $serviceName by label" }
                        continue
                    }
                }

                val serviceDeploymentParams = deploymentParams[serviceName]
                if (serviceDeploymentParams == null) {
                    LOG.trace { "No deployment params for service $serviceName. Skip Azure Debug mode" }
                    continue
                }

                if (!isApplicable(deploymentTask, serviceDeploymentParams.projectModelEntity)) {
                    LOG.trace { "Azure Debug mode is not applicable for service $serviceName" }
                    continue
                }

                put(serviceName, serviceDeploymentParams)
            }
        }

        if (servicesToProcess.isEmpty()) {
            LOG.trace("No services for Azure Debug mode transformation")
            return patchedParams
        }

        val debugModeInfos = DockerDebugModeAzureService
            .getInstance(deploymentTask.project)
            .prepareDebugMode(servicesToProcess)

        if (debugModeInfos.isEmpty()) {
            LOG.trace("No Azure Debug mode info prepared for any service. Skip")
            return patchedParams
        }

        val servicesToSkip =
            detectDependentServices(allServices, debugModeInfos.keys.toHashSet(), deploymentTask.project)

        saveDebugInfoToEnvironment(
            debugModeInfos,
            patchedParams,
            servicesToProcess,
            servicesToSkip,
            deploymentTask.executionEnvironment
        )

        val result = patchedParams.toMutableMap()

        for ((serviceName, serviceInstance) in services) {
            if (!servicesToProcess.containsKey(serviceName) || servicesToSkip.contains(serviceName)) continue

            val patched = patchComposeService(
                serviceName = serviceName,
                serviceInstance = serviceInstance,
                debugModeInfos = debugModeInfos,
                deploymentParams = servicesToProcess,
                patchedParams = patchedParams[serviceName]
            ) ?: continue

            result[serviceName] = patched
        }

        return result
    }

    private fun isApplicable(deploymentTask: DeploymentTask<*>, projectEntity: ProjectModelEntity?): Boolean {
        if (projectEntity == null) {
            LOG.debug("DockerFastModeAzureTransformer is not applicable: project entity is null.")
            return false
        }

        val projectType = (projectEntity.descriptor as? RdProjectDescriptor)?.specificType
        return deploymentTask.isDebugMode && projectType == RdProjectType.AzureFunction
    }

    private fun saveDebugInfoToEnvironment(
        debugModeInfo: DockerDebugModeAzureInfo,
        deploymentTransformedParams: DeploymentTransformedParameters,
        dockerDeploymentModel: RiderDockerDeploymentModel,
        executionEnvironment: ExecutionEnvironment,
    ) {
        val debugConnectorInfo = RiderDockerDebugConnectorInfo(
            debugModeInfo.debugProvider,
            debugModeInfo.debuggerPorts.frontendPorts.hostPort,
            debugModeInfo.debuggerPorts.backendPorts.hostPort,
            deploymentTransformedParams.workingDir ?: dockerDeploymentModel.getWorkingDir(),
            debugModeInfo.getExecutionCommand(),
            debugModeInfo.containerOs,
            debugModeInfo.containerDotnetVersion
        )
        executionEnvironment.putUserData(DOCKERFILE_DEBUG_CONNECTOR_INFO_KEY, debugConnectorInfo)
    }

    private fun saveDebugInfoToEnvironment(
        debugModeInfos: Map<String, DockerDebugModeAzureInfo>,
        patchedParams: Map<String, ServicePatchedParameters>,
        deploymentParams: Map<String, RiderDockerDeploymentParameters>,
        servicesToSkip: HashSet<String>,
        executionEnvironment: ExecutionEnvironment,
    ) {
        val composeDebugConnectorInfos = mutableMapOf<String, RiderDockerDebugConnectorInfo>()

        for ((service, debugModeInfo) in debugModeInfos) {
            if (servicesToSkip.contains(service)) continue

            val servicePatchedParams = patchedParams[service]
            val deploymentModel = deploymentParams[service]?.deploymentModel ?: continue

            val serviceDebugConnectorInfo = RiderDockerDebugConnectorInfo(
                debugModeInfo.debugProvider,
                debugModeInfo.debuggerPorts.frontendPorts.hostPort,
                debugModeInfo.debuggerPorts.backendPorts.hostPort,
                servicePatchedParams?.workingDir ?: deploymentModel.getWorkingDir(),
                debugModeInfo.getExecutionCommand(),
                debugModeInfo.containerOs,
                debugModeInfo.containerDotnetVersion
            )

            composeDebugConnectorInfos[service] = serviceDebugConnectorInfo
        }

        executionEnvironment.putUserData(COMPOSE_DEBUG_CONNECTOR_INFO_KEY, composeDebugConnectorInfos)
    }

    private fun patchComposeService(
        serviceName: String,
        serviceInstance: DockerComposeServiceBase,
        debugModeInfos: Map<String, DockerDebugModeAzureInfo>,
        deploymentParams: Map<String, RiderDockerDeploymentParameters>,
        patchedParams: ServicePatchedParameters?,
    ): ServicePatchedParameters? {
        LOG.trace { "Patching compose service $serviceName for Azure Debug mode" }

        val debugModeInfo = debugModeInfos[serviceName] ?: return null
        val deploymentModel = deploymentParams[serviceName]?.deploymentModel ?: return null

        val servicePatchedParams = patchedParams ?: ServicePatchedParameters()

        val ports = servicePatchedParams.portBindings + debugModeInfo.getComposeDebugModePorts()
        val volumes = servicePatchedParams.volumeBindings + debugModeInfo.getDebugModeVolumes()
        val envVars = servicePatchedParams.environmentVariables + debugModeInfo.getComposeDebugModeEnvVars()

        val patchedBuild = servicePatchedParams.build
        val build = if (serviceInstance is DockerComposeServiceV2) {
            serviceInstance.build?.let { build ->
                DockerComposeBuildV2(build.context, build.dockerfile).apply {
                    if (patchedBuild?.target != null) target = patchedBuild.target
                    val args = debugModeInfo.getComposeDebugModeBuildArgs()
                    arguments = if (patchedBuild?.arguments.isNullOrEmpty()) args else patchedBuild.arguments + args
                }
            }
        } else null

        val entrypoint = debugModeInfo.getEntrypoint(deploymentModel.getContainerOs())

        return servicePatchedParams.copy(
            portBindings = ports,
            volumeBindings = volumes,
            environmentVariables = envVars,
            build = build,
            entrypoint = entrypoint
        )
    }

    private fun detectDependentServices(
        services: List<Pair<String, DockerComposeServiceBase>>,
        servicesToDebug: HashSet<String>,
        project: Project,
    ): HashSet<String> {
        val dependentServices = mutableListOf<String>()
        val servicesToSkip = HashSet<String>()

        for ((serviceName, serviceInstance) in services) {
            val map = serviceInstance.dependsOn?.toJsonValue() as? Map<*, *> ?: continue
            for ((key, value) in map) {
                if (key !is String || !servicesToDebug.contains(key) || value !is DependsOn.Description? || value == null) continue
                if (value.condition == SERVICE_HEALTHY_DEPENDENCY || value.condition == SERVICE_COMPLETED_SUCCESSFULLY_DEPENDENCY) {
                    dependentServices.add(serviceName)
                    servicesToSkip.add(key)
                }
            }
        }

        if (dependentServices.isNotEmpty()) {
            warnAboutDependentServices(dependentServices, servicesToSkip, project)
        }

        return servicesToSkip
    }

    private fun warnAboutDependentServices(
        dependentServices: List<String>,
        servicesToSkip: HashSet<String>,
        project: Project
    ) {
        Notification(
            "Docker",
            RiderDockerBundle.message("rider.docker.notification.dependent.services.title"),
            RiderDockerBundle.message(
                "rider.docker.notification.dependent.services.text",
                dependentServices.joinToString(),
                servicesToSkip.joinToString()
            ),
            NotificationType.WARNING
        ).notify(project)
    }
}