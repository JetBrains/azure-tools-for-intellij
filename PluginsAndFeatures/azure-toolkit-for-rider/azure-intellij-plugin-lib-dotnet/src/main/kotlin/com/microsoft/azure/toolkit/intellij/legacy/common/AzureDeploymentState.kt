/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.common

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

abstract class AzureDeploymentState<T>(
    protected val project: Project,
    private val scope: CoroutineScope
) : RunProfileState {
    protected var processHandlerMessenger: RiderRunProcessHandlerMessager? = null

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val processHandler = RunProcessHandler()
        processHandler.addDefaultListener()
        processHandlerMessenger = RiderRunProcessHandlerMessager(processHandler)
        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        processHandler.startNotify()
        consoleView.attachToProcess(processHandler)

        val job = scope.launch(Dispatchers.Default) {
            try {
                val result = executeSteps(processHandler)
                processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, true)
                onSuccess(result, processHandler)
            }
            catch (ce: CancellationException) {
                processHandlerMessenger?.info("Process was cancelled")
                onFail(ce, processHandler)
            }
            catch (t: Throwable) {
                processHandlerMessenger?.error(t)
                onFail(t, processHandler)
            }
        }

        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                job.cancel()
            }
        })

        return DefaultExecutionResult(consoleView, processHandler)
    }

    protected abstract suspend fun executeSteps(processHandler: RunProcessHandler): T

    protected abstract fun onSuccess(result: T, processHandler: RunProcessHandler)

    private fun onFail(error: Throwable?, processHandler: RunProcessHandler) {
        processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, false)
        processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_EXCEPTION, error)
        processHandler.notifyProcessTerminated(-1)
    }
}