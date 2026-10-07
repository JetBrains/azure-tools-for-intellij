/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppConfigProducer
import com.microsoft.azure.toolkit.intellij.legacy.utils.removeInvalidCharacters
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.IAccountActions
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import javax.swing.JPanel

class FunctionAppCreationDialog(
    project: Project,
    targetProjectOnNetFramework: Boolean
) : ConfigDialog<FunctionAppConfig>(project), Disposable {
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
        basicPanel = AppServiceInfoBasicPanel(targetProjectOnNetFramework) {
            FunctionAppConfigProducer.getInstance().generateDefaultConfig()
        }
        Disposer.register(this, basicPanel)

        advancedPanel = FunctionAppInfoAdvancedPanel(projectName, targetProjectOnNetFramework) {
            FunctionAppConfig()
        }
        Disposer.register(this, advancedPanel)

        panel = panel {
            row { cell(basicPanel) }
            row { cell(advancedPanel) }
        }

        advancedPanel.setValidPricingTier(PricingTier.FUNCTION_PRICING.toList(), PricingTier.CONSUMPTION)

        this.init()

        setFrontPanel(basicFormPanel)
    }

    override fun createCenterPanel() = panel

    override fun getDialogTitle() = "Create Function App"

    override fun getBasicFormPanel() = basicPanel

    override fun getAdvancedFormPanel() = advancedPanel

    override fun dispose() {
        super.dispose()
    }
}