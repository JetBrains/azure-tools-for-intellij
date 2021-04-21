/**
 * Copyright (c) 2019-2021 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerPlatform
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DebuggerWorkerModel
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreExeStartInfo
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreInfo
import com.jetbrains.rider.run.*
import com.jetbrains.rider.runtime.DotNetExecutable

class AzureFunctionsDotNetCoreDebugProfile(
        private val functionsRuntime: AzureFunctionsDotNetCoreRuntime,
        private val dotNetExecutable: DotNetExecutable,
        executionEnvironment: ExecutionEnvironment,
        private val coreToolsExecutablePath: String)
    : DebugProfileStateBase(executionEnvironment), IDotNetDebugProfileStateWithExecutable {

    override suspend fun createWorkerRunInfo(lifetime: Lifetime, helper: DebuggerHelperHost, port: Int): WorkerRunInfo {
        val runInfo = WorkerRunInfo(
                createWorkerCmdFor(consoleKind, port, DebuggerWorkerPlatform.AnyCpu).apply {
                    withWorkDirectory(dotNetExecutable.workingDirectoryOrExeFolder)
                })
        return patchCommandLineInPlugins(lifetime, executionEnvironment.project, runInfo)
    }

    override fun startDebuggerWorker(
            workerCmd: GeneralCommandLine,
            protocolModel: DebuggerWorkerModel,
            protocolServerPort: Int,
            projectLifetime: Lifetime
    ): DebuggerWorkerProcessHandler {
        val processHandler = when (consoleKind) {
            ConsoleKind.Normal -> TerminalProcessHandler(workerCmd)
            ConsoleKind.ExternalConsole -> ExternalConsoleMediator.createProcessHandler(workerCmd)
            ConsoleKind.AttachedProcess ->
                return super.startDebuggerWorker(workerCmd, protocolModel, protocolServerPort, projectLifetime)
        }
        return DebuggerWorkerProcessHandler(processHandler, protocolModel, attached, workerCmd.commandLineString, projectLifetime)
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler)
            : ExecutionResult {
        val useExternalConsole = consoleKind == ConsoleKind.ExternalConsole
        val console = createConsole(useExternalConsole, workerProcessHandler.debuggerWorkerRealHandler,
                workerProcessHandler.presentableCommandLine, executionEnvironment.project)
        dotNetExecutable.onBeforeProcessStarted(executionEnvironment.runProfile, workerProcessHandler)
        return DefaultExecutionResult(console, workerProcessHandler)
    }

    override val consoleKind: ConsoleKind = if (dotNetExecutable.useExternalConsole)
        ConsoleKind.ExternalConsole else ConsoleKind.Normal

    override fun checkBeforeExecution() {
        dotNetExecutable.validate()
    }

    override suspend fun createModelStartInfo(lifetime: Lifetime): DebuggerStartInfoBase {
        return DotNetCoreExeStartInfo(
                DotNetCoreInfo(coreToolsExecutablePath),
                dotNetExecutable.exePath,
                dotNetExecutable.workingDirectory,
                dotNetExecutable.programParameterString,
                functionsRuntime.prepareEnvVars(dotNetExecutable.environmentVariables).toModelMap,
                dotNetExecutable.runtimeArguments,
                dotNetExecutable.executeAsIs,
                dotNetExecutable.useExternalConsole,
                false)
    }

    override val attached: Boolean = false

    override fun getExecutable(): DotNetExecutable = dotNetExecutable
}