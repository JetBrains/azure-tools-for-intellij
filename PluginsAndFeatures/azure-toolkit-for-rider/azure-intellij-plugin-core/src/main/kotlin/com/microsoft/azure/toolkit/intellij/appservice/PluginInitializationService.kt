/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class PluginInitializationService {
    companion object {
        fun getInstance(project: Project): PluginInitializationService = project.service()
    }

    private val isInitialized = AtomicBoolean(false)

    fun setInitialized() {
        isInitialized.store(true)
    }

    fun isInitialized(): Boolean {
        return isInitialized.load()
    }
}
