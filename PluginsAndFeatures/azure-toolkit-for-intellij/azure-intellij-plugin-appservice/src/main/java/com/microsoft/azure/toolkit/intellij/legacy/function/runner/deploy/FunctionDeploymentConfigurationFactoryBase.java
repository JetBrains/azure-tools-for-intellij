package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public abstract class FunctionDeploymentConfigurationFactoryBase extends ConfigurationFactory  {
    protected FunctionDeploymentConfigurationFactoryBase(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public String getName() {
        return message("function.deploy.factory.name");
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.DEPLOY);
    }

    @Override
    public @NotNull @NonNls String getId() {
        return message("function.deploy.factory.name");
    }
}
