/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.launchOnShow
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindItems
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindSelected
import com.microsoft.azure.toolkit.intellij.appservice.utils.bindSelectedItem
import javax.swing.JPanel

class WebAppSettingEditor(
    project: Project,
    private val viewModel: WebAppSettingEditorViewModel
) : SettingsEditor<WebAppConfiguration>() {

    private val webAppTreePanel = WebAppDeploymentTreePanel(project, viewModel).also {
        Disposer.register(this, it)
    }

    private val panel: JPanel = panel {
        row("Project:") {
            comboBox(
                MutableCollectionComboBoxModel<PublishableProjectModel>(),
                renderer = SimpleListCellRenderer.create("") { it.projectName }
            )
                .bindItems(viewModel.publishableProjects)
                .bindSelectedItem(viewModel.selectedProject)
                .align(Align.FILL)
        }
        row("Configuration:") {
            comboBox(
                MutableCollectionComboBoxModel<ConfigurationAndPlatform>(),
                renderer = SimpleListCellRenderer.create("") { "${it.configuration} | ${it.platform}" }
            )
                .bindItems(viewModel.configurationAndPlatforms)
                .bindSelectedItem(viewModel.selectedConfigurationAndPlatform)
                .align(Align.FILL)
        }
        row {
            cell(webAppTreePanel.component)
                .align(Align.FILL)
                .resizableColumn()
        }.resizableRow()
        row {
            checkBox("Open browser after deployment")
                .bindSelected(viewModel.openBrowserAfterDeployment) { viewModel.setOpenBrowserFlag(it) }
        }
    }.also {
        it.launchOnShow("WebAppSettingEditor state observer") {
            viewModel.selectedAppService.collect {
                fireEditorStateChanged()
            }
        }
    }

    override fun resetEditorFrom(configuration: WebAppConfiguration) {
        val configurationOptions = configuration.state ?: return
        viewModel.setConfigFromOptions(configurationOptions)
    }

    override fun applyEditorTo(configuration: WebAppConfiguration) {
        val state = configuration.state ?: return
        viewModel.applySelectedConfigToOptions(state)
    }

    override fun createEditor() = panel
}
