/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.utils

import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig

internal fun isSameApp(first: AppServiceConfig?, second: AppServiceConfig?): Boolean {
    if (first == null || second == null) return first === second
    return first.appName.equals(second.appName, ignoreCase = true) &&
            first.resourceGroup.equals(second.resourceGroup, ignoreCase = true) &&
            first.subscriptionId.equals(second.subscriptionId, ignoreCase = true)
}