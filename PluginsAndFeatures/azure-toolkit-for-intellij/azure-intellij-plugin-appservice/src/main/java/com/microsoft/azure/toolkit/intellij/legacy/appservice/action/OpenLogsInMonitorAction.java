/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.action;

// Monitor UI is not bundled with Rider.
/*
import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class OpenLogsInMonitorAction {
    private static final String APPLICATIONINSIGHTS_CONNECTION_STRING = "APPLICATIONINSIGHTS_CONNECTION_STRING";
    private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private final Project project;
    private final AppServiceAppBase<?, ?, ?> appService;
    private final String resourceId;

    public OpenLogsInMonitorAction(@Nonnull final AppServiceAppBase<?, ?, ?> appService, @Nullable final Project project) {
        this.project = project;
        this.appService = appService;
        this.resourceId = appService.getId();
    }

    public void execute() {
        AzureTaskManager.getInstance().runLater(() ->
                AzureMonitorManager.getInstance().openMonitorWindow(project, getWorkspace(), resourceId));
    }

    @Nullable
    private LogAnalyticsWorkspace getWorkspace() {
        final String aiKey = Optional.ofNullable(appService.getAppSettings())
                .map(settings -> Optional.ofNullable(settings.get(APPINSIGHTS_INSTRUMENTATIONKEY))
                        .orElseGet(() -> getInstrumentationkeyFromConnectionString(settings.get(APPLICATIONINSIGHTS_CONNECTION_STRING))))
                .orElse(null);
        if (StringUtils.isEmpty(aiKey)) {
            throw new AzureToolkitRuntimeException(message("azure.monitor.info.aiNotConfiged"));
        }
        final String subscriptionId = appService.getSubscriptionId();
        final List<ApplicationInsight> insightsResources = Azure.az(AzureApplicationInsights.class).applicationInsights(subscriptionId).list();
        final ApplicationInsight target = insightsResources
                .stream()
                .filter(aiResource -> StringUtils.equals(aiResource.getInstrumentationKey(), aiKey))
                .findFirst()
                .orElseThrow(() -> new AzureToolkitRuntimeException(message("azure.monitor.error.aiNotFound", subscriptionId)));
        final String workspaceResourceId = Optional.ofNullable(target.getRemote())
                .map(ApplicationInsightsComponent::workspaceResourceId).orElse(StringUtils.EMPTY);
        if (StringUtils.isBlank(workspaceResourceId)) {
            AzureMessager.getMessager().info(message("azure.monitor.info.workspaceNotFoundInAI", target.getName()));
            return null;
        }
        return Azure.az(AzureLogAnalyticsWorkspace.class).getById(workspaceResourceId);
    }

    // get the instrument key from connection
    // r.g. input = InstrumentationKey=key;IngestionEndpoint=xxxxx output key
    @Nullable
    private String getInstrumentationkeyFromConnectionString(final String connectionString) {
        if (StringUtils.isEmpty(connectionString)) {
            return null;
        }
        final String[] parts = connectionString.split(";");
        for (final String part : parts) {
            final String[] kv = part.split("=");
            if (kv.length == 2 && StringUtils.equalsIgnoreCase(kv[0], "InstrumentationKey")) {
                return kv[1];
            }
        }
        return null;
    }
}
*/
