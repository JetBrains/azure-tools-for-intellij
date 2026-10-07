/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.execution.RunManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionDeploymentConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer.FunctionAppContainerConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppConfiguration
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer.WebAppContainerConfiguration
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LoadAppServicesActivity : ProjectActivity {
    companion object {
        private val LOG = logger<LoadAppServicesActivity>()
    }

    override suspend fun execute(project: Project) {
        try {
            val hasDeploymentConfigurations = RunManager.getInstance(project)
                .allConfigurationsList
                .any {
                    it is WebAppConfiguration ||
                            it is WebAppContainerConfiguration ||
                            it is FunctionDeploymentConfiguration ||
                            it is FunctionAppContainerConfiguration
                }
            if (!hasDeploymentConfigurations) return

            var isInitialized = false
            for (i in 1..5) {
                val initializationService = PluginInitializationService.getInstance(project)
                if (initializationService.isInitialized()) {
                    isInitialized = true
                    break
                }
                delay(500.milliseconds)
            }

            if (!isInitialized) {
                LOG.trace("Machine store not initialized after 5 attempts")
                return
            }

            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) return

            withBackgroundProgress(project, "Preloading Azure resources") {
                LOG.trace("Loading Azure App Services")
                coroutineScope {
                    launch { loadAppServicePlans() }
                    launch { loadWebApps() }
                    launch { loadFunctionApps() }
                }
            }
        } catch (_: Exception) {
            //User isn't logged in, do nothing
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
                launch {
                    it.slots().list()
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
                launch {
                    it.slots().list()
                }
            }
        }
    }
}