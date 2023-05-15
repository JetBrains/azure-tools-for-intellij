/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.pushimage;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.action.Action.EMPTY_PLACE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class PushImageAction extends AnAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String DIALOG_TITLE = "Push Image to Azure Container Registry";
    private static final AzureDockerSupportConfigurationType configType = AzureDockerSupportConfigurationType.getInstance();

    private final DockerImage dockerImage;

    public PushImageAction() {
        this(null);
    }

    public PushImageAction(@Nullable DockerImage dockerImage) {
        //noinspection DialogTitleCapitalization
        super("Push Image to Azure Container Registry...", "Build/push local image to Azure Container Registry", IntelliJAzureIcons.getIcon("/icons/DockerSupport/PushImage.svg"));
        this.dockerImage = dockerImage;
    }

    @Override
    @AzureOperation(name = "user/docker.push_image")
    public void actionPerformed(@Nonnull AnActionEvent e) {
        OperationContext.current().setTelemetryProperty(PLACE, StringUtils.firstNonBlank(e.getPlace(), EMPTY_PLACE));
        final Project project = e.getProject();
        if (project != null) {
            AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH)
                .handle(() -> runConfiguration(project, this.dockerImage, null));
        }
    }

    public static void push(@Nonnull Tag tag, @Nonnull Project project) {
        final DockerImage image = DockerImage.builder()
            .isDraft(false)
            .repositoryName(tag.getParent().getParent().getName())
            .tagName(tag.getName())
            .build();
        runConfiguration(project, image, null);
    }

    public static void push(@Nonnull ContainerRegistry registry, @Nonnull Project project) {
        runConfiguration(project, null, registry);
    }

    private static void runConfiguration(@Nonnull Project project, @Nullable DockerImage image, @Nullable ContainerRegistry registry) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getPushImageRunConfigurationFactory();
        final String configurationName = String.format("%s: %s", factory.getName(), project.getName());
        final RunnerAndConfigurationSettings existingSettings = manager.findConfigurationByName(configurationName);
        final RunnerAndConfigurationSettings settings = Optional.ofNullable(existingSettings)
            .orElseGet(() -> manager.createConfiguration(configurationName, factory));
        if (settings.getConfiguration() instanceof PushImageRunConfiguration) {
            Optional.ofNullable(image).ifPresent(i -> ((PushImageRunConfiguration) settings.getConfiguration()).setDockerImage(i));
            Optional.ofNullable(registry).ifPresent(i -> ((PushImageRunConfiguration) settings.getConfiguration()).setContainerRegistryId(i.getId()));
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
