package com.microsoft.azure.toolkit.intellij.appservice.docker.debug

import com.intellij.openapi.util.Key
import com.jetbrains.rider.plugins.appender.docker.common.DockerContainerType
import com.jetbrains.rider.plugins.appender.docker.debug.providers.RiderDockerDebugProvider

@Suppress("UnstableApiUsage")
internal data class RiderDockerDebugConnectorInfo(
    val dockerDebugProvider: RiderDockerDebugProvider,
    val debuggerFrontendPort: Int,
    val debuggerBackendPort: Int,
    val workingDir: String,
    val executionCommand: List<String>,
    val containerOs: DockerContainerType,
    val containerDotnetVersion: String?
)

internal val DOCKERFILE_DEBUG_CONNECTOR_INFO_KEY: Key<RiderDockerDebugConnectorInfo> =
    Key<RiderDockerDebugConnectorInfo>("DOCKERFILE_DEBUG_CONNECTOR_INFO")
internal val COMPOSE_DEBUG_CONNECTOR_INFO_KEY: Key<Map<String, RiderDockerDebugConnectorInfo>> =
    Key<Map<String, RiderDockerDebugConnectorInfo>>("COMPOSE_DEBUG_CONNECTOR_INFO")