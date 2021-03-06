/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.appservice

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.*
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.AppServicePlanValidator
import com.microsoft.intellij.helpers.validator.LocationValidator
import com.microsoft.intellij.helpers.validator.PricingTierValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.awt.event.ActionListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class HostingPlanSelector(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2, hidemode 3", "[min!][]", "[sg a]")),
        AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(17)
    }

    val rdoUseExisting = JRadioButton(message("run_config.publish.form.hosting_plan.use_existing"), true)
    val cbHostingPlan = ComboBox<AppServicePlan>()

    val rdoCreateNew = JRadioButton(message("run_config.publish.form.hosting_plan.create_new"))
    val txtName = JTextField("")
    private val lblLocation = JLabel(message("run_config.publish.form.hosting_plan.location.label"))
    val cbLocation = ComboBox<Location>()
    private val lblPricingTier = JLabel(message("run_config.publish.form.hosting_plan.pricing_tier.label"))
    val cbPricingTier = ComboBox<PricingTier>()

    var cachedAppServicePlan: List<AppServicePlan> = emptyList()
    var lastSelectedAppServicePlan: AppServicePlan? = null

    val isCreatingNew: Boolean
        get() = rdoCreateNew.isSelected

    val hostingPlanName: String
        get() = txtName.text

    init {
        initExistingPlanComboBox()
        initLocationComboBox()
        initPricingTierComboBox()
        initHostingPlanButtonsGroup()

        add(rdoUseExisting)
        add(cbHostingPlan, "growx")

        add(rdoCreateNew)
        add(txtName, "growx")
        add(lblLocation, "gapbefore $indentionSize")
        add(cbLocation, "growx")
        add(lblPricingTier, "gapbefore $indentionSize")
        add(cbPricingTier, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()

        if (rdoUseExisting.isSelected) {
            return listOfNotNull(
                    AppServicePlanValidator.checkAppServicePlanIsSet(cbHostingPlan.getSelectedValue())
                            .toValidationInfo(cbHostingPlan)
            )
        }

        return listOfNotNull(
                AppServicePlanValidator.validateAppServicePlanName(txtName.text).toValidationInfo(txtName),
                LocationValidator.checkLocationIsSet(cbLocation.getSelectedValue()).toValidationInfo(cbLocation),
                PricingTierValidator.checkPricingTierIsSet(cbPricingTier.getSelectedValue()).toValidationInfo(cbPricingTier)
        )
    }

    override fun initComponentValidation() {
        txtName.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    AppServicePlanValidator.checkAppServicePlanNameMaxLength(txtName.text)
                            .merge(AppServicePlanValidator.checkInvalidCharacters(txtName.text)) },
                focusLostValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    if (txtName.text.isEmpty()) return@initValidationWithResult ValidationResult()
                    AppServicePlanValidator.checkAppServicePlanNameMinLength(txtName.text) })
    }

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>, defaultAppServicePlanId: String? = null) {
        cachedAppServicePlan = appServicePlans

        cbHostingPlan.fillComboBox<AppServicePlan>(
                elements = appServicePlans,
                defaultComparator = { appServicePlan -> appServicePlan.id() == defaultAppServicePlanId })
    }

    fun fillLocationComboBox(locations: List<Location>, defaultLocation: Region? = AzureDefaults.location) {
        cbLocation.fillComboBox<Location>(
                elements = locations,
                defaultComparator = { location -> location.region() == defaultLocation })
    }

    fun fillPricingTier(pricingTiers: List<PricingTier>, defaultPricingTier: PricingTier? = null) {
        cbPricingTier.fillComboBox(
                elements = pricingTiers,
                defaultElement = defaultPricingTier)
    }

    private fun initExistingPlanComboBox() {
        cbHostingPlan.addActionListener {
            lastSelectedAppServicePlan = cbHostingPlan.getSelectedValue()
        }

        cbHostingPlan.setDefaultRenderer(message("run_config.publish.form.hosting_plan.empty_message")) {
            "${it.name()} (${it.regionName()}, ${it.pricingTier().toSkuDescription().size()})"
        }
    }

    private fun initLocationComboBox() {
        cbLocation.setDefaultRenderer(message("run_config.publish.form.hosting_plan.location.empty_message")) { it.displayName() }
    }

    private fun initPricingTierComboBox() {
        cbPricingTier.setDefaultRenderer(message("run_config.publish.form.hosting_plan.pricing_tier.empty_message")) {
            val skuDescription = it.toSkuDescription()
            "${skuDescription.name()} ${if (skuDescription.name() == "Consumption") "" else "(${skuDescription.tier()})"}"
                    .trim()
        }
    }

    private fun initHostingPlanButtonsGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExisting to ActionListener { toggleHostingPlanPanel(false) },
                rdoCreateNew to ActionListener { toggleHostingPlanPanel(true) }))

        toggleHostingPlanPanel(false)
    }

    fun toggleHostingPlanPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtName, lblLocation, cbLocation, lblPricingTier, cbPricingTier)
        setComponentsEnabled(!isCreatingNew, cbHostingPlan)
    }
}
