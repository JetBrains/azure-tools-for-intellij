package com.microsoft.azure.toolkit.intellij.appservice.docker.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.jetbrains.rider.plugins.appender.docker.common.DockerContainerType
import com.jetbrains.rider.plugins.appender.docker.debug.providers.RiderDockerDebugTargetType

internal object RiderDockerStatisticsCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    private val GROUP = EventLogGroup("rider.docker", 1)

    private val PROJECT_TYPE = EventFields.Enum("project_type", ProjectType::class.java)
    private val DEPLOYMENT_TYPE = EventFields.Enum("deployment_type", DeploymentType::class.java)
    private val FAST_MODE_TRANSFORMATION = EventFields.Boolean("fast_mode_applied")
    private val DEBUG_TRANSFORMATION = EventFields.Boolean("debug_applied")
    private val OS_TYPE = EventFields.Enum("os_type", DockerContainerType::class.java) { it.osName }
    private val TARGET_TYPE = EventFields.Enum("target_type", RiderDockerDebugTargetType::class.java) { it.key }

    private val CONFIG_GENERATED = GROUP.registerEvent("config.generated", PROJECT_TYPE)
    private val DEPLOYMENT_TRANSFORMED = GROUP.registerEvent(
        "deployment.transformed", DEPLOYMENT_TYPE, FAST_MODE_TRANSFORMATION,
        DEBUG_TRANSFORMATION
    )
    private val DEBUG_TARGET_INFO = GROUP.registerEvent("target.started", TARGET_TYPE, OS_TYPE)

    fun configFromCsprojCreated() = CONFIG_GENERATED.log(ProjectType.CSPROJ)
    fun configFromDcprojCreated() = CONFIG_GENERATED.log(ProjectType.DCPROJ)

    fun dockerfileDeploymentTransformed(fastModeApplied: Boolean, debugApplied: Boolean) =
        DEPLOYMENT_TRANSFORMED.log(DeploymentType.DOCKERFILE, fastModeApplied, debugApplied)

    fun composeDeploymentTransformed(fastModeApplied: Boolean, debugApplied: Boolean) =
        DEPLOYMENT_TRANSFORMED.log(DeploymentType.COMPOSE, fastModeApplied, debugApplied)

    fun debuggerStartUp(targetType: RiderDockerDebugTargetType, osType: DockerContainerType) =
        DEBUG_TARGET_INFO.log(targetType, osType)

    private enum class ProjectType {
        CSPROJ, DCPROJ
    }

    private enum class DeploymentType {
        DOCKERFILE, COMPOSE
    }
}