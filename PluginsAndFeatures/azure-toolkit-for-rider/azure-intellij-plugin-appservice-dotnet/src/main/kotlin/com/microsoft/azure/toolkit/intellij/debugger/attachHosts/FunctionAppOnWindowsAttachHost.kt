/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.attachHosts

import com.intellij.openapi.project.Project
import com.jetbrains.rider.debugger.attach.remoting.RiderSshAttachHostBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp

class FunctionAppOnWindowsAttachHost(project: Project, functionApp: FunctionApp) :
    AppServiceOnWindowsAttachHost<FunctionApp>(project, functionApp) {

    override suspend fun prepareApp() {
        super.prepareApp()

        // Ping the Azure Function once so that, when we list its processes, the process running it is active
        // Note that we continue pinging the function when we start debugging it, see AzureAppServicePingingService
        appServiceApp.ping()
    }

    override fun createCopy(): RiderSshAttachHostBase {
        return FunctionAppOnWindowsAttachHost(project, appServiceApp)
    }
}