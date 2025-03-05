/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.aspire

import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.ProjectSessionProfile
import com.jetbrains.rider.aspire.sessionHost.projectLaunchers.ProjectSessionRunProfileState
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntime
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import java.nio.file.Path
import javax.swing.Icon

class FunctionProjectSessionRunProfile(
    private val sessionId: String,
    projectPath: Path,
    dotnetExecutable: DotNetExecutable,
    private val dotnetRuntime: DotNetCoreRuntime,
    private val sessionProcessEventListener: ProcessListener,
    private val sessionProcessLifetime: Lifetime,
    aspireHostProjectPath: Path?
) : ProjectSessionProfile(projectPath, dotnetExecutable, aspireHostProjectPath, false) {

    override fun getIcon(): Icon = IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.RUN)

    override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment
    ) = ProjectSessionRunProfileState(
        sessionId,
        dotnetExecutable,
        dotnetRuntime,
        environment,
        sessionProcessEventListener,
        sessionProcessLifetime
    )
}