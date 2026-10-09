/*
 * Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.ui.OpenPredefinedTerminalActionProvider

internal class AzureCloudShellTerminalActionProvider : OpenPredefinedTerminalActionProvider {
    override fun listOpenPredefinedTerminalActions(project: Project): List<AnAction> =
        listOfNotNull(ActionManager.getInstance().getAction("AzureToolkit.CloudShell.Start"))
}
