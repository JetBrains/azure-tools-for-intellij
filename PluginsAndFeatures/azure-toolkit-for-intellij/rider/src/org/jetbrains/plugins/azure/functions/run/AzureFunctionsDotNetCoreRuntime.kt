/**
 * Copyright (c) 2019-2023 JetBrains s.r.o.
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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rider.run.DotNetProcessRunProfileState
import com.jetbrains.rider.run.IDotNetDebugProfileState
import com.jetbrains.rider.run.dotNetCore.DotNetCoreDebugProfile
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RunningAssemblyInfo
import com.jetbrains.rider.runtime.SuspendedAttachableDotNetRuntime
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntimeType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfo
import org.jetbrains.plugins.azure.functions.run.localsettings.FunctionsWorkerRuntime
import kotlin.io.path.absolutePathString

class AzureFunctionsDotNetCoreRuntime(val coreToolsInfo: FunctionsCoreToolsInfo, val workerRuntime: FunctionsWorkerRuntime)
    : DotNetRuntime(DotNetCoreRuntimeType), SuspendedAttachableDotNetRuntime {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun createDebugState(dotNetExecutable: DotNetExecutable, executionEnvironment: ExecutionEnvironment) : IDotNetDebugProfileState {
        return when (workerRuntime) {
            FunctionsWorkerRuntime.DotNetDefault -> DotNetCoreDebugProfile(
                    DotNetCoreRuntime(coreToolsInfo.coreToolsExecutable.absolutePathString()),
                    dotNetExecutable,
                    executionEnvironment,
                    coreToolsInfo.coreToolsExecutable.absolutePathString()
            )
            FunctionsWorkerRuntime.DotNetIsolated -> AzureFunctionsIsolatedDebugProfile(
                    dotNetExecutable,
                    this,
                    executionEnvironment
            )
        }
    }

    override fun createRunState(dotNetExecutable: DotNetExecutable, executionEnvironment: ExecutionEnvironment): RunProfileState {
        return DotNetProcessRunProfileState(dotNetExecutable, this, executionEnvironment)
    }

    override fun patchRunCommandLine(commandLine: GeneralCommandLine, runtimeArguments: List<String>) {
        if (commandLine.exePath.endsWith(".dll", true)) {
            val exePath = commandLine.exePath
            if (commandLine.parametersList.parametersCount > 0 &&
                    commandLine.parametersList[0] != exePath) {
                commandLine.parametersList.prepend(exePath)
            }
            commandLine.exePath = coreToolsInfo.coreToolsExecutable.absolutePathString()
        }
    }

    override fun createDebugAttachSuspendedState(
            runningAssemblyInfo: RunningAssemblyInfo,
            executionEnvironment: ExecutionEnvironment,
            consoleInitializer: (ConsoleViewImpl) -> Unit
    ) = AzureFunctionsDotNetCoreAttachSuspendedProfileState(this, runningAssemblyInfo, executionEnvironment, runningAssemblyInfo.platform, consoleInitializer)
}