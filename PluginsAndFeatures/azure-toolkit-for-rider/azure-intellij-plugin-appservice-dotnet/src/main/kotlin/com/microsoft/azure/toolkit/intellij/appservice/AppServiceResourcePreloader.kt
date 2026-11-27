/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Warms up the Azure Toolkit caches for App Service resources.
 *
 * Listing apps returns only their identifiers, and every app requires a separate request to load its details.
 * The loaders fetch these details in parallel, so the following synchronous calls are served from the cache.
 *
 * Failures of the resource lists are propagated because the toolkit doesn't retry a failed list request.
 * Failures of the individual resources are logged and skipped because the next access to the resource retries it.
 *
 * The service is application-level because the toolkit caches are shared by all projects,
 * so the parallel requests are limited for the whole IDE.
 */
@Service(Service.Level.APP)
internal class AppServiceResourcePreloader {
    companion object {
        fun getInstance(): AppServiceResourcePreloader = service()

        private val LOG = logger<AppServiceResourcePreloader>()

        private const val MAX_PARALLEL_REQUESTS = 16
    }

    // Toolkit calls block the thread, so a separate limited view keeps them from occupying the whole IO pool
    private val dispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_REQUESTS)

    suspend fun preloadAppServicePlans() {
        withContext(dispatcher) {
            val appServicePlans = Azure.az(AzureAppService::class.java).plans()
            appServicePlans.forEach { appServicePlan ->
                launch { loadResource(appServicePlan, "details") { it.remote } }
            }
        }
    }

    suspend fun preloadWebApps(withSlots: Boolean) {
        withContext(dispatcher) {
            val webApps = Azure.az(AzureWebApp::class.java).webApps()
            webApps.forEach { webApp ->
                launch { loadApp(webApp, withSlots) { it.slots().list() } }
            }
        }
    }

    suspend fun preloadFunctionApps(withSlots: Boolean) {
        withContext(dispatcher) {
            val functionApps = Azure.az(AzureFunctions::class.java).functionApps()
            functionApps.forEach { functionApp ->
                launch { loadApp(functionApp, withSlots) { it.slots().list() } }
            }
        }
    }

    // Slots are loaded after the app because the slot module needs the remote app
    private fun <T : AppServiceAppBase<*, *, *>> loadApp(app: T, withSlots: Boolean, loadSlots: (T) -> Unit) {
        val isLoaded = loadResource(app, "details") { it.remote }
        if (isLoaded && withSlots) loadResource(app, "deployment slots", loadSlots)
    }

    private fun <T : AbstractAzResource<*, *, *>> loadResource(resource: T, part: String, load: (T) -> Unit): Boolean {
        return try {
            load(resource)
            true
        } catch (e: Exception) {
            rethrowControlFlowException(e)
            LOG.debug("Unable to preload $part of Azure resource ${resource.id}", e)
            false
        }
    }
}
