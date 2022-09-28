/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.connection;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.IJavaAgentSupported;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.intellij.CommonConst;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ApplicationInsightsResourceDefinition extends ApplicationInsightsResourceDefinitionBase implements IJavaAgentSupported {
    public static final ApplicationInsightsResourceDefinition INSTANCE = new ApplicationInsightsResourceDefinition();

    public ApplicationInsightsResourceDefinition() {
        super();
    }

    @Override
    public AzureFormJPanel<Resource<ApplicationInsight>> getResourcePanel(Project project) {
        return new ApplicationInsightsResourcePanel();
    }

    @Override
    @Nullable
    public File getJavaAgent() {
        return ApplicationInsightsAgentHolder.getApplicationInsightsLibrary();
    }

    // todo: @hanli
    //      1. Get latest ai library release
    //      2. Framework for plugin local file cache
    static class ApplicationInsightsAgentHolder {
        private static final String APPLICATION_INSIGHTS_URL =
                "https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.2.11/applicationinsights-agent-3.2.11.jar";
        private static final File applicationInsightsLibrary =
                new File(PluginManagerCore.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID)).getPluginPath().toString(), "applicationinsights-agent.jar");

        @Preload
        public static synchronized File getApplicationInsightsLibrary() {
            if (!applicationInsightsLibrary.exists()) {
                try {
                    FileUtils.copyURLToFile(new URL(APPLICATION_INSIGHTS_URL), applicationInsightsLibrary);
                } catch (IOException e) {
                    return null;
                }
            }
            return applicationInsightsLibrary;
        }
    }
}
