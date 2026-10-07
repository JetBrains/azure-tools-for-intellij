/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.view

import com.intellij.xdebugger.impl.ui.attach.dialog.AttachHostItem
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.debugger.asIntellij
import com.microsoft.azure.toolkit.intellij.debugger.attachHosts.AppServiceAttachHost
import com.microsoft.azure.toolkit.intellij.debugger.getIcon
import com.microsoft.azure.toolkit.intellij.debugger.isRunning
import javax.swing.Icon

class AttachAzureAppServiceItem(override val host: AppServiceAttachHost<*>) : AttachHostItem {
    private val appServiceApp = host.appServiceApp

    override fun getId() = appServiceApp.id
    override fun getPresentation() = appServiceApp.name
    override fun getIcon(): Icon = appServiceApp.getIcon() ?: AzureIcons.Common.AZURE.asIntellij()
    override fun toString() = getPresentation()
    fun isRunning() = appServiceApp.isRunning()

    override fun equals(other: Any?): Boolean {
        if (other !is AttachAzureAppServiceItem) {
            return false
        }
        return appServiceApp.id == other.appServiceApp.id
    }

    override fun hashCode() = appServiceApp.id.hashCode()
}

