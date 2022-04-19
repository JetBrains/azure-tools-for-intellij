/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functionapp.config.ui

import com.intellij.ui.HideableTitledPanel
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.ResourceGroupSelector
import com.microsoft.intellij.ui.component.SubscriptionSelector
import com.microsoft.intellij.ui.component.StorageAccountSelector
import com.microsoft.intellij.ui.component.appservice.AppNameComponent
import com.microsoft.intellij.ui.component.appservice.HostingPlanSelector
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import com.microsoft.intellij.ui.component.appservice.OperatingSystemSelector
import com.microsoft.intellij.ui.extension.fillComboBox
import com.microsoft.intellij.ui.extension.setComponentsEnabled
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JPanel

class FunctionAppCreateNewComponent(lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    val pnlAppName = AppNameComponent(lifetime.createNested())

    val pnlSubscription = SubscriptionSelector()

    val pnlResourceGroup = ResourceGroupSelector(lifetime.createNested())
    private val pnlResourceGroupHolder = HideableTitledPanel(message("run_config.publish.form.resource_group.header"), pnlResourceGroup, true)

    val pnlOperatingSystem = OperatingSystemSelector()
    private val pnlOperatingSystemHolder = HideableTitledPanel(message("run_config.publish.form.operating_system.header"), pnlOperatingSystem, true)

    val pnlHostingPlan = HostingPlanSelector(lifetime.createNested())
    private val pnlHostingPlanHolder = HideableTitledPanel(message("run_config.publish.form.hosting_plan.header"), pnlHostingPlan, true)

    val pnlStorageAccount = StorageAccountSelector(lifetime.createNested())
    private val pnlStorageAccountHolder = HideableTitledPanel(message("run_config.publish.form.storage_account.header"), pnlStorageAccount, true)

    init {
        initButtonGroupsState()

        add(pnlAppName, "growx")
        add(pnlSubscription, "growx")
        add(pnlResourceGroupHolder, "growx")
        add(pnlOperatingSystemHolder, "growx")
        add(pnlHostingPlanHolder, "growx")
        add(pnlStorageAccountHolder, "growx")
    }

    fun setOperatingSystemRadioButtons(isDotNetCore: Boolean) {
        setComponentsEnabled(isDotNetCore, pnlOperatingSystem.rdoOperatingSystemLinux)

        if (!isDotNetCore) {
            pnlOperatingSystem.rdoOperatingSystemWindows.doClick()
            toggleOperatingSystem(OperatingSystem.WINDOWS)
        }
    }

    //region Button Group

    private fun initButtonGroupsState() {
        initOperatingSystemButtonGroup()
    }

    private fun initOperatingSystemButtonGroup() {
        pnlOperatingSystem.rdoOperatingSystemWindows.addActionListener { toggleOperatingSystem(OperatingSystem.WINDOWS) }
        pnlOperatingSystem.rdoOperatingSystemLinux.addActionListener { toggleOperatingSystem(OperatingSystem.LINUX) }
    }

    fun toggleOperatingSystem(operatingSystem: OperatingSystem) {
        pnlHostingPlan.cbHostingPlan.fillComboBox(
                filterAppServicePlans(operatingSystem, pnlHostingPlan.cachedAppServicePlan),
                pnlHostingPlan.lastSelectedAppServicePlan
        )

        pnlHostingPlan.cbPricingTier.fillComboBox(
                filterPricingTiers(pnlHostingPlan.cachedPricingTier),
                pnlHostingPlan.lastSelectedAppServicePlan?.pricingTier()
        )
    }

    //endregion Button Group

    fun fillSubscription(subscriptions: List<Subscription>, defaultSubscription: Subscription? = null) =
            pnlSubscription.fillSubscriptionComboBox(subscriptions, defaultSubscription)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>, defaultResourceGroupName: String? = null) {
        pnlResourceGroup.fillResourceGroupComboBox(resourceGroups) {
            resourceGroup -> resourceGroup.name() == defaultResourceGroupName
        }
    }

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>, defaultAppServicePlanId: String? = null) =
            pnlHostingPlan.fillAppServicePlan(
                    appServicePlans,
                    { filterAppServicePlans(pnlOperatingSystem.deployOperatingSystem, it) },
                    defaultAppServicePlanId)

    fun fillLocation(locations: List<Location>, defaultLocation: Region? = null) =
            pnlHostingPlan.fillLocationComboBox(locations, defaultLocation)

    fun fillPricingTier(pricingTiers: List<PricingTier>, defaultPricingTier: PricingTier? = null) =
            pnlHostingPlan.fillPricingTier(filterPricingTiers(pricingTiers), defaultPricingTier)

    fun fillStorageAccount(storageAccounts: List<StorageAccount>, defaultStorageAccountId: String? = null) =
            pnlStorageAccount.fillStorageAccount(storageAccounts, defaultStorageAccountId)

    fun fillStorageAccountType(storageAccountType: List<StorageAccountSkuType>, defaultType: StorageAccountSkuType? = null) =
            pnlStorageAccount.fillStorageAccountType(storageAccountType, defaultType)

    /**
     * Filter App Service Plans to Operating System related values
     */
    private fun filterAppServicePlans(operatingSystem: OperatingSystem,
                                      appServicePlans: List<AppServicePlan>): List<AppServicePlan> {
        return appServicePlans
                .filter { it.operatingSystem() == operatingSystem }
                .sortedWith(compareBy({ it.operatingSystem() }, { it.name() }))
    }

    /**
     * Update Pricing Tiers for Functions App
     *
     * Note: Function Apps cannot use "Free" and "Shared" Pricing Tiers.
     *       Add Consumption plan for pricing on demand
     */
    private fun filterPricingTiers(prices: List<PricingTier>) =
            (prices.filter { it != PricingTier.FREE_F1 && it != PricingTier.SHARED_D1 } +
                    FunctionAppPublishModel.consumptionPricingTier).distinct()
}
