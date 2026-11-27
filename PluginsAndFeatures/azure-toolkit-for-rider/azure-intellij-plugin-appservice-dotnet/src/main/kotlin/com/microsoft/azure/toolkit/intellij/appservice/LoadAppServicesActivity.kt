/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.diagnostic.rethrowControlFlowException
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
            val hasDeploymentConfigurations = RunManager.getInstanceAsync(project)
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
                val preloader = AppServiceResourcePreloader.getInstance()
                coroutineScope {
                    launch { preloader.preloadAppServicePlans() }
                    launch { preloader.preloadWebApps(withSlots = true) }
                    launch { preloader.preloadFunctionApps(withSlots = true) }
                }
            }
        } catch (e: Exception) {
            rethrowControlFlowException(e)
            LOG.debug("Unable to preload Azure App Services", e)
        }
    }
}
