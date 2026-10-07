/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime

import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack
import com.azure.resourcemanager.appservice.models.NetFrameworkVersion
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig

class DotNetRuntimeConfig: RuntimeConfig() {
    var isDocker: Boolean = false
    var stack: RuntimeStack? = null
    var dotnetVersion: String? = null
    var frameworkVersion: NetFrameworkVersion? = null
    var functionStack: FunctionRuntimeStack? = null
}