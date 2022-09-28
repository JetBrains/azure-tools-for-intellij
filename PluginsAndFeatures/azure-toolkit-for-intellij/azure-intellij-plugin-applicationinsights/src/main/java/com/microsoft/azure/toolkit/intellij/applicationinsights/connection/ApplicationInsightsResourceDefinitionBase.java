/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class ApplicationInsightsResourceDefinitionBase extends AzureServiceResource.Definition<ApplicationInsight> {
    public ApplicationInsightsResourceDefinitionBase() {
        super("Azure.Insights", "Azure Application Insights", AzureIcons.ApplicationInsights.MODULE.getIconPath());
    }

    @Override
    public ApplicationInsight getResource(String dataId) {
        return Azure.az(AzureApplicationInsights.class).getById(dataId);
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<ApplicationInsight> data, Project project) {
        final ApplicationInsight insight = data.getData();
        final HashMap<String, String> env = new HashMap<>();
        env.put("APPINSIGHTS_INSTRUMENTATIONKEY", insight.getInstrumentationKey());
        env.put("APPLICATIONINSIGHTS_CONNECTION_STRING", insight.getConnectionString());
        return env;
    }
}
