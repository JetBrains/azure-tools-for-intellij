/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("InvalidBundleOrProperty")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.appservice.components.AppServiceComboBoxDotNetRender
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount

class WebAppContainerComboBox(project: Project) : WebAppComboBox(project) {
    companion object {
        private val LOG = logger<WebAppContainerComboBox>()
    }

    init {
        setRenderer(AppServiceComboBoxDotNetRender())
    }

    override fun loadAppServiceModels(): MutableList<AppServiceConfig> {
        try {
            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) {
                return mutableListOf()
            }

            val webApps = Azure.az(AzureWebApp::class.java).webApps()

            val modifiedWebApps = buildList {
                for (webApp in webApps.sortedBy { it.name }) {
                    if (webApp.runtime == null || webApp.runtime?.isWindows == true) continue

                    val config = convertAppServiceToConfig({ AppServiceConfig() }, webApp)
                    add(config)
                }
            }

            return modifiedWebApps.toMutableList()
        } catch (e: Exception) {
            LOG.error("Unable to load models", e)
            throw e
        }
    }

    override fun createResource() {
        val dialog = WebAppContainerCreationDialog(project)
        Disposer.register(this, dialog)
        setOkActionAndShowDialog(dialog)
    }
}

fun Row.webAppContainerComboBox(project: Project): Cell<WebAppContainerComboBox> {
    val component = WebAppContainerComboBox(project)
    return cell(component)
}