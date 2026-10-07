/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.ParamEditor;
import com.intellij.database.dataSource.url.TypeDescriptorUi;
import com.intellij.database.dataSource.url.TypesRegistryUi;
import com.intellij.database.dataSource.url.ui.BaseTypeDescriptor;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.Consumer;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MongoCosmosDbAccountTypeUIFactory implements TypesRegistryUi.TypeDescriptorUiFactory {
    private static final String TYPE_NAME = "cosmos_account_mongo";
    private static final String CAPTION = "Account";

    @Override
    public void createTypeDescriptor(@NotNull Consumer<? super TypeDescriptorUi> consumer) {
        consumer.consume(new BaseTypeDescriptor(TYPE_NAME) {
            @Override
            protected @NotNull ParamEditor createFieldImpl(@NlsContexts.Label @NotNull String s, @Nullable String s1, @NotNull DataInterchange dataInterchange) {
                return new AzureCosmosDbAccountParamEditor(DatabaseAccountKind.MONGO_DB, formatFieldCaption(CAPTION), dataInterchange);
            }
        });
    }
}
