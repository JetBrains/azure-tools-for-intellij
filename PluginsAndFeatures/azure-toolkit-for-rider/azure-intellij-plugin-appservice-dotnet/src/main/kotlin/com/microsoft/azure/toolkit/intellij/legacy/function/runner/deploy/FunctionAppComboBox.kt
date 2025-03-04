/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("InvalidBundleOrProperty")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.appservice.components.AppServiceComboBoxDotNetRender
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppConfigProducer
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.function.Supplier
import java.util.stream.Collectors

class FunctionAppComboBox(project: Project) : AppServiceComboBox<FunctionAppConfig>(project) {
    var targetProjectOnNetFramework: Boolean = false

    init {
        setRenderer(AppServiceComboBoxDotNetRender())
    }

    override fun refreshItems() {
        Azure.az(AzureFunctions::class.java).refresh()
        super.refreshItems()
    }

    override fun loadAppServiceModels(): MutableList<FunctionAppConfig> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            return mutableListOf()
        }

        return Azure.az(AzureFunctions::class.java)
            .functionApps()
            .parallelStream()
            .map { functionApp -> convertAppServiceToConfig({ FunctionAppConfig() }, functionApp) }
            .filter { a -> a.subscriptionId != null }
            .sorted { a, b -> a.appName().compareTo(b.appName(), true) }
            .collect(Collectors.toList())
    }

    override fun convertAppServiceToConfig(
        supplier: Supplier<FunctionAppConfig>,
        appService: AppServiceAppBase<*, *, *>?
    ): FunctionAppConfig {
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
        val dialog = FunctionAppCreationDialog(project, targetProjectOnNetFramework)
        Disposer.register(this, dialog)
        dialog.data = FunctionAppConfigProducer.getInstance().generateDefaultConfig()
        val actionId: Action.Id<FunctionAppConfig> = Action.Id.of("user/function.create_app.app")
        dialog.setOkAction(
            Action(actionId)
                .withLabel("Create")
                .withIdParam(FunctionAppConfig::appName)
                .withSource { it }
                .withAuthRequired(false)
                .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.functionAppComboBox(project: Project): Cell<FunctionAppComboBox> {
    val component = FunctionAppComboBox(project)
    return cell(component)
}