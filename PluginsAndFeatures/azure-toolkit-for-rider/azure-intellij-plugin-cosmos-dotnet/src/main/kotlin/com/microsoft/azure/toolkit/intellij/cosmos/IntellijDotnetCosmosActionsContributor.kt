/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cosmos

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.ide.common.IActionsContributor
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog
import com.microsoft.azure.toolkit.intellij.cosmos.connection.CassandraCosmosDBAccountResourceDefinition
import com.microsoft.azure.toolkit.intellij.cosmos.connection.MongoCosmosDBAccountResourceDefinition
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase

class IntellijDotnetCosmosActionsContributor : IActionsContributor {
    override fun registerHandlers(am: AzureActionManager) {
        val mongoFunction = { account: MongoCosmosDBAccount ->
            account.mongoDatabases().list().stream().findFirst().orElse(null)
        }
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, e -> r is MongoCosmosDBAccount && r.formalStatus.isConnected }
        ) { r, e: AnActionEvent ->
            openResourceConnector(
                r as MongoCosmosDBAccount,
                mongoFunction,
                MongoCosmosDBAccountResourceDefinition.INSTANCE,
                e.project
            )
        }
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, e -> r is MongoDatabase && r.formalStatus.isConnected }
        ) { r, e: AnActionEvent ->
            openResourceConnector(
                r as MongoDatabase,
                MongoCosmosDBAccountResourceDefinition.INSTANCE,
                e.project
            )
        }

        val sqlFunction = { account: SqlCosmosDBAccount ->
            account.sqlDatabases().list().stream().findFirst().orElse(null)
        }
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, e -> r is SqlCosmosDBAccount && r.formalStatus.isConnected }
        ) { r, e: AnActionEvent ->
            openResourceConnector(
                r as SqlCosmosDBAccount,
                sqlFunction,
                SqlCosmosDBAccountResourceDefinition.INSTANCE,
                e.project
            )
        }
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, e -> r is SqlDatabase && r.formalStatus.isConnected }
        ) { r, e: AnActionEvent ->
            openResourceConnector(
                r as SqlDatabase,
                SqlCosmosDBAccountResourceDefinition.INSTANCE,
                e.project
            )
        }

        val cassandraFunction = { account: CassandraCosmosDBAccount ->
            account.keySpaces().list().stream().findFirst().orElse(null)
        }
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, e -> r is CassandraCosmosDBAccount && r.formalStatus.isConnected }
        ) { r, e: AnActionEvent ->
            openResourceConnector(
                r as CassandraCosmosDBAccount,
                cassandraFunction,
                CassandraCosmosDBAccountResourceDefinition.INSTANCE,
                e.project
            )
        }
        am.registerHandler(
            ResourceCommonActionsContributor.CONNECT,
            { r, e -> r is CassandraKeyspace && r.formalStatus.isConnected }
        ) { r, e: AnActionEvent ->
            openResourceConnector(
                r as CassandraKeyspace,
                CassandraCosmosDBAccountResourceDefinition.INSTANCE,
                e.project
            )
        }
    }

    private fun <T, R> openResourceConnector(
        account: R,
        databaseFunction: (R) -> T?,
        definition: AzureServiceResource.Definition<T>,
        project: Project?
    ) where T : AzResource, R : CosmosDBAccount {
        val database = databaseFunction(account)
        if (database == null) {
            AzureMessager
                .getMessager()
                .warning("Can not connect to ${account.name} as there is no database in selected account")
        } else {
            openResourceConnector(database, definition, project)
        }
    }

    private fun <T> openResourceConnector(
        resource: T,
        definition: AzureServiceResource.Definition<T>,
        project: Project?
    ) where T : AzResource {
        AzureTaskManager.getInstance().runLater {
            val dialog = ConnectorDialog(project)
            dialog.setResource(AzureServiceResource(resource, definition))
            dialog.show()
        }
    }
}