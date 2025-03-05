/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.aspire.generated.CreateSessionRequest
import com.jetbrains.rider.aspire.run.AspireHostConfiguration
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.SessionProcessLauncherExtension
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.getAspireHostRunConfiguration
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.getDotNetRuntime
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.Path

class FunctionProjectSessionProcessLauncher : SessionProcessLauncherExtension {
    companion object {
        private val LOG = logger<FunctionProjectSessionProcessLauncher>()
    }

    override val priority = 3

    override suspend fun isApplicable(
        projectPath: String,
        project: Project
    ): Boolean {
        val path = Path(projectPath)
        val runnableProject = project.solution.runnableProjectsModel.findBySessionProject(path)
        return runnableProject != null
    }

    override suspend fun launchRunProcess(
        sessionId: String,
        sessionModel: CreateSessionRequest,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        aspireHostRunConfigName: String?,
        project: Project
    ) {
        LOG.trace { "Starting run session for ${sessionModel.projectPath}" }

        val aspireHostRunConfig = getAspireHostRunConfiguration(aspireHostRunConfigName, project)
        val executable = getDotNetExecutable(
            sessionModel,
            aspireHostRunConfig,
            true,
            project
        ) ?: return
        val runtime = getDotNetRuntime(executable, project) ?: return

        val projectPath = Path(sessionModel.projectPath)
        val aspireHostProjectPath = aspireHostRunConfig?.let { Path(it.parameters.projectFilePath) }

        val profile = getRunProfile(
            sessionId,
            projectPath,
            executable,
            runtime,
            sessionProcessEventListener,
            sessionProcessLifetime,
            aspireHostProjectPath
        )

        val environment = ExecutionEnvironmentBuilder
            .createOrNull(project, DefaultRunExecutor.getRunExecutorInstance(), profile)
            ?.build()
        if (environment == null) {
            LOG.warn("Unable to create run execution environment")
            return
        }

        setProgramCallbacks(environment)

        withContext(Dispatchers.EDT) {
            environment.runner.execute(environment)
        }
    }

    override suspend fun launchDebugProcess(
        sessionId: String,
        sessionModel: CreateSessionRequest,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        aspireHostRunConfigName: String?,
        project: Project
    ) {
        LOG.trace { "Starting debug session for project ${sessionModel.projectPath}" }

        val aspireHostRunConfig = getAspireHostRunConfiguration(aspireHostRunConfigName, project)
        val executable = getDotNetExecutable(
            sessionModel,
            aspireHostRunConfig,
            true,
            project
        ) ?: return
        val runtime = getDotNetRuntime(executable, project) ?: return

        val projectPath = Path(sessionModel.projectPath)
        val aspireHostProjectPath = aspireHostRunConfig?.let { Path(it.parameters.projectFilePath) }

        val profile = getDebugProfile(
            sessionId,
            projectPath,
            executable,
            runtime,
            sessionProcessEventListener,
            sessionProcessLifetime,
            aspireHostProjectPath
        )
        val debugRunner = ProgramRunner.findRunnerById(FunctionProjectSessionDebugProgramRunner.ID)
        if (debugRunner == null) {
            LOG.warn("Unable to find runner: ${FunctionProjectSessionDebugProgramRunner.ID}")
            return
        }

        val environment = ExecutionEnvironmentBuilder
            .createOrNull(project, DefaultDebugExecutor.getDebugExecutorInstance(), profile)
            ?.runner(debugRunner)
            ?.build()
        if (environment == null) {
            LOG.warn("Unable to create debug execution environment")
            return
        }

        setProgramCallbacks(environment)

        withContext(Dispatchers.EDT) {
            environment.runner.execute(environment)
        }
    }

    private suspend fun getDotNetExecutable(
        sessionModel: CreateSessionRequest,
        hostRunConfiguration: AspireHostConfiguration?,
        addBrowserAction: Boolean,
        project: Project
    ): DotNetExecutable? {
        val factory = FunctionSessionExecutableFactory.getInstance(project)
        val executable = factory.createExecutable(sessionModel, hostRunConfiguration, addBrowserAction)
        if (executable == null) {
            LOG.warn("Unable to create executable for project: ${sessionModel.projectPath}")
        }

        return executable
    }

    private fun getRunProfile(
        sessionId: String,
        projectPath: Path,
        dotnetExecutable: DotNetExecutable,
        dotnetRuntime: DotNetCoreRuntime,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        aspireHostProjectPath: Path?
    ) = FunctionProjectSessionRunProfile(
        sessionId,
        projectPath,
        dotnetExecutable,
        dotnetRuntime,
        sessionProcessEventListener,
        sessionProcessLifetime,
        aspireHostProjectPath
    )

    private fun getDebugProfile(
        sessionId: String,
        projectPath: Path,
        dotnetExecutable: DotNetExecutable,
        dotnetRuntime: DotNetCoreRuntime,
        sessionProcessEventListener: ProcessListener,
        sessionProcessLifetime: Lifetime,
        aspireHostProjectPath: Path?
    ) = FunctionProjectSessionDebugProfile(
        sessionId,
        projectPath,
        dotnetExecutable,
        dotnetRuntime,
        sessionProcessEventListener,
        sessionProcessLifetime,
        aspireHostProjectPath
    )

    private fun setProgramCallbacks(environment: ExecutionEnvironment) {
        environment.callback = object : ProgramRunner.Callback {
            override fun processStarted(runContentDescriptor: RunContentDescriptor?) {
                runContentDescriptor?.apply {
                    isActivateToolWindowWhenAdded = false
                    isAutoFocusContent = false
                }
            }
        }
    }
}