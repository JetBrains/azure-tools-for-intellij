/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;

public interface ContainerSettingView extends MvpView {

    void onListRegistries();

    void listRegistries(@NotNull final List<Registry> registries);

    void fillCredential(@NotNull final PrivateRegistryImageSetting setting);

    void disposeEditor();

    void setStartupFileVisible(boolean visible);
}
