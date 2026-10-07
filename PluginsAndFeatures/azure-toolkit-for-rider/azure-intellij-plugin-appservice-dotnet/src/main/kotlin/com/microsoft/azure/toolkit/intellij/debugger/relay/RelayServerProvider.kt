/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.debugger.relay

import com.intellij.openapi.components.Service
import com.intellij.platform.util.coroutines.childScope
import com.microsoft.azure.toolkit.intellij.debugger.operatingSystem
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class RelayServerProvider(scope: CoroutineScope) {
    private val serversScope = scope.childScope("Relay Servers")
    private val pool = ConcurrentHashMap<String, RelayServer>()

    fun getRelayServerFor(appServiceApp: AppServiceAppBase<*, *, *>): RelayServer {
        return pool.computeIfAbsent(appServiceApp.id) {
            return@computeIfAbsent createServer(appServiceApp).apply {
                serversScope.launch { start() }
            }
        }
    }

    private fun createServer(appServiceApp: AppServiceAppBase<*, *, *>): RelayServer {
        return when (appServiceApp.operatingSystem()) {
            OperatingSystem.WINDOWS -> WindowsRelayServer(appServiceApp)
            OperatingSystem.LINUX -> LinuxRelayServer(appServiceApp)
            OperatingSystem.DOCKER -> throw UnsupportedOperationException("Docker relay server is not supported")
        }
    }
}