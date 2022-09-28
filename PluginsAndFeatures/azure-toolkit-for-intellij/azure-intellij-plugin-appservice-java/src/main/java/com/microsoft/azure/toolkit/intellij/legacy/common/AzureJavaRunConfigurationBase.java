/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common;

import com.intellij.execution.configurations.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AzureJavaRunConfigurationBase<T> extends AzureRunConfigurationBase<T> {
    protected JavaRunConfigurationModule myModule;

    protected AzureJavaRunConfigurationBase(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    protected AzureJavaRunConfigurationBase(@NotNull AzureRunConfigurationBase source) {
        super(source);
    }

    public JavaRunConfigurationModule getConfigurationModule() {
        return myModule;
    }
}
