/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.selectedValueMatches
import com.microsoft.azure.toolkit.intellij.appservice.functionapp.FlexConsumptionInstanceSize
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoAdvancedPanel
import com.microsoft.azure.toolkit.intellij.storage.storage.StorageAccountComboBox
import com.microsoft.azure.toolkit.intellij.storage.storage.StorageAccountConfig
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.FLEX_CONSUMPTION
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.common.utils.Utils
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import java.awt.event.ItemEvent
import java.util.function.Supplier

class FunctionAppInfoAdvancedPanel(
    projectName: String,
    targetProjectOnNetFramework: Boolean = false,
    defaultConfigSupplier: Supplier<FunctionAppConfig>
) : AppServiceInfoAdvancedPanel<FunctionAppConfig>(projectName, targetProjectOnNetFramework, defaultConfigSupplier) {

    private var instanceMemorySize = FlexConsumptionInstanceSize.Size2048MB
    private lateinit var size2048RadioButton: Cell<JBRadioButton>
    private lateinit var size4096RadioButton: Cell<JBRadioButton>
    private lateinit var storageAccountComboBox: Cell<StorageAccountComboBox>

    override fun getAdditionalPanel(): (Panel.() -> Unit) = {
        group("Flex Consumption Properties") {
            buttonsGroup {
                row("Instance memory:") {
                    size2048RadioButton = radioButton("2048MB", FlexConsumptionInstanceSize.Size2048MB)
                        .selected(true)
                    size4096RadioButton = radioButton("4096MB", FlexConsumptionInstanceSize.Size4096MB)
                }
            }.bind(::instanceMemorySize)
        }.visibleIf(selectorServicePlan.selectedValueMatches { it?.pricingTier == FLEX_CONSUMPTION })
        group("Storage") {
            row("Storage account:") {
                storageAccountComboBox = cell(StorageAccountComboBox())
                    .align(Align.FILL)
            }
        }
    }

    override fun getAdditionalValue(result: FunctionAppConfig) {
        val storageAccount = storageAccountComboBox.component.value
        storageAccount?.let {
            result.storageAccountName = it.name
            result.storageAccountResourceGroup = result.resourceGroup
        }

        if (result.pricingTier == FLEX_CONSUMPTION) {
            val selectedInstanceSize =
                if (size2048RadioButton.component.isSelected) FlexConsumptionInstanceSize.Size2048MB.value
                else if (size4096RadioButton.component.isSelected) FlexConsumptionInstanceSize.Size4096MB.value
                else FlexConsumptionInstanceSize.Size2048MB.value
            result.flexConsumptionConfiguration = FlexConsumptionConfiguration().apply {
                deploymentResourceGroup = result.resourceGroup
                deploymentAccount = storageAccount?.name
                instanceSize = selectedInstanceSize
            }
        }
    }

    override fun setAdditionalValue(config: FunctionAppConfig) {
        val storageAccountConfig =
            if (config.subscriptionId != null)
                StorageAccountConfig(
                    config.subscriptionId,
                    config.storageAccountName ?: "account${Utils.getTimestamp()}"
                )
            else null
        storageAccountComboBox.component.value = storageAccountConfig

        if (config.pricingTier == FLEX_CONSUMPTION) {
            if (config.flexConsumptionConfiguration?.instanceSize == FlexConsumptionInstanceSize.Size4096MB.value) {
                size4096RadioButton.component.isSelected = true
            }
            else {
                size2048RadioButton.component.isSelected = true
            }
        }
    }

    override fun onSubscriptionChanged(e: ItemEvent) {
        super.onSubscriptionChanged(e)

        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val item = e.item
            if (item !is Subscription?) return
            storageAccountComboBox.component.setSubscription(item.id)
        }
    }

    override fun onGroupChanged(e: ItemEvent) {
        super.onGroupChanged(e)

        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val item = e.item
            if (item !is ResourceGroup?) return
            storageAccountComboBox.component.setResourceGroup(item?.name)
        }
    }
}