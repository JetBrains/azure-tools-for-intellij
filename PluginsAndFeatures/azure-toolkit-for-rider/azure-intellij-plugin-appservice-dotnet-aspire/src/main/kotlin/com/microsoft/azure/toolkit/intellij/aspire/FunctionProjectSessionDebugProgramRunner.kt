/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.jetbrains.rider.debugger.DotNetDebugRunner
import com.jetbrains.rider.run.configurations.RequiresPreparationRunProfileState

class FunctionProjectSessionDebugProgramRunner : DotNetDebugRunner() {
    companion object {
        const val ID = "aspire.function.project.session.debug.runner"
    }

    override fun getRunnerId() = ID

    override fun canRun(executorId: String, runProfile: RunProfile) =
        executorId == DefaultDebugExecutor.EXECUTOR_ID && runProfile is FunctionProjectSessionDebugProfile

    override suspend fun executeAsync(
        environment: ExecutionEnvironment,
        state: RunProfileState
    ): RunContentDescriptor? {
        if (state is RequiresPreparationRunProfileState) {
            state.prepareExecution(environment)
        }

        return super.executeAsync(environment, state)
    }
}