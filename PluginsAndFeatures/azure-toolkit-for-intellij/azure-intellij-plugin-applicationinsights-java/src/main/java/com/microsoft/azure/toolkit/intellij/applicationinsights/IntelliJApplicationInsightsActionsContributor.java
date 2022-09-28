/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights;

import com.azure.resourcemanager.applicationinsights.models.ApplicationInsightsComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.applicationinsights.ApplicationInsightsActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.applicationinsights.connection.ApplicationInsightsResourceDefinition;
import com.microsoft.azure.toolkit.intellij.applicationinsights.creation.CreateApplicationInsightsAction;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightDraft;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJApplicationInsightsActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureApplicationInsights;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) ->
                CreateApplicationInsightsAction.create(e.getProject(), getDraftApplicationInsight(null));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateAccountHandler = (r, e) ->
                CreateApplicationInsightsAction.create(e.getProject(), getDraftApplicationInsight(r));
        am.registerHandler(ApplicationInsightsActionsContributor.GROUP_CREATE_APPLICATIONINSIGHT, (r, e) -> true, groupCreateAccountHandler);

        final BiPredicate<AzResource<?, ?, ?>, AnActionEvent> connectCondition = (r, e) -> r instanceof ApplicationInsight;
        final BiConsumer<AzResource<?, ?, ?>, AnActionEvent> connectHandler = (r, e) -> AzureTaskManager.getInstance().runLater(
                OperationBundle.description("resource.connect_resource.resource", r.getName()), () -> {
                    final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                    dialog.setResource(new AzureServiceResource<>(((ApplicationInsight) r), ApplicationInsightsResourceDefinition.INSTANCE));
                    dialog.show();
                });
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, connectCondition, connectHandler);
    }

    private static ApplicationInsightDraft getDraftApplicationInsight(@Nullable final ResourceGroup resourceGroup) {
        final List<Subscription> selectedSubscriptions = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        if (selectedSubscriptions.size() == 0) {
            return null;
        }
        final String timestamp = Utils.getTimestamp();
        final Subscription subscription = selectedSubscriptions.get(0);
        final Region region = Optional.ofNullable(resourceGroup).map(ResourceGroup::getRegion).orElse(null);
        final String resourceGroupName = resourceGroup == null ? String.format("rg-%s", timestamp) : resourceGroup.getResourceGroupName();
        final ApplicationInsightDraft applicationInsightDraft = Azure.az(AzureApplicationInsights.class).applicationInsights(subscription.getId())
                .create(String.format("ai-%s", timestamp), resourceGroupName);
        applicationInsightDraft.setRegion(region);
        return applicationInsightDraft;
    }

    @Override
    public int getOrder() {
        return ApplicationInsightsActionsContributor.INITIALIZE_ORDER + 1;
    }
}
