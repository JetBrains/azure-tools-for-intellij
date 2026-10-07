/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webAppContainer

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.microsoft.azure.toolkit.intellij.common.AzureContainerRegistryComboBox
import com.microsoft.azure.toolkit.intellij.common.ContainerRegistryModel
import com.microsoft.azure.toolkit.intellij.common.dockerContainerRegistryComboBox
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime
import com.microsoft.azure.toolkit.lib.common.model.Region
import javax.swing.JLabel
import javax.swing.JPanel

class WebAppContainerSettingEditor(private val project: Project) : SettingsEditor<WebAppContainerConfiguration>() {

    private val panel: JPanel
    private lateinit var containerRegistryComboBox: Cell<AzureContainerRegistryComboBox>
    private lateinit var repositoryLabel: Cell<JLabel>
    private lateinit var repositoryTextField: Cell<JBTextField>
    private lateinit var tagTextField: Cell<JBTextField>
    private lateinit var webAppContainerComboBox: Cell<WebAppContainerComboBox>
    private lateinit var portSpinner: Cell<JBIntSpinner>

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
            row("Web App:") {
                webAppContainerComboBox = webAppContainerComboBox(project)
                    .align(Align.FILL)
                Disposer.register(this@WebAppContainerSettingEditor, webAppContainerComboBox.component)
            }
            row("Website Port:") {
                portSpinner = spinner(80..65535)
            }
        }

        tagTextField.component.text = "latest"
        containerRegistryComboBox.component.addValueChangedListener(::onRegistryChanged)
    }

    private fun onRegistryChanged(value: ContainerRegistryModel) {
        repositoryLabel.component.text = "${value.address}/"
    }

    override fun resetEditorFrom(configuration: WebAppContainerConfiguration) {
        val state = configuration.state ?: return

        val region = if (state.region.isNullOrEmpty()) null else Region.fromName(requireNotNull(state.region))
        val pricingTier = PricingTier(state.pricingTier, state.pricingSize)

        val webAppConfig = AppServiceConfig
            .builder()
            .appName(state.webAppName)
            .subscriptionId(state.subscriptionId)
            .resourceGroup(state.resourceGroupName)
            .region(region)
            .servicePlanName(state.appServicePlanName)
            .servicePlanResourceGroup(state.appServicePlanResourceGroupName)
            .pricingTier(pricingTier)
            .runtime(RuntimeConfig.fromRuntime(WebAppDockerRuntime.INSTANCE))
            .build()
        webAppContainerComboBox.component.setConfigModel(webAppConfig)
        webAppContainerComboBox.component.setValue { AppServiceComboBox.isSameApp(it, webAppConfig) }

        val imageNameParts = state.imageRepository?.let {
            val parts = it.split('/', limit = 2)
            if (parts.count() == 2) parts[0] to parts[1] else null
        }
        if (imageNameParts != null) {
            containerRegistryComboBox.component.setRegistry(imageNameParts.first)
            repositoryTextField.component.text = imageNameParts.second
        }
        tagTextField.component.text = state.imageTag

        portSpinner.component.number = state.port

        webAppContainerComboBox.component.reloadItems()
    }

    override fun applyEditorTo(configuration: WebAppContainerConfiguration) {
        val state = configuration.state ?: return

        val webAppConfig = webAppContainerComboBox.component.value
        val registry = containerRegistryComboBox.component.value
        val repository = repositoryTextField.component.text
        val tag = tagTextField.component.text
        val portValue = portSpinner.component.number

        state.apply {
            webAppName = webAppConfig?.appName
            subscriptionId = webAppConfig?.subscriptionId
            resourceGroupName = webAppConfig?.resourceGroup
            region = webAppConfig?.region?.toString()
            appServicePlanName = webAppConfig?.servicePlanName
            appServicePlanResourceGroupName = webAppConfig?.servicePlanResourceGroup
            pricingTier = webAppConfig?.pricingTier?.tier
            pricingSize = webAppConfig?.pricingTier?.size
            imageRepository = "${registry?.address}/$repository"
            imageTag = tag
            port = portValue
        }
    }

    override fun createEditor() = panel
}