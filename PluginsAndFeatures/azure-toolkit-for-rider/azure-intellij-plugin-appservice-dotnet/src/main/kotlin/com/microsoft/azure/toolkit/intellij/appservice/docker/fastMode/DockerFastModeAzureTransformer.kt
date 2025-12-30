@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.docker.fastMode

import com.intellij.docker.DockerDeploymentConfiguration
import com.intellij.docker.agent.DockerAgentDeploymentConfig
import com.intellij.docker.agent.compose.beans.DockerComposeServiceBase
import com.intellij.docker.agent.compose.beans.v2.DockerComposeBuildV2
import com.intellij.docker.agent.compose.beans.v2.DockerComposeServiceV2
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.remoteServer.runtime.deployment.DeploymentTask
import com.jetbrains.rider.model.RdProjectDescriptor
import com.jetbrains.rider.model.RdProjectType
import com.jetbrains.rider.plugins.appender.docker.deployment.DeploymentTransformedParameters
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentTransformer
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentTransformer.RiderDockerDeploymentParameters
import com.jetbrains.rider.plugins.appender.docker.deployment.ServicePatchedParameters
import com.jetbrains.rider.plugins.appender.docker.fastMode.isFastModeEnabled
import com.jetbrains.rider.plugins.appender.docker.utils.hasBuild
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity


/**
 * Fast mode transformer for Azure Functions projects.
 *
 * Key differences vs Core-fast:
 * - Mount project into /home/site/wwwroot (Functions script root).
 * - Provide AzureWebJobsScriptRoot
 * - DO NOT override entrypoint/cmd (Functions base image already has the correct entrypoint).
 */
internal class DockerFastModeAzureTransformer : RiderDockerDeploymentTransformer {
    companion object {
        private const val COMPOSE_FAST_MODE_LABEL = "com.jetbrains.rider.fast.mode"
        private val LOG = logger<DockerFastModeAzureTransformer>()
    }

    override fun getPriority(): Int = 1

    override fun progressText(): String = "Applying Docker Fast mode deployment transformation for the Azure project"

    override suspend fun transformConfig(
        config: DockerAgentDeploymentConfig,
        deploymentTask: DeploymentTask<*>,
        deploymentParams: RiderDockerDeploymentParameters,
        transformedParams: DeploymentTransformedParameters,
    ): DeploymentTransformedParameters {
        LOG.info("Applying Azure Functions Fast mode deployment transformation for Dockerfile deployment")

        if (!isApplicable(deploymentTask, deploymentParams.projectModelEntity)) {
            LOG.trace("Azure Functions Fast mode is not applicable for the deployment task")
            return transformedParams
        }

        val fastModeInfo = DockerFastModeAzureService
            .getInstance(deploymentTask.project)
            .prepareFastMode(deploymentParams)

        if (fastModeInfo == null) {
            LOG.warn("Unable to prepare Azure Functions Fast mode info for deployment")
            return transformedParams
        }

        val volumes = transformedParams.volumeBindings + fastModeInfo.getFastModeVolumes()
        val envVars = transformedParams.environmentVariables + fastModeInfo.getFastModeEnvVars()

        val buildOptions = getBuildOptionsWithoutTarget(config)
        if (!fastModeInfo.stage.isNullOrEmpty()) {
            buildOptions.add("--target")
            buildOptions.add(fastModeInfo.stage)
        }

        val containerName = fastModeInfo.getContainerName(config.containerName)
        val imageTag = fastModeInfo.getImageTag(config.imageTags?.firstOrNull())
        val workingDir = fastModeInfo.getFastModeWorkingDir()

        val transformedConfig = transformedParams.copy(
            volumeBindings = volumes,
            environmentVariables = envVars,
            buildOptions = buildOptions,
            containerName = containerName,
            imageTag = imageTag,
            workingDir = workingDir,
            cmd = transformedParams.cmd,
            entrypoint = transformedParams.entrypoint
        )

        LOG.debug { "Transformed Azure Functions configuration: $transformedConfig" }

        return transformedConfig
    }

    override suspend fun patchComposeServices(
        services: List<Pair<String, DockerComposeServiceBase>>,
        allServices: List<Pair<String, DockerComposeServiceBase>>,
        deploymentTask: DeploymentTask<*>,
        deploymentParams: Map<String, RiderDockerDeploymentParameters>,
        patchedParams: Map<String, ServicePatchedParameters>,
    ): Map<String, ServicePatchedParameters> {
        LOG.info("Applying Azure Functions Fast mode deployment transformation for Docker Compose deployment")

        val servicesToProcess = buildMap {
            for ((serviceName, serviceInstance) in services) {
                if (!serviceInstance.hasBuild()) {
                    LOG.trace { "Service $serviceName doesn't have build section. Skip Azure Functions Fast mode for the service" }
                    continue
                }

                if (serviceInstance is DockerComposeServiceV2 && serviceInstance.labels != null) {
                    val label = serviceInstance.labels.envs[COMPOSE_FAST_MODE_LABEL]
                    if (label?.equals("false", true) == true) {
                        LOG.trace { "Fast mode is disabled for the service $serviceName with label" }
                        continue
                    }
                }

                val serviceDeploymentParams = deploymentParams[serviceName]
                if (serviceDeploymentParams == null) {
                    LOG.trace { "Unable to find Docker deployment model for the service $serviceName. Skip Azure Functions Fast mode" }
                    continue
                }

                if (!isApplicable(deploymentTask, serviceDeploymentParams.projectModelEntity)) {
                    LOG.trace { "Azure Functions Fast mode is not applicable for the service $serviceName" }
                    continue
                }

                put(serviceName, serviceDeploymentParams)
            }
        }

        if (servicesToProcess.isEmpty()) {
            LOG.info("Unable to find any service for Azure Functions Fast mode transformation")
            return patchedParams
        }

        val fastModeInfos = DockerFastModeAzureService
            .getInstance(deploymentTask.project)
            .prepareFastMode(servicesToProcess)

        if (fastModeInfos.isEmpty()) {
            LOG.info("Unable to prepare Azure Functions Fast mode info for any service. Skip transformation")
            return patchedParams
        }

        val result = patchedParams.toMutableMap()
        for ((serviceName, serviceInstance) in services) {
            if (!servicesToProcess.containsKey(serviceName)) continue

            val servicePatchedParams =
                patchComposeService(serviceName, serviceInstance, fastModeInfos, patchedParams[serviceName]) ?: continue

            result[serviceName] = servicePatchedParams
        }

        return result
    }

    private fun isApplicable(deploymentTask: DeploymentTask<*>, projectEntity: ProjectModelEntity?): Boolean {
        if (projectEntity == null) {
            LOG.debug("DockerFastModeAzureTransformer is not applicable: project entity is null.")
            return false
        }

        val dockerDeploymentConfiguration = deploymentTask.configuration as? DockerDeploymentConfiguration
        val projectType = (projectEntity.descriptor as? RdProjectDescriptor)?.specificType
        return dockerDeploymentConfiguration?.isFastModeEnabled == true && projectType == RdProjectType.AzureFunction
    }

    private fun getBuildOptionsWithoutTarget(config: DockerAgentDeploymentConfig): MutableList<String> {
        if (config.customBuildOptions.isNullOrEmpty()) return mutableListOf()

        if (!config.customBuildOptions.any { it.equals("--target", ignoreCase = true) })
            return config.customBuildOptions.toMutableList()

        val result = mutableListOf<String>()
        var i = 0
        while (i < config.customBuildOptions.size) {
            if (config.customBuildOptions[i].equals("--target", ignoreCase = true)) {
                i += 2
            } else {
                result.add(config.customBuildOptions[i])
                i++
            }
        }

        return result
    }

    private fun patchComposeService(
        serviceName: String,
        serviceInstance: DockerComposeServiceBase,
        fastModeInfos: Map<String, DockerFastModeAzureInfo>,
        patchedParams: ServicePatchedParameters?,
    ): ServicePatchedParameters? {
        LOG.trace { "Patching compose service $serviceName" }

        val fastModeInfo = fastModeInfos[serviceName] ?: return null

        val servicePatchedParams = patchedParams ?: ServicePatchedParameters()

        val volumes = servicePatchedParams.volumeBindings + fastModeInfo.getFastModeVolumes()
        val envVars = servicePatchedParams.environmentVariables + fastModeInfo.getComposeFastModeEnvVars()

        val build = if (serviceInstance is DockerComposeServiceV2) {
            serviceInstance.build?.let { build ->
                DockerComposeBuildV2(build.context, build.dockerfile).apply {
                    if (!fastModeInfo.stage.isNullOrEmpty()) target = fastModeInfo.stage
                }
            }
        } else null

        val imageName = fastModeInfo.getImageTag(serviceInstance.image)
        val workingDir = fastModeInfo.getFastModeWorkingDir()
        val cmd = fastModeInfo.getFastModeCmd()
        val entrypoint = fastModeInfo.getFastModeEntrypoint()

        val patchedParameters = servicePatchedParams.copy(
            volumeBindings = volumes,
            environmentVariables = envVars,
            build = build,
            imageName = imageName,
            workingDir = workingDir,
            cmd = cmd,
            entrypoint = entrypoint
        )

        LOG.debug { "Patched parameters for service ${serviceName}: $patchedParameters" }

        return patchedParameters
    }
}