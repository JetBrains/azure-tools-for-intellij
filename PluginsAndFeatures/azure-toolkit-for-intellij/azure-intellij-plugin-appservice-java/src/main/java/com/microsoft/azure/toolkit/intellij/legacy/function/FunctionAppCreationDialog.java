/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionAppCreationDialog extends ConfigDialog<FunctionAppConfig> {

    private JPanel contentPane;
    private AppServiceInfoBasicPanel<FunctionAppConfig> basicPanel;
    private FunctionAppConfigFormPanelAdvance advancePanel;

    public FunctionAppCreationDialog(final Project project) {
        super(project);
        this.init();
        setFrontPanel(basicPanel);
    }

    @Override
    protected AzureFormPanel<FunctionAppConfig> getAdvancedFormPanel() {
        return advancePanel;
    }

    @Override
    protected AzureFormPanel<FunctionAppConfig> getBasicFormPanel() {
        return basicPanel;
    }

    @Override
    protected String getDialogTitle() {
        return message("function.create.dialog.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        basicPanel = new AppServiceInfoBasicPanel<>(project, () -> FunctionAppConfig.getFunctionAppDefaultConfig(project.getName())) {
            @Override
            public FunctionAppConfig getValue() {
                // Create AI instance with same name by default
                final FunctionAppConfig config = super.getValue();
                Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getApplicationInsightsConfig).ifPresent(insightConfig -> {
                    if (insightConfig.isNewCreate() && !StringUtils.equals(insightConfig.getName(), config.getName())) {
                        insightConfig.setName(config.getName());
                    }
                });
                return config;
            }
        };
        basicPanel.getSelectorRuntime().setPlatformList(Runtime.FUNCTION_APP_RUNTIME);
        advancePanel = new FunctionAppConfigFormPanelAdvance(project);
    }
}
