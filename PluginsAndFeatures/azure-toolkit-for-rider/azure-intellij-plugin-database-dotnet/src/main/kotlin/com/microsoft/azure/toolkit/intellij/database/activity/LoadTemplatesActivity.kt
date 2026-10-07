/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.database.activity

import com.intellij.database.dataSource.DatabaseDriverImpl
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.url.template.UrlTemplate
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.microsoft.azure.toolkit.intellij.database.dbtools.DatabaseServerTypeUIFactory

class LoadTemplatesActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val manager = DatabaseDriverManager.getInstance()
        loadMySqlAzureTemplates(manager)
        loadPostgreSqlAzureTemplates(manager)
        loadSqlServerAzureTemplates(manager)
        loadAzureSqlDatabaseAzureTemplates(manager)
    }

    private fun loadMySqlAzureTemplates(manager: DatabaseDriverManager) {
        val driver = manager.getDriver("mysql.8") as? DatabaseDriverImpl ?: return
        val templates = driver.urlTemplates.toMutableList()
        templates.removeIf { it.template.contains(DatabaseServerTypeUIFactory.MYSQL) }
        val template = UrlTemplate("Azure", "jdbc:mysql://{host::localhost}?[:{port::3306}][/{database}?][/{account:az_mysql_server}?][\\?<&,user={user},password={password},{:identifier}={:param}>]")
        templates.add(template)
        driver.setURLTemplates(templates)
    }

    private fun loadPostgreSqlAzureTemplates(manager: DatabaseDriverManager) {
        val driver = manager.getDriver("postgresql") as? DatabaseDriverImpl ?: return
        val templates = driver.urlTemplates.toMutableList()
        templates.removeIf { it.template.contains(DatabaseServerTypeUIFactory.POSTGRE) }
        val template = UrlTemplate("Azure", "jdbc:postgresql://[{host::localhost}[:{port::5432}]][/{database:database/[^?]+:postgres}?][/{account:az_postgre_server}?][\\?<&,user={user:param},password={password:param},{:identifier}={:param}>]")
        templates.add(template)
        driver.setURLTemplates(templates)
    }

    private fun loadSqlServerAzureTemplates(manager: DatabaseDriverManager) {
        val driver = manager.getDriver("sqlserver.ms") as? DatabaseDriverImpl ?: return
        val templates = driver.urlTemplates.toMutableList()
        templates.removeIf { it.template.contains(DatabaseServerTypeUIFactory.SQLSERVER) }
        val template = UrlTemplate("Azure", "jdbc:sqlserver://{host:ssrp_host:localhost}[\\\\{instance:ssrp_instance}][:{port:ssrp_port}][/{account:az_sqlserver_server}?][;<;,user[Name]={user:param},password={password:param},database[Name]={database},{:identifier}={:param}>];?")
        templates.add(template)
        driver.setURLTemplates(templates)
    }

    private fun loadAzureSqlDatabaseAzureTemplates(manager: DatabaseDriverManager) {
        val driver = manager.getDriver("azure.ms") as? DatabaseDriverImpl ?: return
        val templates = driver.urlTemplates.toMutableList()
        templates.removeIf { it.template.contains(DatabaseServerTypeUIFactory.SQLSERVER) }
        val template = UrlTemplate("Azure", "jdbc:sqlserver://{host:host_ipv6:server.database.windows.net}[:{port::1433}][/{account:az_sqlserver_server}?][;<;,user[Name]={user:param},password={password:param},database[Name]={database},{:identifier}={:param}>];?")
        templates.add(template)
        driver.setURLTemplates(templates)
    }
}