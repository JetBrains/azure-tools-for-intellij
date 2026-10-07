/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig

class DotNetAppServiceConfig : AppServiceConfig() {
    var dotnetRuntime: DotNetRuntimeConfig? = null
}