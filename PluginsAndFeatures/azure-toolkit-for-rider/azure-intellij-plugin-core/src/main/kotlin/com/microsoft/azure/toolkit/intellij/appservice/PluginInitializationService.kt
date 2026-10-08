/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class PluginInitializationService {
    companion object {
        fun getInstance(project: Project): PluginInitializationService = project.service()
    }

    private val initialization = CompletableDeferred<Unit>()

    fun setInitialized() {
        initialization.complete(Unit)
    }

    fun setInitializationFailed(cause: Throwable) {
        initialization.completeExceptionally(cause)
    }

    suspend fun awaitInitialized() {
        initialization.await()
    }

    fun isInitialized(): Boolean {
        return initialization.isCompleted && !initialization.isCancelled
    }
}
