/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppConfigProducer
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionAppInfoAdvancedPanel
import com.microsoft.azure.toolkit.intellij.legacy.utils.removeInvalidCharacters
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.SHARED_PRICING
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.IAccountActions
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import javax.swing.JPanel

class FunctionAppContainerCreationDialog(project: Project) : ConfigDialog<FunctionAppConfig>(project), Disposable {
    companion object {
        private val FUNCTION_APP_CONTAINER_PRICING = SHARED_PRICING.union(setOf(
            PricingTier.ELASTIC_PREMIUM_EP1,
            PricingTier.ELASTIC_PREMIUM_EP2,
            PricingTier.ELASTIC_PREMIUM_EP3))
    }

    private val basicPanel: AppServiceInfoBasicPanel<FunctionAppConfig>
    private val advancedPanel: FunctionAppInfoAdvancedPanel
    private val panel: JPanel

    init {
        val selectedSubscriptions = Azure.az(AzureAccount::class.java).account().selectedSubscriptions
        if (selectedSubscriptions.isEmpty()) {
            this.close()
            throw AzureToolkitRuntimeException(
                "There are no subscriptions selected in your account.",
                IAccountActions.SELECT_SUBS
            )
        }

        val projectName = removeInvalidCharacters(project.name)
        basicPanel = AppServiceInfoBasicPanel {
            FunctionAppConfigProducer.getInstance().generateDefaultConfig().apply {
                pricingTier = PricingTier.ELASTIC_PREMIUM_EP1
            }
        }
        basicPanel.setFixedRuntime(WebAppDockerRuntime.INSTANCE)
        Disposer.register(this, basicPanel)

        advancedPanel = FunctionAppInfoAdvancedPanel(projectName) {
            FunctionAppConfig().apply {
                pricingTier = PricingTier.ELASTIC_PREMIUM_EP1
            }
        }
        advancedPanel.setFixedRuntime(WebAppDockerRuntime.INSTANCE)
        Disposer.register(this, advancedPanel)

        panel = panel {
            row { cell(basicPanel) }
            row { cell(advancedPanel) }
        }

        advancedPanel.setValidPricingTier(FUNCTION_APP_CONTAINER_PRICING.toList(), PricingTier.ELASTIC_PREMIUM_EP1)

        this.init()

        setFrontPanel(basicFormPanel)
    }

    override fun createCenterPanel() = panel

    override fun getDialogTitle() = "Create Function App Container"

    override fun getBasicFormPanel() = basicPanel

    override fun getAdvancedFormPanel() = advancedPanel

    override fun dispose() {
        super.dispose()
    }
}