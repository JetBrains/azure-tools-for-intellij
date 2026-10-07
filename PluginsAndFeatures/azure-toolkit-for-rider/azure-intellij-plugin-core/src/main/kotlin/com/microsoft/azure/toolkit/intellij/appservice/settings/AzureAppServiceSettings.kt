/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
    name = "com.microsoft.azure.toolkit.intellij.appservice.settings.AzureAppServiceSettings",
    storages = [(Storage("AzureSettings.xml"))]
)
@Service
class AzureAppServiceSettings :
    SimplePersistentStateComponent<AzureAppServiceSettingState>(AzureAppServiceSettingState()) {
    companion object {
        fun getInstance() = service<AzureAppServiceSettings>()
    }

    var loadResourcesDuringStartup
        get() = state.loadResourcesDuringStartup
        set(value) {
            state.loadResourcesDuringStartup = value
        }
}
