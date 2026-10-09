/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentTreePanel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceNode
import com.microsoft.azure.toolkit.intellij.appservice.deployment.FunctionAppNode
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.common.action.Action

internal class FunctionAppDeploymentTreePanel(
    vm: AppServiceDeploymentViewModel<FunctionAppConfig>,
    createDialog: () -> ConfigDialog<FunctionAppConfig>
) :
    AppServiceDeploymentTreePanel<FunctionAppConfig>(
        vm,
        "Search function apps...",
        "No function apps found",
        { vm, panel ->
            val dialog = createDialog()
            Disposer.register(panel, dialog as? Disposable ?: dialog.disposable)
            dialog.setOkAction(
                Action<FunctionAppConfig>(Action.Id.of("user/function.create_app.app"))
                    .withLabel("Create")
                    .withIdParam(FunctionAppConfig::appName)
                    .withSource { it }
                    .withAuthRequired(false)
                    .withHandler { config -> vm.addDraftAppService(config) }
            )
            dialog.show()
        }
    ) {
    override fun createAppNode(appServiceModel: AppServiceDeploymentModel<FunctionAppConfig>): AppServiceNode<FunctionAppConfig> {
        return FunctionAppNode(appServiceModel)
    }
}
