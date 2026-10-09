/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.launchOnShow
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindIntValue
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindSelectedItem
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindText
import com.microsoft.azure.toolkit.intellij.common.dockerContainerRegistryComboBox
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp.WebAppDeploymentTreePanel
import javax.swing.JPanel

internal class WebAppContainerSettingEditor(
    project: Project,
    private val viewModel: WebAppContainerSettingEditorViewModel
) : SettingsEditor<WebAppContainerConfiguration>() {

    private val webAppTreePanel = WebAppDeploymentTreePanel(viewModel) {
        WebAppContainerCreationDialog(project)
    }.also {
        Disposer.register(this, it)
    }

    private val panel: JPanel = panel {
        row("Container Registry:") {
            dockerContainerRegistryComboBox(project)
                .applyToComponent { bindSelectedItem(viewModel.selectedContainerRegistry) }
                .align(Align.FILL)
                .resizableColumn()
        }
        row("Repository:") {
            label("No registry selected/").applyToComponent {
                launchOnShow("Container registry address binding") {
                    viewModel.selectedContainerRegistry.collect { registry ->
                        text = registry?.let { "${it.address}/" } ?: "No registry selected/"
                    }
                }
            }
            textField()
                .bindText(viewModel.imageRepository)
                .align(Align.FILL)
                .resizableColumn()
            label("Tag:")
            textField()
                .bindText(viewModel.imageTag)
                .columns(COLUMNS_TINY)
        }
        row("Website Port:") {
            spinner(80..65535)
                .bindIntValue(viewModel.port)
        }
        row {
            cell(webAppTreePanel.component)
                .align(Align.FILL)
                .resizableColumn()
        }.resizableRow()
    }.also {
        it.launchOnShow("WebAppContainerSettingEditor state observer") {
            viewModel.selectedAppService.collect {
                fireEditorStateChanged()
            }
        }
    }

    override fun resetEditorFrom(configuration: WebAppContainerConfiguration) {
        val state = configuration.state ?: return
        viewModel.setConfigFromOptions(state)
    }

    override fun applyEditorTo(configuration: WebAppContainerConfiguration) {
        val state = configuration.state ?: return
        viewModel.applySelectedConfigToOptions(state)
    }

    override fun createEditor() = panel
}
