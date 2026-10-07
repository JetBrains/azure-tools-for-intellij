/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.attachHosts

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.debugger.operatingSystem
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem

object AzureAttachHostFactory {
    fun create(project: Project, appServiceApp: AppServiceAppBase<*, *, *>): AppServiceAttachHost<*>? {
        return when (appServiceApp.operatingSystem()) {
            OperatingSystem.WINDOWS -> createForWindows(appServiceApp, project)
            OperatingSystem.LINUX -> createForLinux(appServiceApp, project)
            OperatingSystem.DOCKER -> null
        }
    }

    private fun createForLinux(
        appServiceApp: AppServiceAppBase<*, *, *>,
        project: Project
    ) = if (appServiceApp is FunctionApp) null else AppServiceOnLinuxAttachHost(project, appServiceApp)

    private fun createForWindows(
        appServiceApp: AppServiceAppBase<*, *, *>,
        project: Project
    ): AppServiceAttachHost<*> {
        return if (appServiceApp is FunctionApp) FunctionAppOnWindowsAttachHost(project, appServiceApp)
        else AppServiceOnWindowsAttachHost(project, appServiceApp)
    }
}