/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class RunOnDockerHostAction extends AnAction {
    private static final String DIALOG_TITLE = "Run on Docker Host";
    private static final AzureDockerSupportConfigurationType configType = AzureDockerSupportConfigurationType.getInstance();
    private final DockerImage dockerImage;

    public RunOnDockerHostAction() {
        this(null);
    }

    public RunOnDockerHostAction(@Nullable final DockerImage dockerImage) {
        super("Run on Docker Host...", "Build image and run in local docker host", IntelliJAzureIcons.getIcon("/icons/DockerSupport/Run.svg"));
        this.dockerImage = dockerImage;
    }

    @Override
    @AzureOperation(name = "user/docker.run_image")
    public void actionPerformed(@Nonnull AnActionEvent e) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(e.getPlace(), EMPTY_PLACE));
        Optional.ofNullable(e.getProject()).ifPresent(p -> runConfiguration(p, this.dockerImage));
    }

    public static void run(@Nonnull Tag tag, @Nonnull Project project) {
        final Repository repository = tag.getParent().getParent();
        final DockerImage image = DockerImage.builder()
            .isDraft(false)
            .repositoryName(repository.getFullName())
            .tagName(tag.getName())
            .build();
        runConfiguration(project, image);
    }

    private static void runConfiguration(Project project, DockerImage dockerImage) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getDockerHostRunConfigurationFactory();
        final String configurationName = String.format("%s: %s", factory.getName(), project.getName());
        final RunnerAndConfigurationSettings existingSettings = manager.findConfigurationByName(configurationName);
        final RunnerAndConfigurationSettings settings = Optional.ofNullable(existingSettings)
            .orElseGet(() -> manager.createConfiguration(configurationName, factory));
        final RunConfiguration configuration = settings.getConfiguration();
        if (configuration instanceof DockerHostRunConfiguration) {
            Optional.ofNullable(dockerImage).ifPresent(image -> ((DockerHostRunConfiguration) configuration).setDockerImage(image));
        }
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }
}
