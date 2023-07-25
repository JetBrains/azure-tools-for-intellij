package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.common.dotnetProjectComboBox
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingPanel
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput.AzureValueChangeBiListener
import javax.swing.JPanel

class RiderWebAppSettingPanel(private val project: Project, configuration: RiderWebAppConfiguration) : RiderAzureSettingPanel<RiderWebAppConfiguration>() {
    private val panel: JPanel
    private lateinit var webAppComboBox: Cell<WebAppComboBox>

    init {
        panel = panel {
            row("Project:") {
                dotnetProjectComboBox(project)
                        .align(Align.FILL)
                        .resizableColumn()
            }
            row("Web App:") {
                webAppComboBox = webAppComboBox(project)
                        .align(Align.FILL)
            }
        }

        webAppComboBox.component.addValueChangedListener(AzureValueChangeBiListener { value, previous ->
            onWebAppChanged(value, previous)
        })

        webAppComboBox.component.reloadItems()
    }

    override fun apply(configuration: RiderWebAppConfiguration) {
    }

    override fun reset(configuration: RiderWebAppConfiguration) {
    }

    override fun disposeEditor() {
    }

    override fun getMainPanel() = panel

    private fun onWebAppChanged(value: WebAppConfig, before: WebAppConfig?) {
        loadDeploymentSlot(value)
        loadAppSettings(value, before)
    }

    private fun loadDeploymentSlot(selectedWebApp: WebAppConfig) {

    }

    private fun loadAppSettings(value: WebAppConfig, before: WebAppConfig?) {

    }
}