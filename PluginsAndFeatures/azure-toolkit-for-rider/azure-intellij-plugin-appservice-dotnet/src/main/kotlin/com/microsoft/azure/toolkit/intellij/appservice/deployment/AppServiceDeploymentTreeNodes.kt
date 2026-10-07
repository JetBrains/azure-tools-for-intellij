/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.deployment

import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel.RemoteAppServiceModel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig

data class GroupNode(val name: String)
data class ResourceGroupNode(val name: String)

interface AppServiceNode<out TConfig : AppServiceConfig> {
    val appServiceModel: AppServiceDeploymentModel<TConfig>
}

data class WebAppNode(override val appServiceModel: AppServiceDeploymentModel<AppServiceConfig>) :
    AppServiceNode<AppServiceConfig>

data class FunctionAppNode(override val appServiceModel: AppServiceDeploymentModel<FunctionAppConfig>) :
    AppServiceNode<FunctionAppConfig>

data class DeploymentSlotNode<out TConfig : AppServiceConfig>(
    val slotName: String,
    val appServiceModel: RemoteAppServiceModel<TConfig>
)