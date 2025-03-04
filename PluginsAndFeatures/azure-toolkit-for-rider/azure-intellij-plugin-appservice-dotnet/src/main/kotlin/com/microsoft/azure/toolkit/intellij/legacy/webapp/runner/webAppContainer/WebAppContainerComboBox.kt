/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("InvalidBundleOrProperty")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.stream.Collectors

class WebAppContainerComboBox(project: Project) : WebAppComboBox(project) {
    init {
        setRenderer(AppComboBoxRender(true))
    }

    override fun loadAppServiceModels(): MutableList<AppServiceConfig> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            return mutableListOf()
        }

        return Azure.az(AzureWebApp::class.java)
            .webApps()
            .parallelStream()
            .filter { a -> a.runtime != null && a.runtime?.isWindows == false }
            .map { webApp -> convertAppServiceToConfig({ AppServiceConfig() }, webApp) }
            .sorted { a, b -> a.appName.compareTo(b.appName, true) }
            .collect(Collectors.toList())
    }

    override fun createResource() {
        val dialog = WebAppContainerCreationDialog(project)
        Disposer.register(this, dialog)
        val actionId: Action.Id<AppServiceConfig> = Action.Id.of("user/webapp.create_app.app")
        dialog.setOkAction(Action(actionId)
            .withLabel("Create")
            .withIdParam(AppServiceConfig::appName)
            .withSource { it }
            .withAuthRequired(false)
            .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.dockerWebAppComboBox(project: Project): Cell<WebAppContainerComboBox> {
    val component = WebAppContainerComboBox(project)
    return cell(component)
}