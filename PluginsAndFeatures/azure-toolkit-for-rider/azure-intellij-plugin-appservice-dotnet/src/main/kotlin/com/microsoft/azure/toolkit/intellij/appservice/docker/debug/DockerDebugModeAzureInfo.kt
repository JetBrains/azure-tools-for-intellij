/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.docker.debug

import com.jetbrains.rider.BundledAssemblyLauncherInfo
import com.jetbrains.rider.CPUKind
import com.jetbrains.rider.plugins.appender.docker.common.DockerContainerType
import com.jetbrains.rider.plugins.appender.docker.debug.providers.RiderDockerDebugProvider
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentEnvironmentVariable
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentPortPair
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentVolume
import com.jetbrains.rider.run.ConsoleKind
import com.jetbrains.rider.run.DebugProfileStateBase
import com.jetbrains.rider.run.environment.ExecutableType
import com.microsoft.azure.toolkit.intellij.appservice.docker.toComposePorts
import com.microsoft.azure.toolkit.intellij.appservice.docker.toEnvVar
import com.microsoft.azure.toolkit.intellij.appservice.docker.toPortBinding
import com.microsoft.azure.toolkit.intellij.appservice.docker.toVolumeBinding

internal data class DockerDebugModeAzureInfo(
    val debugProvider: RiderDockerDebugProvider,
    val debuggerLauncherInfo: BundledAssemblyLauncherInfo,
    val targetCpuKind: CPUKind,
    val debuggerPorts: DockerDebuggerPorts,
    val useLoopbackAddress: Boolean,
    val debuggerVolumes: DockerDebuggerVolumes,
    val environmentVariables: DockerDebuggerAzureEnvironmentVariables,
    val buildArgs: DockerDebuggerBuildArgs,
    val containerOs: DockerContainerType,
    val containerDotnetVersion: String?,
    val debuggerTimeout: Int
)

internal data class DockerDebuggerPorts(
    val frontendPorts: TransformedDeploymentPortPair,
    val backendPorts: TransformedDeploymentPortPair,
    val roslynWorkerPorts: TransformedDeploymentPortPair
)

internal data class DockerDebuggerVolumes(
    val debuggerWorkerFolder: TransformedDeploymentVolume,
    val debuggerLogConfigFolder: TransformedDeploymentVolume?,
    val debuggerLogFolder: TransformedDeploymentVolume?
)

internal data class DockerDebuggerBuildArgs(
    val buildConfiguration: TransformedDeploymentEnvironmentVariable
)

internal data class DockerDebuggerAzureEnvironmentVariables(
    val logFolderVariable: TransformedDeploymentEnvironmentVariable,
    val logConfigFolderVariable: TransformedDeploymentEnvironmentVariable,
    val azureFunctionsConsoleLoggingEnabled: TransformedDeploymentEnvironmentVariable
)

internal fun DockerDebugModeAzureInfo.getDebugModePorts() = buildList {
    add(debuggerPorts.frontendPorts.toPortBinding(useLoopbackAddress))
    add(debuggerPorts.backendPorts.toPortBinding(useLoopbackAddress))
    add(debuggerPorts.roslynWorkerPorts.toPortBinding(useLoopbackAddress))
}

internal fun DockerDebugModeAzureInfo.getComposeDebugModePorts() = buildList {
    add(debuggerPorts.frontendPorts.toComposePorts(useLoopbackAddress))
    add(debuggerPorts.backendPorts.toComposePorts(useLoopbackAddress))
    add(debuggerPorts.roslynWorkerPorts.toComposePorts(useLoopbackAddress))
}

internal fun DockerDebugModeAzureInfo.getDebugModeVolumes() = buildList {
    add(debuggerVolumes.debuggerWorkerFolder.toVolumeBinding())
    debuggerVolumes.debuggerLogConfigFolder?.let { add(it.toVolumeBinding()) }
    debuggerVolumes.debuggerLogFolder?.let { add(it.toVolumeBinding()) }
}

internal fun DockerDebugModeAzureInfo.getDebugModeEnvVars() = buildList {
    add(environmentVariables.logFolderVariable.toEnvVar())
    add(environmentVariables.logConfigFolderVariable.toEnvVar())
    add(environmentVariables.azureFunctionsConsoleLoggingEnabled.toEnvVar())
}

internal fun DockerDebugModeAzureInfo.getComposeDebugModeEnvVars() = buildMap {
    put(environmentVariables.logFolderVariable.key, environmentVariables.logFolderVariable.value)
    put(environmentVariables.logConfigFolderVariable.key, environmentVariables.logConfigFolderVariable.value)
    put(
        environmentVariables.azureFunctionsConsoleLoggingEnabled.key,
        environmentVariables.azureFunctionsConsoleLoggingEnabled.value
    )
}

internal fun DockerDebugModeAzureInfo.getDebugModeBuildArgs() = buildList {
    add(buildArgs.buildConfiguration.toEnvVar())
}

internal fun DockerDebugModeAzureInfo.getComposeDebugModeBuildArgs() = buildMap {
    put(buildArgs.buildConfiguration.key, buildArgs.buildConfiguration.value)
}

internal suspend fun DockerDebugModeAzureInfo.getDebuggerEntrypoint(): List<String> {
    val commandLine = DebugProfileStateBase.createWorkerCmdForLauncherInfo(
        ConsoleKind.ExternalConsole,
        debuggerPorts.frontendPorts.containerPort,
        debuggerLauncherInfo,
        ExecutableType.Console,
        clientMode = false,
        usePlugins = false,
        usePty = true,
        "--backend-port=${debuggerPorts.backendPorts.containerPort}",
        "--roslyn-worker-port=${debuggerPorts.roslynWorkerPorts.containerPort}",
        "--timeout=$debuggerTimeout"
    )
    return buildList {
        add(commandLine.exePath)
        addAll(commandLine.parametersList.list)
    }
}