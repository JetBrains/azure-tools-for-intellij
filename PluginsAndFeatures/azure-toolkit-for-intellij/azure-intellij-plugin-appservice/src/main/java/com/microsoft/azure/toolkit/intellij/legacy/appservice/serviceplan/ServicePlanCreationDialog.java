/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan;

import com.intellij.ui.components.JBLabel;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.ObjectUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.intellij.util.ValidationUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ServicePlanCreationDialog extends AzureDialog<AppServicePlanDraft>
    implements AzureForm<AppServicePlanDraft> {
    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private JPanel contentPanel;
    private JBLabel labelDescription;
    private AzureTextInput textName;
    private PricingTierComboBox comboBoxPricingTier;

    public ServicePlanCreationDialog(final Subscription subscription,
                                     final ResourceGroup resourceGroup,
                                     final String name,
                                     final List<PricingTier> pricingTierList, final PricingTier defaultPricingTier) {
        super();
        this.subscription = subscription;
        this.resourceGroup = resourceGroup;
        this.init();
        this.textName.setValue(name);
        this.textName.addValidator(this::validateName);
        this.comboBoxPricingTier.setPricingTierList(pricingTierList);
        this.comboBoxPricingTier.setDefaultPricingTier(defaultPricingTier);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, message("appService.servicePlan.description"));
        this.pack();
    }

    private AzureValidationInfo validateName() {
        final String value = this.textName.getValue();
        try {
            ValidationUtils.validateAppServicePlanName(value);
        } catch (final IllegalArgumentException e) {
            return AzureValidationInfo.error(e.getMessage(), this);
        }
        if (ObjectUtils.allNotNull(subscription, resourceGroup)) {
            final AppServicePlan appServicePlan = Azure.az(AzureAppService.class).plans(subscription.getId()).get(value, resourceGroup.getName());
            if (appServicePlan != null && appServicePlan.exists()) {
                return AzureValidationInfo.error("App service plan name must be unique in each resource group.", this);
            }
        }
        return AzureValidationInfo.success(this);
    }

    @Override
    public AzureForm<AppServicePlanDraft> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return message("appService.servicePlan.create.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public AppServicePlanDraft getValue() {
        final AppServicePlanDraft draft = Azure.az(AzureAppService.class).plans(this.subscription.getId())
            .create(this.textName.getValue(), Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse("<none>"));
        draft.setPricingTier(this.comboBoxPricingTier.getValue());
        return draft;
    }

    @Override
    public void setValue(final AppServicePlanDraft data) {
        this.subscription = data.getSubscription();
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.textName = new AzureTextInput();
        this.textName.setRequired(true);
        this.comboBoxPricingTier = new PricingTierComboBox();
    }
}
