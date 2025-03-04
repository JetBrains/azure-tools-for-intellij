/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.process.ProcessInfo
import com.intellij.xdebugger.attach.LocalAttachHost
import com.jetbrains.rider.debugger.attach.RiderAttachDebuggerBase
import com.jetbrains.rider.debugger.attach.remoting.RiderSshAttachHostBase
import com.jetbrains.rider.model.RdProcessInfoBase
import com.microsoft.azure.toolkit.intellij.debugger.attachHosts.AppServiceAttachHost

class AzureAppServiceAttachDebugger(
    private val debugger: RiderAttachDebuggerBase<RdProcessInfoBase>,
    rdProcessInfo: RdProcessInfoBase
) :
    RiderAttachDebuggerBase<RdProcessInfoBase>(rdProcessInfo) {
    override fun getDebuggerDisplayName(): String = debugger.debuggerDisplayName

    override fun createLocalAttachProfile(
        processInfo: ProcessInfo,
        rdProcessInfo: RdProcessInfoBase,
        localAttachHost: LocalAttachHost
    ): RunProfile {
        throw UnsupportedOperationException("AzureAppServiceAttachDebugger supports only remote debugging")
    }

    override fun createRemoteAttachProfile(
        processInfo: ProcessInfo,
        rdProcessInfo: RdProcessInfoBase,
        remoteAttachHost: RiderSshAttachHostBase
    ): RunProfile {
        if (remoteAttachHost !is AppServiceAttachHost<*>)
            throw IllegalArgumentException("RiderAzureAppServiceAttachHost expected, got ${remoteAttachHost.javaClass.name}")

        val innerProfile = debugger.createRemoteAttachProfile(processInfo, rdProcessInfo, remoteAttachHost)
        return AzureAppServiceAttachProfile(remoteAttachHost, innerProfile)
    }
}