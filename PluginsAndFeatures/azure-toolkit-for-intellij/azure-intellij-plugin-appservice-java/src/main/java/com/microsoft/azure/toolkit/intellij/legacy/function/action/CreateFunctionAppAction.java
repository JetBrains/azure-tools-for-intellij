/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.action;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppCreationDialog;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import rx.Single;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class CreateFunctionAppAction {
    @AzureOperation(name = "function.open_creation_dialog", type = AzureOperation.Type.ACTION)
    public static void openDialog(final Project project, @Nullable final FunctionAppConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final FunctionAppCreationDialog dialog = new FunctionAppCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.setData(data);
            }
            dialog.setOkActionListener((config) -> {
                dialog.close();
                createFunctionApp(config)
                    .subscribe(functionApp -> {
                    }, (error) -> {
                        final String title = String.format("Reopen dialog \"%s\"", dialog.getTitle());
                        final Consumer<Object> act = t -> openDialog(project, config);
                        final Action.Id<Object> REOPEN = Action.Id.of("function.reopen_creation_dialog");
                        final Action<?> action = new Action<>(REOPEN, act, new ActionView.Builder(title));
                        AzureMessager.getMessager().error(error, null, action);
                    });
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "function.create_app.app", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private static Single<FunctionAppBase<?,?,?>> createFunctionApp(final FunctionAppConfig config) {
        final AzureString title = description("function.create_app.app", config.getName());
        final IntellijAzureMessager actionMessenger = new IntellijAzureMessager() {
            @Override
            public boolean show(IAzureMessage raw) {
                if (raw.getType() != IAzureMessage.Type.INFO) {
                    return super.show(raw);
                }
                return false;
            }
        };
        final AzureTask<FunctionAppBase<?,?,?>> task = new AzureTask<>(null, title, false, () -> {
            final Operation operation = TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.CREATE_FUNCTION_APP);
            operation.trackProperties(config.getTelemetryProperties());
            try {
                OperationContext.current().setMessager(actionMessenger);
                final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setIndeterminate(true);
                return FunctionAppService.getInstance().createOrUpdateFunctionApp(config);
            } finally {
                operation.trackProperties(OperationContext.action().getTelemetryProperties());
                operation.complete();
            }
        });
        CacheManager.getUsageHistory(FunctionAppConfig.class).push(config);
        return AzureTaskManager.getInstance().runInBackgroundAsObservable(task).toSingle();
    }
}
