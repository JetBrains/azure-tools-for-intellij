/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientConfig;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.RunOnDockerHostAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import lombok.SneakyThrows;

import java.util.Objects;
import java.util.Optional;

public class IntelliJContainerRegistryActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ContainerRegistryActionsContributor.PULL_IMAGE, IntelliJContainerRegistryActionsContributor::pullImage);
        am.registerHandler(ContainerRegistryActionsContributor.INSPECT_IMAGE, IntelliJContainerRegistryActionsContributor::inspectImage);
        am.registerHandler(ContainerRegistryActionsContributor.LOGIN, IntelliJContainerRegistryActionsContributor::login);
        am.registerHandler(ContainerRegistryActionsContributor.LOGOUT, IntelliJContainerRegistryActionsContributor::logout);
        am.registerHandler(ContainerRegistryActionsContributor.RUN_LOCALLY, IntelliJContainerRegistryActionsContributor::runImage);
    }

    private static void login(ContainerRegistry s, AnActionEvent e) {
        final Project project = Objects.requireNonNull(e.getProject());
        final AzureActionManager am = AzureActionManager.getInstance();
        if (!s.isAdminUserEnabled()) {
            final Action<ContainerRegistry> enableAdminUser = am.getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(s);
            throw new AzureToolkitRuntimeException(String.format("Admin user is not enabled for (%s), but it is required to docker login to an Azure Container Registry.", s.getName()), enableAdminUser);
        }
        final String command = String.format("docker login %s -u %s", s.getLoginServerUrl(), s.getUserName());
        TerminalUtils.executeInTerminal(project, command, s.getName());
    }

    private static void logout(ContainerRegistry s, AnActionEvent e) {
        final Project project = Objects.requireNonNull(e.getProject());
        final String command = String.format("docker logout %s", s.getLoginServerUrl());
        TerminalUtils.executeInTerminal(project, command, s.getName());
    }

    private static void runImage(Tag tag, AnActionEvent e) {
        RunOnDockerHostAction.run(tag, Objects.requireNonNull(e.getProject()));
    }

    @SneakyThrows(JsonProcessingException.class)
    private static void inspectImage(Tag tag, AnActionEvent event) {
        final AzureDockerClient client = AzureDockerClient.getDefault();
        final Image image = client.getImage(tag);
        final String message = String.format("Image %s is not found locally, it must be pulled first.", tag.getImageName());
        final Action<Tag> pull = AzureActionManager.getInstance().getAction(ContainerRegistryActionsContributor.PULL_IMAGE).bind(tag);
        final Action<AzResource> manifest = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_PORTAL_URL).bind(tag).withLabel("Open Manifest in Portal");
        final InspectImageResponse inspection = Optional.ofNullable(image).map(i -> client.inspectImage(i.getId()))
            .orElseThrow(() -> new AzureToolkitRuntimeException(message, pull, manifest));
        final String content = DockerClientConfig.getDefaultObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(inspection);
        final Project project = Objects.requireNonNull(event.getProject());
        AzureTaskManager.getInstance().runLater(() -> {
            final String title = tag.getFullName().replaceAll("\\W+", "-") + ".json";
            VirtualFileActions.openJsonStringInEditor(content, title, project);
        });
    }

    private static void pullImage(Tag t) {
        final AzureActionManager am = AzureActionManager.getInstance();
        final String repositoryName = t.getParent().getParent().getName();
        if (!t.exists()) {
            throw new AzureToolkitRuntimeException(String.format("image %s:%s doesn't exist", repositoryName, t.getName()));
        }
        final ContainerRegistry registry = t.getParent().getParent().getParent();
        if (!registry.isAdminUserEnabled()) {
            final Action<ContainerRegistry> enableAdminUser = am.getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(registry);
            throw new AzureToolkitRuntimeException(String.format("Admin user is not enabled for (%s), but it is required to pull image from Azure Container Registry.", registry.getName()), enableAdminUser);
        }
        final String imageNameWithTag = String.format("%s:%s", repositoryName, t.getName());
        try {
            AzureDockerClient.getDefault().pullImage(Objects.requireNonNull(registry.getLoginServerUrl()), registry.getUserName(),
                registry.getPrimaryCredential(), repositoryName, t.getName());
            final Action<Tag> inspect = am.getAction(ContainerRegistryActionsContributor.INSPECT_IMAGE).withLabel("Inspect").bind(t);
            final Action<Tag> run = am.getAction(ContainerRegistryActionsContributor.RUN_LOCALLY).bind(t);
            final Action<Tag> copyRunCommand = am.getAction(ContainerRegistryActionsContributor.COPY_RUN_COMMAND).bind(t);
            final AzureString message = AzureString.format("Image %s is successfully pulled from Azure Container Registry %s", imageNameWithTag, registry.getName());
            AzureMessager.getMessager().success(message, inspect, run, copyRunCommand);
        } catch (final InterruptedException e) {
            throw new AzureToolkitRuntimeException(String.format("interrupted when pulling image \"%s\"", imageNameWithTag), e);
        }
    }

    @Override
    public int getOrder() {
        return ContainerRegistryActionsContributor.INITIALIZE_ORDER + 1;
    }
}
