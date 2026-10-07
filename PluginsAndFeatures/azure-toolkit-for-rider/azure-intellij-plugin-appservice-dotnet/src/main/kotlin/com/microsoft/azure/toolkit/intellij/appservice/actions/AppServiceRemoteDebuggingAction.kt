/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.actions

import com.intellij.openapi.actionSystem.CustomizedDataContext
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.attach.XAttachDebuggerProvider
import com.intellij.xdebugger.attach.XAttachHost
import com.intellij.xdebugger.attach.XAttachHostProvider
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachToProcessDialogFactory
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachToProcessViewWithHosts.Companion.DEFAULT_ATTACH_HOST
import com.microsoft.azure.toolkit.intellij.debugger.AzureAttachDialogHostType
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

object AppServiceRemoteDebuggingAction {
    fun startDebugging(appService: AppServiceAppBase<*, *, *>, project: Project?, dataContext: DataContext) {
        val factory = project?.service<AttachToProcessDialogFactory>() ?: return

        factory.showDialog(
            XAttachDebuggerProvider.EP.extensionList,
            XAttachHostProvider.EP.extensionList.filterIsInstance<XAttachHostProvider<XAttachHost>>(),
            CustomizedDataContext.withSnapshot(dataContext) { sink ->
                sink[AttachToProcessDialogFactory.DEFAULT_VIEW_HOST_TYPE] = AzureAttachDialogHostType
                sink[DEFAULT_ATTACH_HOST] = appService.id
            }
        )
    }
}