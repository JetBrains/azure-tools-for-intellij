package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureSettingsEditor

class RiderWebAppSettingEditor(project: Project, configuration: RiderWebAppConfiguration) :
    RiderAzureSettingsEditor<RiderWebAppConfiguration>() {

    private val panel = RiderWebAppSettingPanel(project, configuration)

    override fun getPanel() = panel
}