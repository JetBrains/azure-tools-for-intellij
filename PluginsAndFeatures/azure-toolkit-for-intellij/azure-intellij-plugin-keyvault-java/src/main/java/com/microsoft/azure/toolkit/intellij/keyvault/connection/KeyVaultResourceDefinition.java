/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


@Getter
public class KeyVaultResourceDefinition extends BaseKeyVaultResourceDefinition
        implements SpringSupported<KeyVault> {

    public static final KeyVaultResourceDefinition INSTANCE = new KeyVaultResourceDefinition();

    @Override
    public AzureFormJPanel<Resource<KeyVault>> getResourcePanel(Project project) {
        return new KeyVaultResourcePanel();
    }

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        if (StringUtils.containsIgnoreCase(key, "certificate")) {
            properties.add(Pair.of("spring.cloud.azure.keyvault.certificate.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
        } else if (StringUtils.containsIgnoreCase(key, "secret")) {
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-sources[0].endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-source-enabled", "true"));
        } else {
            properties.add(Pair.of("spring.cloud.azure.keyvault.certificate.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-sources[0].endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-source-enabled", "true"));
        }
        return properties;
    }
}
