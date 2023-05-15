/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class DockerHostRunConfigurationFactory extends ConfigurationFactory {
    public static final String FACTORY_NAME = "Docker";

    public DockerHostRunConfigurationFactory(AzureDockerSupportConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DockerHostRunConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project.getName()));
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new DockerHostRunConfiguration(template.getProject(), this, name);
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.DockerSupport.RUN);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return FACTORY_NAME;
    }
}
