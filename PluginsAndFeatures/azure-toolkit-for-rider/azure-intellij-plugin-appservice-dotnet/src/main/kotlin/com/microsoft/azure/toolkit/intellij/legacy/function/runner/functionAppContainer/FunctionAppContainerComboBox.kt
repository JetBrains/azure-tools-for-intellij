/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DuplicatedCode")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.appservice.components.AppServiceComboBoxDotNetRender
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionAppComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.auth.AzureAccount

class FunctionAppContainerComboBox(project: Project) : FunctionAppComboBox(project) {
    companion object {
        private val LOG = logger<FunctionAppContainerComboBox>()
    }

    init {
        setRenderer(AppServiceComboBoxDotNetRender())
    }

    override fun loadAppServiceModels(): MutableList<FunctionAppConfig> {
        try {
            val account = Azure.az(AzureAccount::class.java).account()
            if (!account.isLoggedIn) {
                return mutableListOf()
            }

            val functionApps = Azure.az(AzureFunctions::class.java).functionApps()

            val modifiedFunctionApps = buildList {
                for (functionApp in functionApps.sortedBy { it.name }) {
                    if (functionApp.runtime == null || functionApp.runtime?.isWindows == true) continue

                    val config = convertAppServiceToConfig({ FunctionAppConfig() }, functionApp)
                    add(config)
                }
            }

            return modifiedFunctionApps.toMutableList()
        } catch (e: Exception) {
            LOG.error("Unable to load models", e)
            throw e
        }
    }

    override fun createResource() {
        val dialog = FunctionAppContainerCreationDialog(project)
        Disposer.register(this, dialog)
        setOkActionAndShowDialog(dialog)
    }
}

fun Row.functionAppContainerComboBox(project: Project): Cell<FunctionAppContainerComboBox> {
    val component = FunctionAppContainerComboBox(project)
    return cell(component)
}