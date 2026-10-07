/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.impl.ui.attach.dialog.diagnostics.ProcessesFetchingProblemException
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

fun webSocketsDisabledException(appServiceApp: AppServiceAppBase<*, *, *>) : ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "WebSockets should be enabled for debugging",
        enableWebSocketsAction(appServiceApp))
}

fun debuggingExtensionNotInstalledException(appServiceApp: AppServiceAppBase<*, *, *>) : ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "Debugging extension is not installed",
        installDebuggingExtensionAction(appServiceApp))
}

fun debuggingExtensionIsOutdatedException(appServiceApp: AppServiceAppBase<*, *, *>) : ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "Debugging extension is outdated",
        installDebuggingExtensionAction(appServiceApp))
}

fun cantReachSshEndpointException(): ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "Couldn't reach SSH endpoint, please check that your app is running",
        null
    )
}

fun sshIsDisabledException(): ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "SSH is not enabled for this app. To enable SSH follow this instructions:",
        browseLinkAction("https://go.microsoft.com/fwlink/?linkid=2132395")
    )
}

fun cantDetermineTunnelStatusException(): ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "Status of the App Service tunnel couldn't be determined",
        null)
}

fun extensionFailedToInstallException(): ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "Couldn't install debugging extension",
        null)
}

fun kuduFailedToStartException(): ProcessesFetchingProblemException {
    return ProcessesFetchingProblemException(
        AllIcons.General.Warning,
        "Kudu failed to start",
        null)
}