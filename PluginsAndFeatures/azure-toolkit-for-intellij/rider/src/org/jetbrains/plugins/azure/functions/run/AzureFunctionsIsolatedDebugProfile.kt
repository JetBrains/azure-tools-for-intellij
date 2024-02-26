/**
 * Copyright (c) 2021-2023 JetBrains s.r.o.
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

@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.impl.ProcessListUtil
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.rd.util.withBackgroundContext
import com.intellij.openapi.rd.util.withUiContext
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.application
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.system.CpuArch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.debugger.RiderDebuggerBundle
import com.jetbrains.rider.model.DesktopClrRuntime
import com.jetbrains.rider.model.debuggerHelper.PlatformArchitecture
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DotNetClrAttachStartInfo
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachStartInfo
import com.jetbrains.rider.run.*
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.run.dotNetCore.toCPUKind
import com.jetbrains.rider.run.msNet.MsNetAttachProfileState
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.apply
import com.microsoft.azuretools.utils.JsonUtils
import com.microsoft.intellij.util.PluginUtil
import kotlinx.coroutines.delay
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class AzureFunctionsIsolatedDebugProfile(
        private val dotNetExecutable: DotNetExecutable,
        private val dotNetRuntime: DotNetRuntime,
        executionEnvironment: ExecutionEnvironment)
    : DebugProfileStateBase(executionEnvironment) {

    companion object {
        private val logger = Logger.getInstance(AzureFunctionsIsolatedDebugProfile::class.java)
        private val waitDuration = 1.minutes
        private const val DOTNET_ISOLATED_DEBUG_ARGUMENT = "--dotnet-isolated-debug"
        private const val DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT = "--enable-json-output"
        private const val DOTNET_JSON_OUTPUT_FILE_ARGUMENT = "--json-output-file"
    }

    private var processId = 0
    private var isNetFrameworkProcess = false
    private lateinit var targetProcessHandler: ProcessHandler
    private lateinit var console: ConsoleView

    override suspend fun createWorkerRunInfo(lifetime: Lifetime, helper: DebuggerHelperHost, port: Int): WorkerRunInfo {
        // Launch Azure Functions host process
        processId = withBackgroundProgress(executionEnvironment.project, RiderAzureBundle.message("run_config.run_function_app.debug.progress.starting_debugger")) {
            withBackgroundContext {
                launchAzureFunctionsHost()
            }
        } ?: 0

        if (targetProcessHandler.isProcessTerminated) {
            logger.warn("Azure Functions host process terminated before the debugger could attach.")

            // Notify user
            PluginUtil.showErrorNotificationProject(
                    executionEnvironment.project,
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_process_terminated"))
            return super.createWorkerRunInfo(lifetime, helper, port)
        }
        if (processId == 0) {
            logger.warn("Azure Functions host did not return isolated worker process id.")

            // Notify user
            PluginUtil.showErrorNotificationProject(
                    executionEnvironment.project,
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_pid_unspecified"))
            return super.createWorkerRunInfo(lifetime, helper, port)
        }

        // Get process info
        val targetProcess = withBackgroundContext {
            ProcessListUtil.getProcessList().firstOrNull { it.pid == processId }
        }

        if (targetProcess == null) {
            logger.warn("Unable to find target process with pid $processId")
            // Create debugger worker info
            return super.createWorkerRunInfo(lifetime, helper, port)
        }

        // Determine process architecture, and whether it is .NET / .NET Core
        val processExecutablePath = ParametersListUtil.parse(targetProcess.commandLine).firstOrNull()
        val processArchitecture = getPlatformArchitecture(lifetime, processId)
        val processTargetFramework = processExecutablePath?.let {
            DebuggerHelperHost.getInstance(executionEnvironment.project)
                    .getAssemblyTargetFramework(it, lifetime)
        }

        isNetFrameworkProcess = processExecutablePath?.endsWith("dotnet.exe") == false && (processTargetFramework?.isNetFramework ?: false)
        return if (!isNetFrameworkProcess) {
            // .NET Core
            DotNetCoreAttachProfileState(targetProcess, executionEnvironment, processArchitecture)
                    .createWorkerRunInfo(lifetime, helper, port)
        } else {
            // .NET Framework
            val clrRuntime = DesktopClrRuntime("")
            MsNetAttachProfileState(
                    targetProcess,
                    processArchitecture.toCPUKind(),
                    clrRuntime,
                    executionEnvironment,
                    RiderDebuggerBundle.message("MsNetAttachProvider.display.name", clrRuntime.version)
            )
                    .createWorkerRunInfo(lifetime, helper, port)
        }
    }

    private suspend fun getPlatformArchitecture(lifetime: Lifetime, pid: Int): PlatformArchitecture {
        if (SystemInfo.isWindows) {
            return DebuggerHelperHost.getInstance(executionEnvironment.project)
                    .getProcessArchitecture(lifetime, pid)
        }

        return when (CpuArch.CURRENT) {
            CpuArch.X86 -> PlatformArchitecture.X86
            CpuArch.X86_64 -> PlatformArchitecture.X64
            CpuArch.ARM64 -> PlatformArchitecture.Arm64
            else -> PlatformArchitecture.Unknown
        }
    }

    private suspend fun launchAzureFunctionsHost(): Int? {
        application.assertIsNonDispatchThread()

        val programParameters = ParametersListUtil.parse(dotNetExecutable.programParameterString)

        // Enable isolated worker debugger
        if (!programParameters.contains(DOTNET_ISOLATED_DEBUG_ARGUMENT)) {
            programParameters.add(DOTNET_ISOLATED_DEBUG_ARGUMENT)
        }

        // We will need to read the worker process PID, so the debugger can later attach to it.
        //
        // We are using this file to read the PID,
        // (see https://github.com/Azure/azure-functions-dotnet-worker/issues/900).
        //
        // Example contents: { "name":"dotnet-worker-startup", "workerProcessId" : 28460 }

        val tempPidFile = FileUtil.createTempFile(
                File(FileUtil.getTempDirectory()),
                "Rider-AzureFunctions-IsolatedWorker-",
                "json.pid", true, true)

        if (!programParameters.contains(DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT)) {
            programParameters.add(DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT)
        }
        if (!programParameters.contains(DOTNET_JSON_OUTPUT_FILE_ARGUMENT)) {
            programParameters.add(DOTNET_JSON_OUTPUT_FILE_ARGUMENT)
            programParameters.add(tempPidFile.path)
        }

        // Start the Azure Functions host
        val commandLine = dotNetExecutable
                .copy(
                        useExternalConsole = false,
                        programParameterString = ParametersListUtil.join(programParameters)
                )
                .createRunCommandLine(dotNetRuntime)
                .apply(dotNetRuntime, ParametersListUtil.parse(dotNetExecutable.runtimeArguments))

        val processListeners = withUiContext {
            PatchCommandLineExtension.EP_NAME.getExtensions(executionEnvironment.project)
                    .map { it.patchRunCommandLine(commandLine, dotNetRuntime, executionEnvironment.project) }
        }

        val commandLineString = commandLine.commandLineString

        targetProcessHandler = TerminalProcessHandler(executionEnvironment.project, commandLine)

        logger.info("Starting functions host process with command line: $commandLineString")

        processListeners.filterNotNull().forEach { targetProcessHandler.addProcessListener(it) }

        console = createConsole(
                consoleKind = ConsoleKind.Normal,
                processHandler = targetProcessHandler,
                project = executionEnvironment.project)

        if (dotNetExecutable.useExternalConsole) {
            logger.debug("Ignoring for isolated worker: dotNetExecutable.useExternalConsole=${dotNetExecutable.useExternalConsole}")
            console.print(RiderAzureBundle.message("run_config.run_function_app.debug.ignore.externalconsole") + System.lineSeparator(),
                    ConsoleViewContentType.SYSTEM_OUTPUT)
        }

        console.attachToProcess(targetProcessHandler)

        targetProcessHandler.startNotify()

        var timeout = 0.milliseconds
        while (timeout <= waitDuration) {
            val pidFileJson = JsonUtils.readJsonFile(tempPidFile)
            if (pidFileJson != null) {
                if (pidFileJson.has("workerProcessId")) {
                    val pidFromJson = pidFileJson.get("workerProcessId").asInt

                    logger.info("Got functions isolated worker process id from JSON output.")
                    logger.info("Functions isolated worker process id: $pidFromJson")
                    return pidFromJson
                }
            }
            delay(500)
            timeout += 500.milliseconds
        }

        return null
    }

    override suspend fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler): ExecutionResult {
        throw UnsupportedOperationException("Use overload with lifetime")
    }

    override suspend fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler, lifetime: Lifetime):
            ExecutionResult {

        if (processId == 0) {
            // If we do not get pid from the isolated worker process, destroy the process here
            if (!targetProcessHandler.isProcessTerminating && !targetProcessHandler.isProcessTerminated) {
                logger.debug("Destroying Azure Functions host process.")
                targetProcessHandler.destroyProcess()
            }

            // Return console output
            return DefaultExecutionResult(console, targetProcessHandler)
        }

        // Proceed with attaching debugger
        workerProcessHandler.attachTargetProcess(targetProcessHandler)
        return DefaultExecutionResult(console, workerProcessHandler)
    }

    override val consoleKind: ConsoleKind = if (dotNetExecutable.useExternalConsole)
        ConsoleKind.ExternalConsole else ConsoleKind.Normal

    override val attached: Boolean = false

    override suspend fun checkBeforeExecution() {
        dotNetExecutable.validate()
    }

    override suspend fun createModelStartInfo(lifetime: Lifetime): DebuggerStartInfoBase = if (!isNetFrameworkProcess) {
        // .NET Core
        DotNetCoreAttachStartInfo(processId)
    } else {
        // .NET Framework
        DotNetClrAttachStartInfo("", processId)
    }
}