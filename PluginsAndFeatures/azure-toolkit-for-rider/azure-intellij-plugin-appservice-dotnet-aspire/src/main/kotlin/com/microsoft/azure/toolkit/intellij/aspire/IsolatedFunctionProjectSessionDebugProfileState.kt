/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.run.ConsoleKind
import com.jetbrains.rider.run.DebugProfileStateBase
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.run.kill
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.profileStates.FunctionIsolatedBaseDebugProfileState

class IsolatedFunctionProjectSessionDebugProfileState(
    private val sessionId: String,
    private val dotnetExecutable: DotNetExecutable,
    dotnetRuntime: DotNetCoreRuntime,
    private val sessionProcessEventListener: ProcessListener,
    private val sessionProcessLifetime: Lifetime
) : FunctionIsolatedBaseDebugProfileState(dotnetExecutable, dotnetRuntime) {
    companion object {
        private val LOG = logger<IsolatedFunctionProjectSessionDebugProfileState>()
    }

    override val consoleKind = ConsoleKind.Normal

    override suspend fun prepareExecution(environment: ExecutionEnvironment) {
        val (executionResult, pid) = launchFunctionHostWaitingForDebugger(
            environment,
            sessionProcessEventListener,
            true
        ) ?: return

        functionHostExecutionResult = executionResult

        sessionProcessLifetime.onTerminationIfAlive {
            if (!executionResult.processHandler.isProcessTerminated && !executionResult.processHandler.isProcessTerminated) {
                LOG.trace("Killing Function session process handler (id: $sessionId)")
                executionResult.processHandler.kill()
            }
        }

        val targetProcess = getTargetProcess(pid) ?: return

        val processArchitecture = getPlatformArchitecture(sessionProcessLifetime, pid, environment.project)

        wrappedState = DotNetCoreAttachProfileState(
            targetProcess,
            environment,
            processArchitecture
        )
    }

    override suspend fun createWorkerRunInfo(
        lifetime: Lifetime,
        helper: DebuggerHelperHost,
        port: Int
    ) = DebugProfileStateBase.createWorkerRunInfoForLauncherInfo(
        consoleKind,
        port,
        getLauncherInfo(lifetime, helper),
        dotnetExecutable.executableType,
        dotnetExecutable.usePty
    )
}