package com.microsoft.azure.toolkit.intellij.appservice.docker

import com.intellij.docker.agent.compose.beans.DockerComposePort
import com.intellij.docker.agent.settings.DockerEnvVarImpl
import com.intellij.docker.agent.settings.DockerPortBindingImpl
import com.intellij.docker.agent.settings.DockerVolumeBindingImpl
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentEnvironmentVariable
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentPortPair
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentVolume

/* Extensions from RiderTransformedDeploymentConfig, they are internal in the rider.intellij.plugin.appender.
   TODO(Remove them) */
internal fun TransformedDeploymentPortPair.toPortBinding(useLoopbackAddress: Boolean) = DockerPortBindingImpl().apply {
    if (useLoopbackAddress) hostIp = "127.0.0.1"
    hostPort = this@toPortBinding.hostPort
    containerPort = this@toPortBinding.containerPort
}

internal fun TransformedDeploymentPortPair.toComposePorts(useLoopbackAddress: Boolean) = DockerComposePort(
    if (useLoopbackAddress) "127.0.0.1" else null,
    this@toComposePorts.hostPort,
    this@toComposePorts.containerPort
)

internal fun TransformedDeploymentVolume.toVolumeBinding() = DockerVolumeBindingImpl(
    containerPath,
    hostPath,
    readOnly
)

internal fun TransformedDeploymentEnvironmentVariable.toEnvVar() = DockerEnvVarImpl(
    key,
    value
)