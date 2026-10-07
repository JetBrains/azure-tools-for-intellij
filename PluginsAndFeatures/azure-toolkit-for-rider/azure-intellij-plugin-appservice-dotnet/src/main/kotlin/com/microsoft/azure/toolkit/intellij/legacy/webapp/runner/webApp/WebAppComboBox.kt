/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("InvalidBundleOrProperty")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.appservice.components.AppServiceComboBoxDotNetRender
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.function.Supplier

open class WebAppComboBox(project: Project) : AppServiceComboBox<AppServiceConfig>(project) {
    companion object {
        private val LOG = logger<WebAppComboBox>()
    }

    var targetProjectOnNetFramework: Boolean = false

    init {
        setRenderer(AppServiceComboBoxDotNetRender())
    }

    override fun refreshItems() {
        try {
            Azure.az(AzureWebApp::class.java).refresh()
            super.refreshItems()
        } catch (e: Exception) {
            LOG.error("Error while refreshing items", e)
            throw e
        }
    }

    override fun loadAppServiceModels(): MutableList<AppServiceConfig> {
        try {
            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) {
                return mutableListOf()
            }

            val webApps = Azure.az(AzureWebApp::class.java).webApps()

            val modifiedWebApps = buildList {
                for (webApp in webApps.sortedBy { it.name }) {
                    val config = convertAppServiceToConfig({ AppServiceConfig() }, webApp)
                    add(config)
                }
            }

            return modifiedWebApps.toMutableList()
        } catch (e: Exception) {
            LOG.error("Unable to load models", e)
            throw e
        }
    }

    @Suppress("DuplicatedCode")
    override fun convertAppServiceToConfig(
        supplier: Supplier<AppServiceConfig>,
        appService: AppServiceAppBase<*, *, *>?
    ): AppServiceConfig {
        val config = supplier.get()
        if (appService == null) return config

        config.apply {
            subscriptionId = appService.subscriptionId
            resourceGroup = appService.resourceGroupName
            appName = appService.name
            region = appService.region
            runtime = RuntimeConfig().apply {
                os = OperatingSystem.fromString(appService.remote?.operatingSystem()?.name)
            }
            val servicePlan = appService.appServicePlan
            servicePlan?.also {
                pricingTier = it.pricingTier
                servicePlanName = it.name
                servicePlanResourceGroup = it.resourceGroupName
            }
        }

        return config
    }

    override fun createResource() {
        val dialog = WebAppCreationDialog(project, targetProjectOnNetFramework)
        Disposer.register(this, dialog)
        setOkActionAndShowDialog(dialog)
    }

    protected fun setOkActionAndShowDialog(dialog: ConfigDialog<AppServiceConfig>) {
        val actionId: Action.Id<AppServiceConfig> = Action.Id.of("user/webapp.create_app.app")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(AppServiceConfig::appName)
                .withSource { it }
                .withAuthRequired(false)
                .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.webAppComboBox(project: Project): Cell<WebAppComboBox> {
    val component = WebAppComboBox(project)
    return cell(component)
}