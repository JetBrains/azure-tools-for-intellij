/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.core.dependencies.javaxannotation.Nonnull;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.LRUStack;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTableDraft;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollectionDraft;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainerDraft;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;

import javax.annotation.Nullable;

public class CreateCosmosContainerAction {
    public static void createSQLContainer(@Nonnull Project project, @Nonnull SqlDatabase database,
                                          @Nonnull final SqlContainerDraft.SqlContainerConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosSQLContainerCreationDialog dialog = new CosmosSQLContainerCreationDialog(project, database);
            dialog.getForm().setValue(data);
            final Action.Id<SqlContainerDraft.SqlContainerConfig> actionId = Action.Id.of("user/cosmos.create_container.container|database");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(SqlContainerDraft.SqlContainerConfig::getContainerId)
                .withIdParam(database.getName())
                .withSource(database)
                .withAuthRequired(true)
                .withHandler(c -> doCreateSqlContainer(database, c)));
            dialog.show();
        });
    }

    private static void doCreateSqlContainer(@Nonnull SqlDatabase database, @Nonnull final SqlContainerDraft.SqlContainerConfig config) {
        final SqlContainerDraft draft = database.containers().create(config.getContainerId(), database.getResourceGroupName());
        draft.setConfig(config);
        draft.commit();
        final LRUStack history = CacheManager.getUsageHistory(draft.getClass());
        history.push(draft);
    }

    public static void createMongoCollection(@Nonnull Project project, @Nonnull MongoDatabase database,
                                             @Nonnull final MongoCollectionDraft.MongoCollectionConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosMongoCollectionCreationDialog dialog = new CosmosMongoCollectionCreationDialog(project, database);
            dialog.getForm().setValue(data);
            final Action.Id<MongoCollectionDraft.MongoCollectionConfig> actionId = Action.Id.of("user/cosmos.create_collection.collection|database");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(MongoCollectionDraft.MongoCollectionConfig::getCollectionId)
                .withIdParam(database.getName())
                .withSource(database)
                .withAuthRequired(true)
                .withHandler(c -> doCreateMongoCollection(database, c)));
            dialog.show();
        });
    }

    private static void doCreateMongoCollection(@Nonnull MongoDatabase database, @Nullable final MongoCollectionDraft.MongoCollectionConfig config) {
        final MongoCollectionDraft draft = database.collections().create(config.getCollectionId(), database.getResourceGroupName());
        draft.setConfig(config);
        draft.commit();
        final LRUStack history = CacheManager.getUsageHistory(draft.getClass());
        history.push(draft);
    }

    public static void createCassandraTable(@Nonnull Project project, @Nonnull CassandraKeyspace keyspace,
                                            @Nonnull final CassandraTableDraft.CassandraTableConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosCassandraTableCreationDialog dialog = new CosmosCassandraTableCreationDialog(project, keyspace);
            dialog.getForm().setValue(data);
            final Action.Id<CassandraTableDraft.CassandraTableConfig> actionId = Action.Id.of("user/cosmos.create_table.table|keyspace");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(CassandraTableDraft.CassandraTableConfig::getTableId)
                .withIdParam(keyspace.getName())
                .withSource(keyspace)
                .withAuthRequired(true)
                .withHandler(config -> doCreateCassandraTable(keyspace, config)));
            dialog.show();
        });
    }

    private static void doCreateCassandraTable(@Nonnull CassandraKeyspace keyspace, @Nonnull final CassandraTableDraft.CassandraTableConfig config) {
        final CassandraTableDraft draft = keyspace.tables().create(config.getTableId(), keyspace.getResourceGroupName());
        draft.setConfig(config);
        draft.commit();
        final LRUStack history = CacheManager.getUsageHistory(draft.getClass());
        history.push(draft);
    }
}
