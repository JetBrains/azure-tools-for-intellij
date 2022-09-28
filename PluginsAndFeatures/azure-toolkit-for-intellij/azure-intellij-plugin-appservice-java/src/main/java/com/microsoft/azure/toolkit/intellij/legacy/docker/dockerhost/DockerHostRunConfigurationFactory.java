/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.dockerhost;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import com.microsoft.azure.toolkit.intellij.legacy.docker.AzureDockerSupportConfigurationType;
import org.jetbrains.annotations.NotNull;

public class DockerHostRunConfigurationFactory extends DockerHostRunConfigurationFactoryBase {
    public DockerHostRunConfigurationFactory(AzureDockerSupportConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DockerHostRunConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project.getName()));
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new DockerHostRunConfiguration(template.getProject(), this, name);
    }
}
