/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.docker.debug

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.jetbrains.rider.model.RdProjectDebugModePropertiesResponse
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachStartInfo
import com.jetbrains.rider.plugins.appender.docker.common.RiderRunnableProjectDockerfileMappingService
import com.jetbrains.rider.plugins.appender.docker.debug.providers.RiderDockerDebugProvider
import com.jetbrains.rider.plugins.appender.docker.debug.providers.RiderDockerDebugTargetType
import com.jetbrains.rider.plugins.appender.docker.deployment.RiderDockerDeploymentModel
import java.io.File

/**
 * Debug provider for Azure Functions running in Docker.
 *
 * Azure Functions containers start the application indirectly via `func start`,
 * which spawns the actual .NET worker process. The worker PID is written by
 * `func` to a JSON file. This provider waits for that file, extracts the
 * `workerProcessId`, and creates a debugger attach configuration so Rider
 * can attach the debugger to the running .NET worker process.
 */
class DockerDebugModeAzureProvider : RiderDockerDebugProvider {
    companion object {
        internal const val DEBUGGER_KEY = "docker-azure-netcore"

        private const val WORKER_PROCESS_ID_FIELD = "workerProcessId"

        private const val TIMEOUT_MS = 30_000L
        private const val POLL_INTERVAL_MS = 300L

        private val LOG = logger<DockerDebugModeAzureProvider>()
    }

    override val priority = 0

    override val debuggerKey = DEBUGGER_KEY

    override val debuggerTargetType = RiderDockerDebugTargetType.NET_CORE

    override fun isApplicable(
        deploymentModel: RiderDockerDeploymentModel,
        projectProperties: RdProjectDebugModePropertiesResponse?
    ): Boolean {
        // Temporary true
        return true
    }

    override fun getDebuggerStartInfo(
        executionCommand: List<String>,
        workingDir: String,
        dotnetVersion: String?,
        environment: ExecutionEnvironment,
    ): DebuggerStartInfoBase? {
        if (executionCommand.isEmpty()) {
            LOG.trace { "Execution command is empty. Unable to determine PID file." }
            return null
        }

        val fileWithPid = executionCommand.first()
        val file = File(fileWithPid)
        val mapper = ObjectMapper()

        LOG.trace { "Waiting for Azure Functions worker PID in file: $fileWithPid" }

        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            if (file.exists()) {
                try {
                    val lines = file.readLines()

                    for (line in lines.asReversed()) {
                        val node = mapper.readTree(line)
                        val pid = node.get(WORKER_PROCESS_ID_FIELD)?.asInt()

                        if (pid != null) {
                            LOG.trace { "Detected Azure Functions worker PID: $pid" }
                            return DotNetCoreAttachStartInfo(pid)
                        }
                    }
                } catch (e: Exception) {
                    LOG.trace(e)
                }
            }

            Thread.sleep(POLL_INTERVAL_MS)
        }

        LOG.warn("Timeout waiting for Azure Functions worker PID in file: $fileWithPid")
        return null
    }
}