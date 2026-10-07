/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainerDraft;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CosmosSQLContainerCreationDialog extends AzureDialog<SqlContainerDraft.SqlContainerConfig> implements AzureForm<SqlContainerDraft.SqlContainerConfig> {
    private static final String PARTITION_KEY_PATTERN = "[a-zA-Z0-9_/]+";
    private static final String CONTAINER_ID_PATTERN = "[a-z0-9][a-z0-9-]{2,61}";
    private JCheckBox chkDedicatedThroughput;
    private ThroughputConfigPanel pnlThroughput;
    private AzureTextInput txtPartitionKey;
    private AzureTextInput txtContainerId;
    private JPanel pnlRoot;
    private JLabel lblContainerId;
    private JLabel lblPartitionKey;

    private final Project project;
    private final SqlDatabase database;

    public CosmosSQLContainerCreationDialog(Project project, SqlDatabase database) {
        super(project);
        this.project = project;
        this.database = database;
        init();
    }

    @Override
    protected void init() {
        super.init();
        txtContainerId.setRequired(true);
        txtContainerId.addValidator(this::validateContainerId);
        txtPartitionKey.setRequired(true);
        txtPartitionKey.addValidator(this::validatePartitionKey);
        txtPartitionKey.addValueChangedListener(value -> {
            if (!StringUtils.startsWith(value, "/")) {
                txtPartitionKey.setValue("/" + value);
                txtPartitionKey.setCaretPosition(value.length() + 1);
            }
        });
        pnlThroughput.setVisible(chkDedicatedThroughput.isSelected());
        chkDedicatedThroughput.addItemListener(e -> pnlThroughput.setVisible(chkDedicatedThroughput.isSelected()));

        lblPartitionKey.setLabelFor(txtPartitionKey);
        lblContainerId.setLabelFor(txtContainerId);
    }

    private AzureValidationInfo validateContainerId() {
        final String value = txtContainerId.getValue();
        if (StringUtils.isBlank(value)) {
            return AzureValidationInfo.builder().input(txtContainerId).type(AzureValidationInfo.Type.ERROR).message("Container ID cannot be empty.").build();
        } else if (!value.matches(CONTAINER_ID_PATTERN)) {
            return AzureValidationInfo.error("Container names can only contain lowercase letters, numbers, or the dash (-) character, " +
                    "it should between 3 and 63 characters long and must start with a lowercase letter or number", txtContainerId);
        }
        final SqlContainer sqlContainer = database.containers().get(value, database.getResourceGroupName());
        if (sqlContainer != null) {
            return AzureValidationInfo.error("Container with same id already exists.", txtContainerId);
        }
        return AzureValidationInfo.success(txtContainerId);
    }

    private AzureValidationInfo validatePartitionKey() {
        final String value = txtPartitionKey.getValue();
        if (StringUtils.isEmpty(value) || StringUtils.equalsIgnoreCase(value, "/")) {
            return AzureValidationInfo.error("Partition key should not be empty", txtPartitionKey);
        } else if (!value.matches(PARTITION_KEY_PATTERN)) {
            return AzureValidationInfo.error("Partition key path accepts alphanumeric and underscore (_) characters. " +
                    "You can also use nested objects by using the standard path notation(/).", txtPartitionKey);
        }
        return AzureValidationInfo.success(txtPartitionKey);
    }

    @Override
    public AzureForm<SqlContainerDraft.SqlContainerConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create SQL Container";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public SqlContainerDraft.SqlContainerConfig getValue() {
        final ThroughputConfig throughput = this.pnlThroughput.getValue();
        final SqlContainerDraft.SqlContainerConfig result = new SqlContainerDraft.SqlContainerConfig();
        result.setContainerId(txtContainerId.getValue());
        result.setPartitionKey(txtPartitionKey.getValue());
        if (chkDedicatedThroughput.isSelected()) {
            result.setThroughput(throughput.getThroughput());
            result.setMaxThroughput(throughput.getMaxThroughput());
        }
        return result;
    }

    @Override
    public void setValue(SqlContainerDraft.SqlContainerConfig data) {
        this.txtContainerId.setValue(data.getContainerId());
        Optional.ofNullable(data.getPartitionKey()).ifPresent(value -> this.txtPartitionKey.setValue(StringUtils.startsWith(value, "/") ? value : "/" + value));
        this.pnlThroughput.setValue(data);
        this.chkDedicatedThroughput.setSelected(ObjectUtils.anyNotNull(data.getThroughput(), data.getMaxThroughput()));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtContainerId, txtPartitionKey, pnlThroughput);
    }
}
