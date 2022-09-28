/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.webapponlinux;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class WebAppOnLinuxDeployConfigurationFactoryBase extends ConfigurationFactory {
    protected static final String FACTORY_NAME = "Web App for Containers";

    protected WebAppOnLinuxDeployConfigurationFactoryBase(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public String getName() {
        return FACTORY_NAME;
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.DockerSupport.RUN_ON_WEB_APP);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return FACTORY_NAME;
    }
}
