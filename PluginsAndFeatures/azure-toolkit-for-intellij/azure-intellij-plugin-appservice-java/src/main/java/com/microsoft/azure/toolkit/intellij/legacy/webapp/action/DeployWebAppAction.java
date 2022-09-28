/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class DeployWebAppAction {

    private final WebAppConfigurationType configType = WebAppConfigurationType.getInstance();

    private final Project project;
    @Nonnull
    private final WebApp webApp;

    public DeployWebAppAction(@Nonnull final WebApp webApp, final Project project) {
        super();
        this.project = project;
        this.webApp = webApp;
    }

    public void execute() {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final RunnerAndConfigurationSettings settings = getRunConfigurationSettings(manager);
        if (RunDialog.editConfiguration(project, settings, message("webapp.deploy.configuration.title"),
                DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }

    private RunnerAndConfigurationSettings getRunConfigurationSettings(RunManagerEx manager) {
        final ConfigurationFactory factory = configType.getWebAppConfigurationFactory();
        final String runConfigurationName = String.format("%s: %s:%s", factory.getName(), project.getName(), webApp.name());
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            settings = manager.createConfiguration(runConfigurationName, factory);
        }
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof WebAppConfiguration) {
            ((WebAppConfiguration) runConfiguration).setWebApp(webApp);
        }
        return settings;
    }
}
