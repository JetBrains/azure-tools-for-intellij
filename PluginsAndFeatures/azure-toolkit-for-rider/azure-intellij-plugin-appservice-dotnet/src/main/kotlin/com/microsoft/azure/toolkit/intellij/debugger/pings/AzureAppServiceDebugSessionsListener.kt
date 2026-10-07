/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.pings

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManagerListener

class AzureAppServiceDebugSessionsListener : XDebuggerManagerListener {
    override fun processStarted(debugProcess: XDebugProcess) {
        val pingingService = AzureAppServicePingingService.getInstance(debugProcess.session.project)
        pingingService.debuggingStarted(debugProcess)
    }

    override fun processStopped(debugProcess: XDebugProcess) {
        val pingingService = AzureAppServicePingingService.getInstance(debugProcess.session.project)
        pingingService.debuggingStopped(debugProcess)
    }
}