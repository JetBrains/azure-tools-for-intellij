/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceKuduClient
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class LinuxAppServiceTunnel(private val appServiceApp: AppServiceAppBase<*, *, *>) {
    suspend fun waitUntilStarted() {
        val kuduClient = appServiceApp.kuduManager ?: throw cantDetermineTunnelStatusException()

        withContext(Dispatchers.IO) {
            kuduClient.wakeUp()

            for (attempt in 1..10) {
                if (isReady(kuduClient.appServiceTunnelStatus)) break
                delay(500)
            }
        }
    }

    // Do a fake call to wake Kudu up
    private fun AppServiceKuduClient.wakeUp() = deploymentLog

    private fun isReady(status: TunnelStatus): Boolean {
        return when (status.state.lowercase()) {
            "starting" -> false
            "stopped" -> throw cantReachSshEndpointException()
            "started" -> {
                if (status.isCanReachPort) true
                else throw sshIsDisabledException()
            }
            else -> throw IllegalArgumentException("Unknown ssh tunnel status received: ${status.state}")
        }
    }
}