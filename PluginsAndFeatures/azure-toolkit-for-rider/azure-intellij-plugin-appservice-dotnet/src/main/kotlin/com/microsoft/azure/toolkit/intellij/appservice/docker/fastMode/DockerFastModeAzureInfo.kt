/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.docker.fastMode

import com.intellij.docker.agent.DockerRepoTag
import com.intellij.docker.agent.settings.DockerEnvVarImpl
import com.intellij.docker.agent.settings.DockerVolumeBindingImpl
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentEnvironmentVariable
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentVolume
import java.nio.file.Path

internal data class DockerFastModeAzureInfo(
    val projectFilePath: Path,
    val projectName: String,
    val stage: String?,
    val isRootless: Boolean,
    val fastModeVolumes: DockerFastModeAzureVolumes,
    val environmentVariables: DockerFastModeAzureEnvironmentVariables,
)

internal data class DockerFastModeAzureVolumes(
    val scriptRootFolder: TransformedDeploymentVolume,
    val solutionFolder: TransformedDeploymentVolume?,
    val nugetPackagesFolder: TransformedDeploymentVolume?,
)

internal data class DockerFastModeAzureEnvironmentVariables(
    val azureWebJobsScriptRoot: TransformedDeploymentEnvironmentVariable?,
    val azureFunctionsJobHostConsoleLoggingEnabled: TransformedDeploymentEnvironmentVariable?,
    val dotnetUsePollingFileWatcherVariable: TransformedDeploymentEnvironmentVariable?,
)

internal fun DockerFastModeAzureInfo.getFastModeVolumes() = buildList {
    add(fastModeVolumes.scriptRootFolder.toVolumeBinding())
    fastModeVolumes.solutionFolder?.let { add(it.toVolumeBinding()) }
    fastModeVolumes.nugetPackagesFolder?.let { add(it.toVolumeBinding()) }
}

internal fun DockerFastModeAzureInfo.getFastModeEnvVars() = buildList {
    environmentVariables.azureWebJobsScriptRoot?.let { add(it.toEnvVar()) }
    environmentVariables.azureFunctionsJobHostConsoleLoggingEnabled?.let { add(it.toEnvVar()) }
    environmentVariables.dotnetUsePollingFileWatcherVariable?.let { add(it.toEnvVar()) }
}

internal fun DockerFastModeAzureInfo.getComposeFastModeEnvVars() = buildMap {
    environmentVariables.azureWebJobsScriptRoot?.let { put(it.key, it.value) }
    environmentVariables.dotnetUsePollingFileWatcherVariable?.let { put(it.key, it.value) }
}

internal fun DockerFastModeAzureInfo.getFastModeWorkingDir() = fastModeVolumes.scriptRootFolder.containerPath

internal fun DockerFastModeAzureInfo.getFastModeCmd() = emptyList<String>()

internal fun DockerFastModeAzureInfo.getFastModeEntrypoint(): List<String>? = null

internal fun DockerFastModeAzureInfo.getContainerName(baseContainerName: String?): String {
    return if (baseContainerName.isNullOrEmpty()) projectName else baseContainerName
}

private const val DEFAULT_DEV_TAG = "dev"

internal fun DockerFastModeAzureInfo.getImageTag(baseImageTag: String?): String {
    if (baseImageTag.isNullOrEmpty()) return "${projectName}:${DEFAULT_DEV_TAG}"

    val tag = DockerRepoTag.fromString(baseImageTag)
    return tag.qualifiedRepository + ":${DEFAULT_DEV_TAG}"
}


/* Extensions from RiderTransformedDeploymentConfig, they are internal in the rider.intellij.plugin.appender.
   TODO(Remove them) */
internal fun TransformedDeploymentVolume.toVolumeBinding() = DockerVolumeBindingImpl(
    containerPath,
    hostPath,
    readOnly
)

internal fun TransformedDeploymentEnvironmentVariable.toEnvVar() = DockerEnvVarImpl(
    key,
    value
)