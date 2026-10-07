/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.attachHosts

import com.intellij.openapi.project.Project
import com.jetbrains.rider.debugger.attach.remoting.RiderSshAttachHostBase
import com.microsoft.azure.toolkit.intellij.debugger.LinuxAppServiceTunnel
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

open class AppServiceOnLinuxAttachHost<T: AppServiceAppBase<*, *, *>>(project: Project, appServiceApp: T) :
    AppServiceAttachHost<T>(project, appServiceApp) {

    private val linuxAppServiceTunnel = LinuxAppServiceTunnel(appServiceApp)

    override suspend fun prepareApp() {
        linuxAppServiceTunnel.waitUntilStarted()
    }

    override fun createCopy(): RiderSshAttachHostBase {
        return AppServiceOnLinuxAttachHost(project, appServiceApp)
    }
}

