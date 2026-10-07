/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.microsoft.azure.toolkit.intellij.common.AzureContainerRegistryComboBox
import com.microsoft.azure.toolkit.intellij.common.ContainerRegistryModel
import com.microsoft.azure.toolkit.intellij.common.dockerContainerRegistryComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime
import com.microsoft.azure.toolkit.lib.common.model.Region
import javax.swing.JLabel
import javax.swing.JPanel

class FunctionAppContainerSettingEditor(project: Project) : SettingsEditor<FunctionAppContainerConfiguration>() {

    private val panel: JPanel
    private lateinit var containerRegistryComboBox: Cell<AzureContainerRegistryComboBox>
    private lateinit var repositoryLabel: Cell<JLabel>
    private lateinit var repositoryTextField: Cell<JBTextField>
    private lateinit var tagTextField: Cell<JBTextField>
    private lateinit var functionAppContainerComboBox: Cell<FunctionAppContainerComboBox>

    init {
        panel = panel {
            row("Container Registry:") {
                containerRegistryComboBox = dockerContainerRegistryComboBox(project)
                    .align(Align.FILL)
                    .resizableColumn()
            }
            row("Repository:") {
                repositoryLabel = label("")
                repositoryTextField = textField()
                label("Tag:")
                tagTextField = textField()
                    .columns(COLUMNS_TINY)
            }
            row("Function App:") {
                functionAppContainerComboBox = functionAppContainerComboBox(project)
                    .align(Align.FILL)
                Disposer.register(this@FunctionAppContainerSettingEditor, functionAppContainerComboBox.component)
            }
        }

        tagTextField.component.text = "latest"
        containerRegistryComboBox.component.addValueChangedListener(::onRegistryChanged)
    }

    private fun onRegistryChanged(value: ContainerRegistryModel) {
        repositoryLabel.component.text = "${value.address}/"
    }

    override fun resetEditorFrom(configuration: FunctionAppContainerConfiguration) {
        val state = configuration.state ?: return

        val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
        val pricingTier = PricingTier(state.pricingTier, state.pricingSize)

        val functionAppConfig = FunctionAppConfig
            .builder()
            .appName(state.functionAppName)
            .subscriptionId(state.subscriptionId)
            .resourceGroup(state.resourceGroupName)
            .region(region)
            .servicePlanName(state.appServicePlanName)
            .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
            .pricingTier(pricingTier)
            .runtime(RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE))
            .storageAccountName(state.storageAccountName)
            .storageAccountResourceGroup(state.storageAccountResourceGroup)
            .build()
        functionAppContainerComboBox.component.setConfigModel(functionAppConfig)
        functionAppContainerComboBox.component.setValue { AppServiceComboBox.isSameApp(it, functionAppConfig) }

        val imageNameParts = state.imageRepository?.let {
            val parts = it.split('/', limit = 2)
            if (parts.count() == 2) parts[0] to parts[1] else null
        }
        if (imageNameParts != null) {
            containerRegistryComboBox.component.setRegistry(imageNameParts.first)
            repositoryTextField.component.text = imageNameParts.second
        }
        tagTextField.component.text = state.imageTag

        functionAppContainerComboBox.component.reloadItems()
    }

    override fun applyEditorTo(configuration: FunctionAppContainerConfiguration) {
        val state = configuration.state ?: return

        val functionAppConfig = functionAppContainerComboBox.component.value
        val registry = containerRegistryComboBox.component.value
        val repository = repositoryTextField.component.text
        val tag = tagTextField.component.text

        state.apply {
            functionAppName = functionAppConfig?.appName
            subscriptionId = functionAppConfig?.subscriptionId
            resourceGroupName = functionAppConfig?.resourceGroup
            region = functionAppConfig?.region?.toString()
            appServicePlanName = functionAppConfig?.servicePlanName
            appServicePlanResourceGroupName = functionAppConfig?.servicePlanResourceGroup
            pricingTier = functionAppConfig?.pricingTier?.tier
            pricingSize = functionAppConfig?.pricingTier?.size
            storageAccountName = functionAppConfig?.storageAccountName
            storageAccountResourceGroup = functionAppConfig?.storageAccountResourceGroup
            imageRepository = "${registry?.address}/$repository"
            imageTag = tag
        }
    }

    override fun createEditor() = panel
}