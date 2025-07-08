/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LoadAppServicesActivity : ProjectActivity {
    companion object {
        private val LOG = logger<LoadAppServicesActivity>()
    }

    override suspend fun execute(project: Project) {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) return

        LOG.trace("Loading Azure App Services")
        coroutineScope {
            launch { loadAppServicePlans() }
            launch { loadWebApps() }
            launch { loadFunctionApps() }
        }
    }

    private suspend fun loadAppServicePlans() {
        LOG.trace("Loading Azure App Service plans")
        val appServicePlans = Azure.az(AzureAppService::class.java).plans()
        coroutineScope {
            appServicePlans.forEach {
                launch {
                    it.remote
                }
            }
        }
    }

    private suspend fun loadWebApps() {
        LOG.trace("Loading Azure Web Apps")
        val webApps = Azure.az(AzureWebApp::class.java).webApps()
        coroutineScope {
            webApps.forEach {
                launch {
                    it.remote
                }
            }
        }
    }

    private suspend fun loadFunctionApps() {
        LOG.trace("Loading Azure Function Apps")
        val functionApps = Azure.az(AzureFunctions::class.java).functionApps()
        coroutineScope {
            functionApps.forEach {
                launch {
                    it.remote
                }
            }
        }
    }
}