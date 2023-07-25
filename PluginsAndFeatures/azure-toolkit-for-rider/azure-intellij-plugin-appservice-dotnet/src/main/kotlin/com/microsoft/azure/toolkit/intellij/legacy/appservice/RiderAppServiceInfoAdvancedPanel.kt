package com.microsoft.azure.toolkit.intellij.legacy.appservice

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig
import com.microsoft.azure.toolkit.intellij.common.AzureDotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig
import java.awt.event.ItemEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Supplier
import javax.swing.JLabel
import javax.swing.JPanel

class RiderAppServiceInfoAdvancedPanel<T>(
        project: Project,
        private val defaultConfigSupplier: Supplier<T>
) : JPanel(), AzureFormPanel<T> where T : AppServiceConfig {
    companion object {
        private const val NOT_APPLICABLE = "N/A"
    }

    private val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")

    private val panel: JPanel
    private val textName = AppNameInput().apply {
        isRequired = true
        value = "app-${project.name}-${LocalDateTime.now().format(formatter)}"
    }
    private val subscriptionComboBox = SubscriptionComboBox().apply {
        isRequired = true
        addItemListener { onSubscriptionChanged(it) }
    }
    private val resourceGroupComboBox = ResourceGroupComboBox().apply {
        isRequired = true
        addItemListener { onGroupChanged(it) }
    }
    private val regionComboBox = RegionComboBox().apply {
        isRequired = true
        addItemListener { onRegionChanged(it) }
    }
    private val servicePlanComboBox = ServicePlanComboBox().apply {
        isRequired = true
        addItemListener { onServicePlanChanged(it) }
    }
    private val textSku = JLabel().apply {
        text = NOT_APPLICABLE
        border = JBUI.Borders.emptyLeft(5)
    }

    private val selectorApplication = AzureDotnetProjectComboBox(project) { _ -> true }

    private var operatingSystem = OperatingSystem.WINDOWS
    private lateinit var deploymentGroup: Row

    init {
        panel = panel {
            group("Project Details") {
                row("Subscription:") {
                    cell(subscriptionComboBox)
                            .align(Align.FILL)
                }
                row("Resource Group:") {
                    cell(resourceGroupComboBox)
                            .align(Align.FILL)
                }
            }
            group("Instance Details") {
                row("Name:") {
                    cell(textName)
                    label(".azurewebsites.net")
                }
                buttonsGroup {
                    row("Operating System:") {
                        radioButton("Windows", OperatingSystem.WINDOWS)
                                .component
                                .addItemListener { onOperatingSystemChanged(it) }
                        radioButton("Linux", OperatingSystem.LINUX)
                    }
                }.bind(::operatingSystem)
                row("Region:") {
                    cell(regionComboBox)
                            .align(Align.FILL)
                }
            }
            group("App Service Plan") {
                row("Plan:") {
                    cell(servicePlanComboBox)
                            .align(Align.FILL)
                }
                row("SKU and size:") {
                    cell(textSku)
                }
            }
            deploymentGroup = group("Deployment") {
                row("Project:") {
                    cell(selectorApplication)
                            .align(Align.FILL)
                }
            }
        }

        add(panel)
    }

    override fun getValue(): T {
        val subscription = subscriptionComboBox.value
        val resourceGroup = resourceGroupComboBox.value
        val name = textName.value
        val os = operatingSystem
        val region = regionComboBox.value
        val servicePlan = servicePlanComboBox.value
        val project = selectorApplication.value

        val config = defaultConfigSupplier.get()
        config.subscription = subscription
        config.resourceGroup = ResourceGroupConfig.fromResource(resourceGroup)
        config.name = name
        config.runtime = Runtime(os, null, null)
        config.region = region


        return config
    }

    override fun setValue(data: T) {
    }

    override fun getInputs(): List<AzureFormInput<*>> = listOf(textName, subscriptionComboBox, resourceGroupComboBox)
    override fun setVisible(visible: Boolean) {
        panel.isVisible = visible
        super<JPanel>.setVisible(visible)
    }

    fun setDeploymentVisible(visible: Boolean) {
        deploymentGroup.visible(visible)
    }

    private fun onServicePlanChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED) {
            val plan = e.item as? AppServicePlan ?: return
            if (plan.pricingTier == null) return
            val pricing = if (plan.pricingTier == PricingTier.CONSUMPTION) "Consumption" else "${plan.pricingTier.tier}_${plan.pricingTier.size}"
            textSku.text = pricing
        } else if (e.stateChange == ItemEvent.DESELECTED) {
            textSku.text = NOT_APPLICABLE
        }
    }

    private fun onSubscriptionChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val subscription = e.item as? Subscription ?: return
            resourceGroupComboBox.setSubscription(subscription)
            textName.setSubscription(subscription)
            regionComboBox.setSubscription(subscription)
            servicePlanComboBox.setSubscription(subscription)
        }
    }

    private fun onOperatingSystemChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED) {
            servicePlanComboBox.setOperatingSystem(OperatingSystem.WINDOWS)
        } else {
            servicePlanComboBox.setOperatingSystem(OperatingSystem.LINUX)
        }
    }

    private fun onRegionChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val region = e.item as? Region ?: return
            servicePlanComboBox.setRegion(region)
        }
    }

    private fun onGroupChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val resourceGroup = e.item as? ResourceGroup ?: return
            servicePlanComboBox.setResourceGroup(resourceGroup)
        }
    }
}