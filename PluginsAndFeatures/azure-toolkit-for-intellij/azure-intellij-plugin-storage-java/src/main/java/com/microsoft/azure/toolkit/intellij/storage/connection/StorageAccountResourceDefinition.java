/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.ConnectionStringStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;


@Getter
public class StorageAccountResourceDefinition extends BaseStorageAccountResourceDefinition
    implements SpringSupported<IStorageAccount> {

    public static final StorageAccountResourceDefinition INSTANCE = new StorageAccountResourceDefinition();

    @Setter
    private TempData tempData;

    @Override
    public AzureFormJPanel<Resource<IStorageAccount>> getResourcePanel(Project project) {
        final StorageAccountResourcePanel panel = new StorageAccountResourcePanel();
        if (Objects.nonNull(this.tempData)) {
            panel.setMethod(this.tempData.getType());
            if (StringUtils.isNotBlank(this.tempData.getConnectionString())) {
                final ConnectionStringStorageAccount account = Azure.az(AzureStorageAccount.class).getOrInitByConnectionString(this.tempData.getConnectionString());
                if (Objects.nonNull(account)) {
                    panel.setValue(StorageAccountResourceDefinition.INSTANCE.define(account));
                }
            }
            this.tempData = null;
        }
        return panel;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        if (StringUtils.containsIgnoreCase(key, "blob")) {
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.endpoint", String.format("https://${%s_ACCOUNT_NAME}.blob%s", Connection.ENV_PREFIX, suffix)));
        } else if (StringUtils.containsIgnoreCase(key, "share")) {
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.endpoint", String.format("https://${%s_ACCOUNT_NAME}.file%s", Connection.ENV_PREFIX, suffix)));
        } else {
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.endpoint", String.format("https://${%s_ACCOUNT_NAME}.file%s", Connection.ENV_PREFIX, suffix)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.endpoint", String.format("https://${%s_ACCOUNT_NAME}.blob%s", Connection.ENV_PREFIX, suffix)));
        }
        return properties;
    }

    @Override
    public Map<String, String> getSpringPropertyTypes() {
        final Map<String, String> fields = new HashMap<>();
        fields.put("spring.cloud.azure.storage.blob.account-name", "Name");
        fields.put("spring.cloud.azure.storage.blob.account-key", "Access Key");
        fields.put("spring.cloud.azure.storage.blob.endpoint", "Endpoint Url");
        fields.put("spring.cloud.azure.storage.fileshare.account-name", "Name");
        fields.put("spring.cloud.azure.storage.fileshare.account-key", "Access Key");
        fields.put("spring.cloud.azure.storage.fileshare.endpoint", "Endpoint Url");
        return fields;
    }

    @Data
    @AllArgsConstructor
    public static class TempData {
        private int type; // 0: Azure, 1: Azurite, 2: ConnectionString
        private String connectionString;
    }
}
