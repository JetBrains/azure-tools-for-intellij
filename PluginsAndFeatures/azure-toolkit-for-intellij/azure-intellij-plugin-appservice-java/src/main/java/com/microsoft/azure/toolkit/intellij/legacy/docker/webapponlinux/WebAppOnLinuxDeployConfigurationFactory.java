/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.webapponlinux;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.docker.AzureDockerSupportConfigurationType;
import org.jetbrains.annotations.NotNull;

public class WebAppOnLinuxDeployConfigurationFactory extends WebAppOnLinuxDeployConfigurationFactoryBase {
    public WebAppOnLinuxDeployConfigurationFactory(AzureDockerSupportConfigurationType configurationType) {
        super(configurationType);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WebAppOnLinuxDeployConfiguration(project, this, String.format("%s: %s", FACTORY_NAME, project
                .getName()));
    }

    @Override
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
        return new WebAppOnLinuxDeployConfiguration(template.getProject(), this, name);
    }
}
