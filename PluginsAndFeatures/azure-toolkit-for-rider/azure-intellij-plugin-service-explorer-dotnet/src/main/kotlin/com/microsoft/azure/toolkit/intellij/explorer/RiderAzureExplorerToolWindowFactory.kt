/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.explorer

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.microsoft.azure.toolkit.intellij.AzureToolkitService
import com.microsoft.azure.toolkit.intellij.appservice.PluginInitializationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class RiderAzureExplorerToolWindowFactory : AzureExplorer.ToolWindowFactory() {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val initializationService = PluginInitializationService.getInstance(project)
        AzureToolkitService.getInstance(project).scope.launch {
            initializationService.awaitInitialized()
            withContext(Dispatchers.EDT) {
                if (!toolWindow.isDisposed) {
                    super.createToolWindowContent(project, toolWindow)
                }
            }
        }
    }
}
