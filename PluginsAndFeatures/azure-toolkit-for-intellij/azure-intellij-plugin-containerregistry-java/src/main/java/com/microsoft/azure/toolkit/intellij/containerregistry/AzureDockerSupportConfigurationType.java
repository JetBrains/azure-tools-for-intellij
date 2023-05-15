/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfigurationFactory;
import com.microsoft.azure.toolkit.intellij.containerregistry.pushimage.PushImageRunConfigurationFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class AzureDockerSupportConfigurationType implements ConfigurationType {
    private static final Map<String, Function<AzureDockerSupportConfigurationType, ConfigurationFactory>> FACTORIES_FUNCTION_MAP = new ConcurrentHashMap<>() {
        {
            put(DockerHostRunConfigurationFactory.FACTORY_NAME, DockerHostRunConfigurationFactory::new);
            put(PushImageRunConfigurationFactory.FACTORY_NAME, PushImageRunConfigurationFactory::new);
        }
    };

    public static AzureDockerSupportConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(AzureDockerSupportConfigurationType.class);
    }

    @Override
    public String getDisplayName() {
        return "Azure Docker Support";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Azure Docker Support Configuration Type";
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.DockerSupport.MODULE);
    }

    @NotNull
    @Override
    public String getId() {
        return "AZURE_DOCKER_SUPPORT_CONFIGURATION";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return FACTORIES_FUNCTION_MAP.values().stream().map(function -> function.apply(this)).toArray(ConfigurationFactory[]::new);
    }

    public static void registerConfigurationFactory(String id, Function<AzureDockerSupportConfigurationType, ConfigurationFactory> factoryFunction) {
        FACTORIES_FUNCTION_MAP.put(id, factoryFunction);
    }

    // Todo: Migrate web app on linux to web app configuration type
    public ConfigurationFactory getWebAppOnLinuxDeployConfigurationFactory() {
        return FACTORIES_FUNCTION_MAP.get("Web App for Containers").apply(this);
    }

    public ConfigurationFactory getDockerHostRunConfigurationFactory() {
        return FACTORIES_FUNCTION_MAP.get(DockerHostRunConfigurationFactory.FACTORY_NAME).apply(this);
    }

    public ConfigurationFactory getPushImageRunConfigurationFactory() {
        return FACTORIES_FUNCTION_MAP.get(PushImageRunConfigurationFactory.FACTORY_NAME).apply(this);
    }
}
