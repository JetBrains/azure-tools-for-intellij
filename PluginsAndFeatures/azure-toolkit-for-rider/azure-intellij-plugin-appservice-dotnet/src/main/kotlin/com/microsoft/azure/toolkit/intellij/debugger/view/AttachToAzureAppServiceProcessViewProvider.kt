/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.debugger.view

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.attach.XAttachDebuggerProvider
import com.intellij.xdebugger.attach.XAttachHost
import com.intellij.xdebugger.attach.XAttachHostProvider
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachDialogState
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachToProcessView
import com.intellij.xdebugger.impl.ui.attach.dialog.extensions.XAttachToProcessViewProvider
import com.intellij.xdebugger.impl.ui.attach.dialog.items.columns.AttachDialogColumnsLayout

class AttachToAzureAppServiceProcessViewProvider : XAttachToProcessViewProvider {
    override fun getProcessView(
        project: Project,
        state: AttachDialogState,
        columnsLayout: AttachDialogColumnsLayout,
        attachDebuggerProviders: List<XAttachDebuggerProvider>,
        attachHostProviders: List<XAttachHostProvider<out XAttachHost>>
    ): AttachToProcessView {
        return AttachToAzureAppServiceProcessView(project, state, columnsLayout, attachDebuggerProviders)
    }
}

