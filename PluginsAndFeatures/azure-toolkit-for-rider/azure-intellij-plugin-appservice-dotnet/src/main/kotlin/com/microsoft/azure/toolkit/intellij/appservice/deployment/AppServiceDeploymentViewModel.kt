/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.DraftAppServiceModel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import kotlinx.coroutines.flow.StateFlow

interface AppServiceDeploymentViewModel<TConfig : AppServiceConfig> {
    val draftAppServiceState: StateFlow<List<DraftAppServiceModel<TConfig>>>
    val remoteAppServiceState: StateFlow<AppServiceLoadState<TConfig>>
    val selectedAppService: StateFlow<Pair<TConfig, String?>?>
    val isNetFramework: StateFlow<Boolean>

    fun selectAppService(appService: AppServiceDeploymentModel<TConfig>?, deploymentSlotName: String?)
    fun addDraftAppService(config: TConfig)
    fun refreshAppServices()
}