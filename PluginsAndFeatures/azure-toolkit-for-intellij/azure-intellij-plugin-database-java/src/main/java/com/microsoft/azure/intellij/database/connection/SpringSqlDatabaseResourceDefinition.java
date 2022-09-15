/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.intellij.database.connection;

import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.intellij.database.connection.SqlDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public abstract class SpringSqlDatabaseResourceDefinition<T extends IDatabase> extends SqlDatabaseResourceDefinition<T> implements SpringSupported<T> {

    protected SpringSqlDatabaseResourceDefinition(String name, String title, String icon) {
        super(name, title, icon);
    }

    @Override
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.datasource.url", String.format("${%s_URL}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.datasource.username", String.format("${%s_USERNAME}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.datasource.password", String.format("${%s_PASSWORD}", Connection.ENV_PREFIX)));
        return properties;
    }
}
