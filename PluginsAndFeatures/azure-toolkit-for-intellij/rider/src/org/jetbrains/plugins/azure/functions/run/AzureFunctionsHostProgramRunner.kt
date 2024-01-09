package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.jetbrains.rider.debugger.DotNetProgramRunner

class AzureFunctionsHostProgramRunner: DotNetProgramRunner() {
    override fun canRun(executorId: String, runConfiguration: RunProfile) =
            executorId == DefaultRunExecutor.EXECUTOR_ID &&
                    runConfiguration is AzureFunctionsHostConfiguration
}