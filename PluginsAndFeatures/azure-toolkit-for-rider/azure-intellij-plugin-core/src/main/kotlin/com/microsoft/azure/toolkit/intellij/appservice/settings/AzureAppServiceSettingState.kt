/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.settings

import com.intellij.openapi.components.BaseState

class AzureAppServiceSettingState : BaseState() {
    var loadResourcesDuringStartup by property(true)
}
