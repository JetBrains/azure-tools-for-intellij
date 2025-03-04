/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import java.util.function.BiConsumer

class AppServiceRemoteDebuggingHandler : BiConsumer<AppServiceAppBase<*, *, *>, AnActionEvent> {
    override fun accept(function: AppServiceAppBase<*, *, *>, event: AnActionEvent) {
        AzureTaskManager.getInstance().runLater {
            AppServiceRemoteDebuggingAction.startDebugging(function, event.project, event.dataContext)
        }
    }
}