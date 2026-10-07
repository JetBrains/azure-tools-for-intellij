/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.utils

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper

internal fun isAccountSignedIn() {
    try {
        AzureLoginHelper.ensureAzureSubsAvailable()
    } catch (e: Exception) {
        throw RuntimeConfigurationError(e.message)
    }
}