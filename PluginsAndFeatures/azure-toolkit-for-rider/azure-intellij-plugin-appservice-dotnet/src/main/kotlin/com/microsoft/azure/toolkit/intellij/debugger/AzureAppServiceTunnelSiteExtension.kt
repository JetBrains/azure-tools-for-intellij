/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.intellij.xdebugger.impl.ui.attach.dialog.diagnostics.ProcessesFetchingProblemException
import com.microsoft.azure.toolkit.intellij.AppServiceKuduClientExt
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AzureAppServiceTunnelSiteExtension {
    private const val EXTENSION_ID = "JetBrains.Azure.AppService.Tunnel"

    suspend fun throwIfAppServiceTunnelExtensionNotInstalled(appServiceApp: AppServiceAppBase<*, *, *>) {
        withContext(Dispatchers.IO) {
            val kuduManagerExt = appServiceApp.getKuduManagerExt() ?: return@withContext

            val installedPackage = kuduManagerExt.getInstalledPackage(EXTENSION_ID)
                ?: throw debuggingExtensionNotInstalledException(appServiceApp)

            val latestPackage = kuduManagerExt.getPackageFromRemoteStore(EXTENSION_ID)

            if (installedPackage.version != latestPackage.version) {
                throw debuggingExtensionIsOutdatedException(appServiceApp)
            }
        }
    }

    suspend fun installOrUpdate(appServiceApp: AppServiceAppBase<*, *, *>) {
        withContext(Dispatchers.IO) {
            val kuduManagerExt = appServiceApp.getKuduManagerExt() ?: return@withContext
            kuduManagerExt.installOrUpdatePackage(EXTENSION_ID)
            restartKuduAndCheckExtensionInstalled(kuduManagerExt)
        }
    }

    private fun AppServiceAppBase<*, *, *>.getKuduManagerExt(): AppServiceKuduClientExt? {
        val remote = getRemote() ?: return null
        return AppServiceKuduClientExt.getClient(remote)
    }

    private suspend fun restartKuduAndCheckExtensionInstalled(manager: AppServiceKuduClientExt) {
        manager.killKuduProcess()

        for (i in 1..10) {
            delay(500)

            try {
                manager.getInstalledPackage(EXTENSION_ID) ?: throw extensionFailedToInstallException()
                return
            } catch (e: ProcessesFetchingProblemException) {
                throw e
            } catch (e: Throwable) {
                continue
            }
        }

        throw kuduFailedToStartException()
    }
}
