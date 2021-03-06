/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration;

import java.util.concurrent.Future;

public interface ConfigurationLoader {

    Future<Configuration> loadAsync();

}
