/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MigrateStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@Nonnull Project project) {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            AzureEventBus.once("account.logged_in.account", (a, b) -> migrate(project));
        } else {
            migrate(project);
        }
    }

    private static void migrate(@Nonnull Project project) {
        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        final ConnectionManager manager = project.getService(ConnectionManager.class);
        final Map<String, List<Connection<?, ?>>> moduleConnections = manager
            .getConnections().stream()
            .collect(Collectors.groupingBy(c -> c.getConsumer().getName()));
        moduleConnections.forEach((moduleName, connections) -> {
            Optional.ofNullable(moduleManager.findModuleByName(moduleName))
                .map(AzureModule::from).filter(m -> !m.isInitialized())
                .ifPresent(module -> AzureTaskManager.getInstance().write(() -> {
                    final Environment env = module.initializeWithDefaultEnvIfNot();
                    connections.forEach(c -> {
                        try {
                            manager.removeConnection(c.getResource().getId(), c.getConsumer().getId());
                            env.addConnection(c);
                        } catch (final Exception e) {
                            AzureMessager.getMessager().error(e);
                        }
                    });
                    env.save();
                }));
        });
    }
}
