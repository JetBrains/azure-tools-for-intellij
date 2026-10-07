/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.execution.process.ProcessInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.xdebugger.attach.XAttachDebugger
import com.intellij.xdebugger.attach.XAttachDebuggerProvider
import com.intellij.xdebugger.attach.XAttachHost
import com.intellij.xdebugger.impl.ui.attach.dialog.extensions.XAttachDialogUiInvisibleDebuggerProvider
import com.jetbrains.rider.debugger.attach.RiderAttachDebuggerBase
import com.jetbrains.rider.debugger.attach.RiderRemoteProcessInfo
import com.jetbrains.rider.debugger.attach.dotnet.MsClrAttachProvider
import com.jetbrains.rider.model.RdProcessInfoBase
import com.microsoft.azure.toolkit.intellij.debugger.attachHosts.AppServiceAttachHost

class RiderAzureAttachDebuggerProvider : XAttachDebuggerProvider, XAttachDialogUiInvisibleDebuggerProvider {
    override fun isAttachHostApplicable(xAttachHost: XAttachHost): Boolean = xAttachHost is AppServiceAttachHost<*>

    override fun getAvailableDebuggers(
        project: Project,
        hostInfo: XAttachHost,
        process: ProcessInfo,
        contextHolder: UserDataHolder
    ): MutableList<out XAttachDebugger> {
        if (process !is RiderRemoteProcessInfo) {
            return mutableListOf()
        }

        val clrAttachProvider = XAttachDebuggerProvider.EP.findExtension(MsClrAttachProvider::class.java)
            ?: return mutableListOf()

        return clrAttachProvider.getAvailableDebuggers(project, hostInfo, process, contextHolder)
            .filterIsInstance<RiderAttachDebuggerBase<RdProcessInfoBase>>()
            .map { AzureAppServiceAttachDebugger(it, process.rdProcessInfo) }
            .toMutableList()
    }
}