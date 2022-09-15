/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public abstract class SqlDatabaseResourceDefinition<T extends IDatabase> extends AzureServiceResource.Definition<T> {
    protected SqlDatabaseResourceDefinition(String name, String title, String icon) {
        super(name, title, icon);
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<T> data, Project project) {
        final SqlDatabaseResource<T> resource = (SqlDatabaseResource<T>) data;
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_URL", Connection.ENV_PREFIX), resource.getJdbcUrl().toString());
        env.put(String.format("%s_USERNAME", Connection.ENV_PREFIX), resource.getUsername());
        env.put(String.format("%s_PASSWORD", Connection.ENV_PREFIX), Optional.ofNullable(resource.loadPassword()).or(() -> Optional.ofNullable(resource.inputPassword(project))).orElse(""));
        return env;
    }

    @Override
    public boolean write(@Nonnull Element resourceEle, @Nonnull Resource<T> paramResource) {
        final SqlDatabaseResource<T> resource = (SqlDatabaseResource<T>) paramResource;
        final String defName = resource.getDefinition().getName();

        final Password.SaveType saveType = resource.getPassword().saveType();
        resourceEle.addContent(new Element("url").setText(resource.getJdbcUrl().toString()));
        resourceEle.addContent(new Element("username").setText(resource.getUsername()));
        resourceEle.addContent(new Element("passwordSave").setText(saveType.name()));
        final char[] password = resource.getPassword().password();
        final String storedPassword = PasswordStore.loadPassword(defName, resource.getDataId(), resource.getUsername(), saveType);
        if (ArrayUtils.isNotEmpty(password) && !StringUtils.equals(String.valueOf(password), storedPassword)) {
            PasswordStore.savePassword(defName, resource.getDataId(), resource.getUsername(), resource.getPassword().password(), saveType);
        }

        resourceEle.setAttribute(new Attribute("id", resource.getId()));
        resourceEle.addContent(new Element("dataId").addContent(resource.getDataId()));

        return true;
    }

    @Override
    public Resource<T> read(@Nonnull Element resourceEle) {
        final String dataId = resourceEle.getChildTextTrim("dataId");

        if (StringUtils.isBlank(dataId)) {
            throw new AzureToolkitRuntimeException("Missing required dataId for database in service link.");
        }
        final SqlDatabaseResource<T> resource = new SqlDatabaseResource<>(dataId, resourceEle.getChildTextTrim("username"), this);

        final String defName = this.getName();
        resource.setJdbcUrl(JdbcUrl.from(resourceEle.getChildTextTrim("url")));

        resource.setPassword(new Password().saveType(Password.SaveType.valueOf(resourceEle.getChildTextTrim("passwordSave"))));
        if (resource.getPassword().saveType() == Password.SaveType.FOREVER) {
            PasswordStore.migratePassword(resource.getId(), resource.getUsername(), defName, resource.getId(), resource.getUsername());
        }
        final String savedPassword = PasswordStore.loadPassword(defName, resource.getId(), resource.getUsername(), resource.getPassword().saveType());
        if (StringUtils.isNotBlank(savedPassword)) {
            resource.getPassword().password(savedPassword.toCharArray());
        }
        return resource;
    }

    @Override
    public Resource<T> define(@Nonnull T resource) {
        return new SqlDatabaseResource<>(resource, null, this);
    }

    @Override
    public Resource<T> define(@Nonnull String dataId) {
        return new AzureServiceResource<>(getResource(dataId), this);
    }

    @Override
    public abstract T getResource(String dataId);

    @Override
    public abstract AzureFormJPanel<Resource<T>> getResourcePanel(Project project);
}
