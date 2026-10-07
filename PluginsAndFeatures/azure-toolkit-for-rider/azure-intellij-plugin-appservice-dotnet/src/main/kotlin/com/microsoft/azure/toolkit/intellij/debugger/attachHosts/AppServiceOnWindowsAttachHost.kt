/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.attachHosts

import com.intellij.openapi.project.Project
import com.jetbrains.rider.debugger.attach.remoting.RiderSshAttachHostBase
import com.microsoft.azure.toolkit.intellij.debugger.AzureAppServiceTunnelSiteExtension.throwIfAppServiceTunnelExtensionNotInstalled
import com.microsoft.azure.toolkit.intellij.debugger.webSocketsDisabledException
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

open class AppServiceOnWindowsAttachHost<T : AppServiceAppBase<*, *, *>>(project: Project, appServiceApp: T) :
    AppServiceAttachHost<T>(project, appServiceApp) {

    override suspend fun prepareApp() {
        throwIfWebSocketsDisabled()
        throwIfAppServiceTunnelExtensionNotInstalled(appServiceApp)
    }

    override fun createCopy(): RiderSshAttachHostBase {
        return AppServiceOnWindowsAttachHost(project, appServiceApp)
    }

    private fun throwIfWebSocketsDisabled() {
        if (!appServiceApp.webSocketsEnabled()) throw webSocketsDisabledException(appServiceApp)
    }

    private fun AppServiceAppBase<*, *, *>.webSocketsEnabled(): Boolean {
        return remote?.webSocketsEnabled() ?: false
    }
}