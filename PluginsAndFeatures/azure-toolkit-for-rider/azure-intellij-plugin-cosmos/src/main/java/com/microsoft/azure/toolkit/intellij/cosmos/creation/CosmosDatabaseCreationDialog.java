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
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class CosmosDatabaseCreationDialog extends AzureDialog<DatabaseConfig> implements AzureForm<DatabaseConfig> {
    private JPanel pnlRoot;
    private AzureTextInput txtName;
    private ThroughputConfigPanel pnlThroughput;

    private final Project project;
    private final AbstractAzResourceModule<?, ?, ?> module;

    public CosmosDatabaseCreationDialog(@Nullable Project project, @Nonnull CosmosDBAccount account) {
        super(project);
        this.project = project;
        assert CollectionUtils.isNotEmpty(account.getSubModules());
        this.module = account.getSubModules().get(0);
        this.setTitle(String.format("Create %s", module.getResourceTypeName()));
        $$$setupUI$$$();
        this.init();
    }

    protected void init() {
        super.init();
        txtName.addValidator(this::validateDatabaseName);
    }

    private AzureValidationInfo validateDatabaseName() {
        final String value = txtName.getValue();
        return StringUtils.endsWith(value, StringUtils.SPACE) || StringUtils.containsAny(value, "\\", "/", "#", "?", "%") ?
                AzureValidationInfo.error("Database name should not end with space nor contain characters '\\', '/', '#', '?', '%'", txtName) : AzureValidationInfo.success(txtName);
    }

    @Override
    public AzureForm<DatabaseConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Database";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public DatabaseConfig getValue() {
        final DatabaseConfig result = new DatabaseConfig();
        final ThroughputConfig value = pnlThroughput.getValue();
        result.setName(txtName.getValue());
        result.setThroughput(value.getThroughput());
        result.setMaxThroughput(value.getMaxThroughput());
        return result;
    }

    @Override
    public void setValue(@Nonnull DatabaseConfig data) {
        txtName.setValue(data.getName());
        pnlThroughput.setValue(data);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, pnlThroughput);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
