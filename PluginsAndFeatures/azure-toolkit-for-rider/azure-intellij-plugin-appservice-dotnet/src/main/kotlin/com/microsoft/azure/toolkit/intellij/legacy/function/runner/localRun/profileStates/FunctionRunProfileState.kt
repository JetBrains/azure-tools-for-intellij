/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.profileStates

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.jetbrains.rider.run.ConsoleKind
import com.jetbrains.rider.run.TerminalProcessHandler
import com.jetbrains.rider.run.createConsole
import com.jetbrains.rider.run.createRunCommandLine
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import kotlin.io.path.Path

class FunctionRunProfileState(
    private val dotNetExecutable: DotNetExecutable,
    private val dotNetRuntime: DotNetRuntime,
    private val environment: ExecutionEnvironment
) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult {
        dotNetExecutable.validate()

        val commandLine = dotNetExecutable.createRunCommandLine(dotNetRuntime)
        val originalExecutable = Path(commandLine.exePath)
        val processHandler = TerminalProcessHandler(
            environment.project,
            commandLine,
            commandLine.commandLineString,
            originalExecutable = originalExecutable
        )
        val console = createConsole(
            ConsoleKind.Normal,
            processHandler,
            environment.project
        )

        //Start browser action
        dotNetExecutable.onBeforeProcessStarted(environment, environment.runProfile, processHandler)

        return DefaultExecutionResult(console, processHandler)
    }
}