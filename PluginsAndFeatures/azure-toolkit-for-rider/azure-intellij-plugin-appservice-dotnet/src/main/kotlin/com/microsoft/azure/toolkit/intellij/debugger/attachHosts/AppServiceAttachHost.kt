/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.attachHosts

import com.intellij.execution.process.ProcessInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.remote.RemoteCredentials
import com.jetbrains.rider.debugger.attach.processes.MsClrRemoteAttachableProcessesHost
import com.jetbrains.rider.debugger.attach.remoting.RiderSshAttachHostBase
import com.jetbrains.rider.debugger.attach.remoting.tools.DebuggerTools
import com.jetbrains.rider.debugger.attach.remoting.tools.local.DefaultLocalDebuggerTools
import com.jetbrains.rider.model.RdProcessInfoBase
import com.microsoft.azure.toolkit.intellij.debugger.AzureRemoteDebuggerTools
import com.microsoft.azure.toolkit.intellij.debugger.relay.RelayServerProvider
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

abstract class AppServiceAttachHost<T : AppServiceAppBase<*, *, *>>(project: Project, val appServiceApp: T) :
    RiderSshAttachHostBase(project, lazy { getCredentials(appServiceApp) }) {

    companion object {
        fun getCredentials(appServiceApp: AppServiceAppBase<*, *, *>): RemoteCredentials {
            val relayProvider = service<RelayServerProvider>()
            return relayProvider.getRelayServerFor(appServiceApp).remoteCredentials
        }
    }

    override suspend fun getProcessListAsync(): List<ProcessInfo> {
        prepareApp()
        return super.getProcessListAsync()
    }

    override suspend fun calculateProcesses(debuggerTools: DebuggerTools): List<RdProcessInfoBase> {
        val processesHost = service<MsClrRemoteAttachableProcessesHost>()
        return processesHost.calculateRemoteProcesses(project, debuggerTools)
    }

    override suspend fun createTools(credentials: RemoteCredentials): DebuggerTools {
        val remote = AzureRemoteDebuggerTools.create(credentials, project)
        val local = DefaultLocalDebuggerTools(project, remote.getCpuKind())

        return DebuggerTools(local, remote)
    }

    protected abstract suspend fun prepareApp()
}

