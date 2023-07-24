///**
// * Copyright (c) 2018-2021 JetBrains s.r.o.
// *
// * All rights reserved.
// *
// * MIT License
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// *
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package com.microsoft.intellij.serviceexplorer.azure.database.actions
//
//import com.intellij.database.autoconfig.DataSourceDetector
//import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
//import com.microsoft.tooling.msservices.helpers.Name
//import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqldatabase.SqlDatabaseNode
//import com.microsoft.tooling.msservices.serviceexplorer.azure.database.sqlserver.SqlServerNode
//
//@Name("Open in Database Tools")
//class SqlDatabaseConnectDataSourceAction(private val databaseNode: SqlDatabaseNode)
//    : ConnectDataSourceAction(databaseNode) {
//
//    override fun populateConnectionBuilder(builder: DataSourceDetector.Builder): DataSourceDetector.Builder? {
//        val databaseServerNode = databaseNode.parent as SqlServerNode
//
//        val sqlServer = AzureSqlServerMvpModel.getSqlServerById(databaseServerNode.subscriptionId, databaseServerNode.sqlServerId)
//
//        return builder
//                .withName("Azure SQL Database - "  + databaseServerNode.sqlServerName + " - " + databaseNode.sqlDatabaseName)
//                .withDriverClass(AzureSqlConnectionStringBuilder.defaultDriverClass)
//                .withUrl(AzureSqlConnectionStringBuilder.build(
//                        sqlServer.fullyQualifiedDomainName(),
//                        databaseNode.sqlDatabaseName
//                ))
//                .withUser(sqlServer.administratorLogin())
//    }
//}
//
