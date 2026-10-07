/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.ide.BrowserUtil
import com.intellij.xdebugger.impl.ui.attach.dialog.diagnostics.ProcessesFetchingProblemAction
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

fun installDebuggingExtensionAction(appServiceApp: AppServiceAppBase<*, *, *>): ProcessesFetchingProblemAction {
    return ProcessesFetchingProblemAction(
        "rider.azure.install.debugging.extension",
        "Install debugging extension"
    ) { _, _, _ ->
        AzureAppServiceTunnelSiteExtension.installOrUpdate(appServiceApp)
    }
}

fun enableWebSocketsAction(appServiceApp: AppServiceAppBase<*, *, *>): ProcessesFetchingProblemAction {
    return ProcessesFetchingProblemAction(
        "rider.azure.enable.websockets",
        "Enable websockets"
    ) { _, _, _ ->
        appServiceApp.enableWebSockets()
    }
}

fun browseLinkAction(url: String): ProcessesFetchingProblemAction {
    return ProcessesFetchingProblemAction(
        "rider.azure.open.link",
        "Open in browser"
    ) { _, _, _ -> BrowserUtil.browse(url) }
}