/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureIntegerInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ThroughputConfigPanel implements AzureForm<ThroughputConfig> {
    private JRadioButton autoScaleRadioButton;
    private JRadioButton manualRadioButton;
    private JLabel lblThroughputRu;
    private AzureIntegerInput txtThroughputRu;
    private JLabel lblMaxThroughput;
    private AzureIntegerInput txtMaxThroughput;
    private JPanel pnlRoot;

    public ThroughputConfigPanel() {
        init();
    }

    protected void init() {
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(autoScaleRadioButton);
        buttonGroup.add(manualRadioButton);
        autoScaleRadioButton.addItemListener(e -> toggleThroughputStatus());
        manualRadioButton.addItemListener(e -> toggleThroughputStatus());
        txtThroughputRu.setMinValue(400);
        txtThroughputRu.setMaxValue(999999);
        txtThroughputRu.setValue(400);
        txtThroughputRu.addValidator(() -> validateThroughputIncrements(txtThroughputRu, 100));
        txtMaxThroughput.setMinValue(1000);
        txtMaxThroughput.setValue(4000);
        txtMaxThroughput.addValidator(() -> validateThroughputIncrements(txtMaxThroughput, 1000));
        autoScaleRadioButton.setSelected(true);

        lblThroughputRu.setLabelFor(txtThroughputRu);
        lblMaxThroughput.setLabelFor(txtMaxThroughput);
    }

    private AzureValidationInfo validateThroughputIncrements(@Nonnull AzureIntegerInput input, final int unit) {
        final Integer value = input.getValue();
        return Objects.nonNull(value) && value % unit == 0 ? AzureValidationInfo.success(input) :
                AzureValidationInfo.error(String.format("Throughput must be in multiples of %d", unit), input);
    }

    private void toggleThroughputStatus() {
        lblMaxThroughput.setVisible(autoScaleRadioButton.isSelected());
        txtMaxThroughput.setVisible(autoScaleRadioButton.isSelected());
        txtMaxThroughput.setRequired(autoScaleRadioButton.isSelected());
        lblThroughputRu.setVisible(manualRadioButton.isSelected());
        txtThroughputRu.setVisible(manualRadioButton.isSelected());
        txtThroughputRu.setRequired(manualRadioButton.isSelected());
    }

    @Override
    public ThroughputConfig getValue() {
        final ThroughputConfig result = new ThroughputConfig();
        if (autoScaleRadioButton.isSelected()) {
            result.setMaxThroughput(txtMaxThroughput.getValue());
        } else if (manualRadioButton.isSelected()) {
            result.setThroughput(txtThroughputRu.getValue());
        }
        return result;
    }

    @Override
    public void setValue(ThroughputConfig data) {
        if (Objects.nonNull(data.getThroughput())) {
            manualRadioButton.setSelected(true);
            txtThroughputRu.setValue(data.getThroughput());
        } else {
            autoScaleRadioButton.setSelected(true);
            Optional.ofNullable(data.getMaxThroughput()).ifPresent(value -> txtMaxThroughput.setValue(value));
        }
    }

    public void setVisible(boolean visible) {
        pnlRoot.setVisible(visible);
        getInputs().forEach(input -> input.validateValueAsync());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtMaxThroughput, txtThroughputRu);
    }
}
