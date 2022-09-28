/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.docker.action;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.microsoft.azure.toolkit.intellij.legacy.docker.AzureDockerSupportConfigurationType;

public class PushImageAction extends PushImageActionBase {
    private final AzureDockerSupportConfigurationType configType = AzureDockerSupportConfigurationType.getInstance();

    @Override
    protected ConfigurationFactory getPushImageRunConfigurationFactory() {
        return configType.getPushImageRunConfigurationFactory();
    }
}
