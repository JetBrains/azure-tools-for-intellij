/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.debugger.pings

import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

class FunctionPinger(private val functionApp: FunctionAppBase<*, *, *>, cs: CoroutineScope) {
    private val scope = cs.childScope("App Service Pinger for ${functionApp.name}")

    fun start() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                functionApp.ping()
                delay(30.seconds)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}

