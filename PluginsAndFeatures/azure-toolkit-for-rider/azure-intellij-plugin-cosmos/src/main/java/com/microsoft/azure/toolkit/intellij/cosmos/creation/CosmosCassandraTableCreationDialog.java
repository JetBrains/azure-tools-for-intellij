/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTable;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTableDraft;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class CosmosCassandraTableCreationDialog extends AzureDialog<CassandraTableDraft.CassandraTableConfig> implements AzureForm<CassandraTableDraft.CassandraTableConfig> {
    public static final String TEMPLATE = "CREATE TABLE %s.%s";
    public static final String PARTITION_KEY_PATTERN = "[a-zA-Z0-9_]+";

    private AzureTextInput txtTableId;
    private JTextArea textSchema;
    private JPanel pnlRoot;
    private JLabel lblCreateCommand;
    private HyperlinkLabel lblLearnMore;
    private JLabel lblTableId;
    private JLabel lblCommand;

    private final Project project;
    private final CassandraKeyspace keyspace;

    public CosmosCassandraTableCreationDialog(Project project, CassandraKeyspace keyspace) {
        super(project);
        this.project = project;
        this.keyspace = keyspace;
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        txtTableId.setRequired(true);
        txtTableId.addValidator(this::validateTableId);
        txtTableId.addValueChangedListener(value -> {
            final String displayName = value.length() > 50 ? value.substring(0, 47) + "..." : value;
            final String command = String.format(TEMPLATE, keyspace.getName(), displayName);
            lblCreateCommand.setText(command);
        });

        lblTableId.setLabelFor(txtTableId);
        lblCommand.setLabelFor(textSchema);

        lblLearnMore.setHyperlinkText("Learn More");
        lblLearnMore.setHyperlinkTarget("https://aka.ms/cassandra-create-table");

        textSchema.setFont(JBFont.label());
        this.pack();
    }

    private AzureValidationInfo validateTableId() {
        final String value = txtTableId.getValue();
        if (StringUtils.isNotEmpty(value) && value.length() > 48) {
            return AzureValidationInfo.error("Table id should be less than 48 characters.", txtTableId);
        }
        if (!value.matches(PARTITION_KEY_PATTERN)) {
            return AzureValidationInfo.error("Table id should only contain alpha-numeric characters and underscores.", txtTableId);
        }
        if (StringUtils.equalsAnyIgnoreCase(value, "table", "keyspace")) {
            return AzureValidationInfo.error(String.format("'%s' should not be used as table name", value), txtTableId);
        }
        final CassandraTable cassandraTable = keyspace.tables().get(value, keyspace.getResourceGroupName());
        if (cassandraTable != null) {
            return AzureValidationInfo.error("Table with same id already exists.", txtTableId);
        }
        return AzureValidationInfo.success(txtTableId);
    }

    @Override
    public AzureForm<CassandraTableDraft.CassandraTableConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Cassandra Table";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public CassandraTableDraft.CassandraTableConfig getValue() {
        final CassandraTableDraft.CassandraTableConfig result = new CassandraTableDraft.CassandraTableConfig();
        result.setTableId(txtTableId.getValue());
        result.setSchema(textSchema.getText());
        return result;
    }

    @Override
    public void setValue(CassandraTableDraft.CassandraTableConfig data) {
        txtTableId.setValue(data.getTableId());
        textSchema.setText(data.getSchema());
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<ValidationInfo> validationInfos = super.doValidateAll();
        if (validationInfos.isEmpty()) {
            final String schema = textSchema.getText();
            if (StringUtils.isBlank(schema)) {
                validationInfos.add(new ValidationInfo("Command is required", textSchema));
            }
        }
        return validationInfos;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtTableId);
    }

}
