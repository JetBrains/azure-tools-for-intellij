package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel
import java.util.*

class DotNetWebAppSettingModel : WebAppSettingModel() {
    var appSettingsKey: String = UUID.randomUUID().toString()
    var appSettings: Map<String, String> = mapOf()
    var appSettingsToRemove: Set<String> = setOf()
    var openBrowserAfterDeployment: Boolean = true
    var slotPanelVisible: Boolean = true
    var artifactIdentifier: String = ""
    var packaging: String = ""
    var publishableProject: PublishableProjectModel? = null
    var configuration: String = ""
    var platform: String = ""
}