/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DuplicatedCode")

package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.selectedValueMatches
import com.intellij.util.ui.JBUI
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.FLEX_CONSUMPTION
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import java.awt.event.ItemEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Supplier
import javax.swing.JLabel
import javax.swing.JPanel

open class AppServiceInfoAdvancedPanel<T>(
    private val projectName: String,
    targetProjectOnNetFramework: Boolean = false,
    private val defaultConfigSupplier: Supplier<T>
) : JPanel(), Disposable, AzureFormPanel<T> where T : AppServiceConfig {
    companion object {
        private const val NOT_APPLICABLE = "N/A"
    }

    private var config: T?

    private val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
        value = "app-${projectName}-${LocalDateTime.now().format(formatter)}"
    }
    private val selectorSubscription = SubscriptionComboBox().apply {
        isRequired = true
        addItemListener { onSubscriptionChanged(it) }
    }
    private val selectorGroup = ResourceGroupComboBox().apply {
        isRequired = true
        addItemListener { onGroupChanged(it) }
    }
    private val selectorRegion = RegionComboBox().apply {
        isRequired = true
        addItemListener { onRegionChanged(it) }
        itemsLoader = RegionSupplier()
    }
    protected val selectorServicePlan = ServicePlanComboBox().apply {
        isRequired = true
        addItemListener { onServicePlanChanged(it) }
    }
    private val textSku = JLabel().apply {
        text = NOT_APPLICABLE
        border = JBUI.Borders.emptyLeft(5)
    }

    private var operatingSystem = OperatingSystem.WINDOWS
    private lateinit var operatingSystemGroup: ButtonsGroup
    private lateinit var windowsRadioButton: Cell<JBRadioButton>
    private lateinit var linuxRadioButton: Cell<JBRadioButton>
    private lateinit var dockerRadioButton: Cell<JBRadioButton>

    init {
        Disposer.register(this, textName)

        panel = panel {
            group("Project Details") {
                row("Subscription:") {
                    cell(selectorSubscription)
                        .align(Align.FILL)
                }
                row("Resource Group:") {
                    cell(selectorGroup)
                        .align(Align.FILL)
                }
            }
            group("Instance Details") {
                row("Name:") {
                    cell(textName)
                    label(".azurewebsites.net")
                }
                operatingSystemGroup = buttonsGroup {
                    row("Operating System:") {
                        windowsRadioButton = radioButton("Windows", OperatingSystem.WINDOWS)
                            .enabledIf(selectorServicePlan.selectedValueMatches { it?.pricingTier != FLEX_CONSUMPTION })
                        windowsRadioButton.component.addItemListener { onOperatingSystemChanged(it) }
                        linuxRadioButton = radioButton("Linux", OperatingSystem.LINUX)
                            .enabled(!targetProjectOnNetFramework)
                        dockerRadioButton = radioButton("Docker", OperatingSystem.DOCKER)
                            .visible(false)
                    }
                }.bind(::operatingSystem)
                row("Region:") {
                    cell(selectorRegion)
                        .align(Align.FILL)
                }
            }
            group("App Service Plan") {
                row("Plan:") {
                    cell(selectorServicePlan)
                        .align(Align.FILL)
                }
                row("SKU and size:") {
                    cell(textSku)
                }
            }
            getAdditionalPanel()?.let { panel(it) }
        }

        add(panel)

        config = defaultConfigSupplier.get().also { setValue(it) }
    }

    protected open fun getAdditionalPanel(): (Panel.() -> Unit)? = null

    override fun getValue(): T {
        val name = textName.value
        val region = selectorRegion.value
        val subscription = selectorSubscription.value
        val resourceGroup = selectorGroup.value
        val servicePlan = selectorServicePlan.value
        val operatingSystem =
            if (windowsRadioButton.component.isSelected) OperatingSystem.WINDOWS
            else if (linuxRadioButton.component.isSelected) OperatingSystem.LINUX
            else OperatingSystem.DOCKER

        val result = config ?: defaultConfigSupplier.get()
        result.appName = name
        result.region = region
        result.runtime = (result.runtime ?: RuntimeConfig()).apply {
            os = operatingSystem
        }
        subscription?.let { result.subscriptionId = it.id }
        resourceGroup?.let { result.resourceGroup = it.name }
        servicePlan?.let {
            result.servicePlanName = it.name
            result.pricingTier = it.pricingTier
            val resourceGroupName = if (it.isDraftForCreating) result.resourceGroup else it.resourceGroupName
            resourceGroupName?.let { rgn -> result.servicePlanResourceGroup = rgn }
        }

        getAdditionalValue(result)

        config = result

        return result
    }

    protected open fun getAdditionalValue(result: T) {}

    override fun setValue(config: T) {
        this.config = config

        val subscription =
            config.subscriptionId?.let { Azure.az(AzureAccount::class.java).account().getSubscription(it) }

        subscription?.let { selectorSubscription.value = it }
        textName.value = config.appName
        textName.setSubscription(subscription)
        when (config.runtime?.os) {
            OperatingSystem.WINDOWS -> windowsRadioButton.component.isSelected = true
            OperatingSystem.LINUX -> linuxRadioButton.component.isSelected = true
            OperatingSystem.DOCKER -> dockerRadioButton.component.isSelected = true
            else -> linuxRadioButton.component.isSelected = true
        }

        AzureTaskManager.getInstance().runOnPooledThread {
            selectorRegion.value = config.region
            AppServiceConfig.getResourceGroup(config)?.let { selectorGroup.value = it }
            AppServiceConfig.getServicePlanConfig(config)?.let {
                selectorServicePlan.value =
                    if (it.subscriptionId != null && it.name != null) it.toResource()
                    else null
            }
            setAdditionalValue(config)
        }
    }

    protected open fun setAdditionalValue(config: T) {}

    override fun getInputs(): List<AzureFormInput<*>> = listOf(
        textName,
        selectorSubscription,
        selectorGroup,
        selectorRegion,
        selectorServicePlan
    )

    override fun setVisible(visible: Boolean) {
        panel.isVisible = visible
        @Suppress("RemoveExplicitSuperQualifier")
        super<JPanel>.setVisible(visible)
    }

    private fun onServicePlanChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED) {
            val plan = e.item as? AppServicePlan ?: return
            if (plan.pricingTier == null) return
            val pricingTier = plan.pricingTier
            textSku.text = pricingTier.toString()
            if (pricingTier.isFlexConsumption) {
                linuxRadioButton.component.isSelected = true
                selectorRegion.clear()
                selectorRegion.reloadItems()
            }
        } else if (e.stateChange == ItemEvent.DESELECTED) {
            textSku.text = NOT_APPLICABLE
        }
    }

    protected open fun onSubscriptionChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val subscription = e.item as? Subscription ?: return
            selectorGroup.setSubscription(subscription)
            textName.setSubscription(subscription)
            selectorRegion.setSubscription(subscription)
            selectorServicePlan.setSubscription(subscription)
        }
    }

    private fun onOperatingSystemChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED) {
            selectorServicePlan.setOperatingSystem(OperatingSystem.WINDOWS)
        } else {
            selectorServicePlan.setOperatingSystem(OperatingSystem.LINUX)
        }
    }

    private fun onRegionChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val region = e.item as? Region ?: return
            selectorServicePlan.setRegion(region)
        }
    }

    protected open fun onGroupChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val resourceGroup = e.item as? ResourceGroup ?: return
            selectorServicePlan.setResourceGroup(resourceGroup)
        }
    }

    fun setValidPricingTier(pricingTier: List<PricingTier>, defaultPricingTier: PricingTier) {
        selectorServicePlan.setValidPricingTierList(pricingTier, defaultPricingTier)
    }

    fun setFixedRuntime(runtime: Runtime) {
        when (runtime.operatingSystem) {
            OperatingSystem.WINDOWS -> {
                windowsRadioButton.component.isSelected = true
            }

            OperatingSystem.LINUX -> {
                linuxRadioButton.component.isSelected = true
            }

            OperatingSystem.DOCKER -> {
                dockerRadioButton.component.isSelected = true
            }

            else -> {
                linuxRadioButton.component.isSelected = true
            }
        }
        operatingSystemGroup.visible(false)
    }

    override fun dispose() {
    }

    inner class RegionSupplier : Supplier<List<Region>> {
        override fun get(): List<Region> {
            val subscription = selectorSubscription.value
            val pricingTier = selectorServicePlan.value?.pricingTier
            if (subscription == null) {
                return emptyList()
            }

            if (pricingTier == null) {
                return Azure.az(AzureAccount::class.java).listRegions(subscription.id)
            }

            return Azure.az(AzureAppService::class.java)
                .forSubscription(subscription.id)
                .functionApps()
                .listRegions(pricingTier)
        }
    }
}