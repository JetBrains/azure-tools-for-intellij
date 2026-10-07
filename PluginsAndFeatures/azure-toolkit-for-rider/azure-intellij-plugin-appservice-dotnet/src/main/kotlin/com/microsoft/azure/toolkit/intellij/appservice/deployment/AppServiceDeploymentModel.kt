/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig

interface AppServiceDeploymentModel<out TConfig : AppServiceConfig> {
    val config: TConfig

    class DraftAppServiceModel<out TConfig : AppServiceConfig>(
        override val config: TConfig
    ) : AppServiceDeploymentModel<TConfig>

    class RemoteAppServiceModel<out TConfig : AppServiceConfig>(
        val resourceGroup: String,
        override val config: TConfig,
        val deploymentSlots: List<String>
    ) : AppServiceDeploymentModel<TConfig>
}

sealed interface AppServiceLoadState<out TConfig : AppServiceConfig> {
    data object Loading : AppServiceLoadState<Nothing>
    data class Loaded<out TConfig : AppServiceConfig>(val items: List<AppServiceDeploymentModel.RemoteAppServiceModel<TConfig>>) : AppServiceLoadState<TConfig>
    data class Error(val message: String) : AppServiceLoadState<Nothing>
}