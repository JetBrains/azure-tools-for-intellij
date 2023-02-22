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

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.impl.local.FileWatcher
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SpinWait
import com.jetbrains.rdclient.util.idea.pumpMessages
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerPlatform
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachStartInfo
import com.jetbrains.rider.run.*
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.apply
import com.microsoft.azuretools.utils.JsonUtils
import com.microsoft.intellij.util.PluginUtil
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.io.File
import java.time.Duration

class AzureFunctionsDotNetCoreIsolatedDebugProfile(
        private val dotNetExecutable: DotNetExecutable,
        private val dotNetRuntime: DotNetRuntime,
        executionEnvironment: ExecutionEnvironment)
    : DebugProfileStateBase(executionEnvironment) {

    companion object {
        private val logger = Logger.getInstance(AzureFunctionsDotNetCoreIsolatedDebugProfile::class.java)
        private val waitDuration = Duration.ofMinutes(1)
        private const val DOTNET_ISOLATED_DEBUG_ARGUMENT = "--dotnet-isolated-debug"
        private const val DOTNET_ENABLE_JSON_OUTPUT_ARGUMENT = "--enable-json-output"
        private const val DOTNET_JSON_OUTPUT_FILE_ARGUMENT = "--json-output-file"
        private val controlCharsRegex = "\u001B\\[[\\d;]*[^\\d;]".toRegex()
    }

    private var processId = 0
    private lateinit var targetProcessHandler: ProcessHandler
    private lateinit var console: ConsoleView

    override suspend fun createWorkerRunInfo(lifetime: Lifetime, helper: DebuggerHelperHost, port: Int): WorkerRunInfo {
        // Launch Azure Functions host process
        launchAzureFunctionsHost()

        // Show progress bar
        ProgressManager.getInstance().run(
                object : Task.Backgroundable(executionEnvironment.project, RiderAzureBundle.message("run_config.run_function_app.debug.progress.starting_debugger"), false) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true

                        SpinWait.spinUntil(lifetime, waitDuration) {
                            processId != 0 || targetProcessHandler.isProcessTerminated
                        }
                    }
                })

        // Wait until we get a process ID (or the process terminates)
        pumpMessages(waitDuration) {
            processId != 0 || targetProcessHandler.isProcessTerminated
        }
        if (targetProcessHandler.isProcessTerminated) {
            logger.warn("Azure Functions host process terminated before the debugger could attach.")

            // Notify user
            PluginUtil.showErrorNotificationProject(
                    executionEnvironment.project,
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_process_terminated"))
        }
        if (processId == 0) {
            logger.warn("Azure Functions host did not return isolated worker process id.")

            // Notify user
            PluginUtil.showErrorNotificationProject(
                    executionEnvironment.project,
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_pid_unspecified"))
        }

        // Create debugger worker info
        return createWorkerRunInfoFor(port, DebuggerWorkerPlatform.AnyCpu)
    }

    private fun launchAzureFunctionsHost() {

        val programParameters = ParametersListUtil.parse(dotNetExecutable.programParameterString)

        // Enable isolated worker debugger
        if (!programParameters.contains(DOTNET_ISOLATED_DEBUG_ARGUMENT)) {
            programParameters.add(DOTNET_ISOLATED_DEBUG_ARGUMENT)
        }

        // We will need to read the worker process PID, so the debugger can later attach to it.
        //
        // In typical scenarios, the PID is written to the process output,
        // which we will read further down this method.
        //
        // However, there are cases where the PID is not printed to console
        // (see https://github.com/Azure/azure-functions-dotnet-worker/issues/900).
        // For those cases, we're adding a backup of writing the PID to a temporary file.

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

        val processListeners = PatchCommandLineExtension.EP_NAME.getExtensions(executionEnvironment.project)
                .map { it.patchRunCommandLine(commandLine, dotNetRuntime, executionEnvironment.project) }

        val commandLineString = commandLine.commandLineString

        targetProcessHandler = TerminalProcessHandler(commandLine)

        logger.info("Starting functions host process with command line: $commandLineString")
        targetProcessHandler.addProcessListener(object : ProcessAdapter() {

            override fun processTerminated(event: ProcessEvent) = logger.info("Process terminated: $commandLineString")

            override fun startNotified(event: ProcessEvent) {
                logger.info("Started functions host process")
                super.startNotified(event)
            }

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val processText = event.text.replace(controlCharsRegex, "")

                // Typical approach: read PID from process output
                if (processId == 0 &&
                        processText.contains("Azure Functions .NET Worker (PID: ", ignoreCase = true) &&
                        processText.contains(") initialized", ignoreCase = true)) {

                    val pidFromLog = processText.substringAfter("PID: ")
                            .dropWhile { !it.isDigit() }
                            .takeWhile { it.isDigit() }
                            .toInt()

                    logger.info("Got functions isolated worker process id from console output.")
                    logger.info("Functions isolated worker process id: $pidFromLog")
                    processId = pidFromLog
                }

                // Case where the PID is emitted to the temporary file
                if (processId == 0) {
                    // Example contents: { "name":"dotnet-worker-startup", "workerProcessId" : 28460 }
                    val pidFileJson = JsonUtils.readJsonFile(tempPidFile)
                    if (pidFileJson != null) {
                        if (pidFileJson.has("workerProcessId")) {
                            val pidFromJson = pidFileJson.get("workerProcessId").asInt

                            logger.info("Got functions isolated worker process id from JSON output.")
                            logger.info("Functions isolated worker process id: $pidFromJson")
                            processId = pidFromJson
                        }
                    }
                }

                // No need to keep listening when the PID is known
                if (processId != 0) {
                    targetProcessHandler.removeProcessListener(this)
                }

                super.onTextAvailable(event, outputType)
            }
        })

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
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler): ExecutionResult {
        throw UnsupportedOperationException("Use overload with lifetime")
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler, lifetime: Lifetime): ExecutionResult {

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

    override suspend fun createModelStartInfo(lifetime: Lifetime): DebuggerStartInfoBase
        = DotNetCoreAttachStartInfo(processId)
}