package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox
import com.microsoft.azure.toolkit.intellij.legacy.webapp.RiderWebAppCreationDialog
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import java.util.stream.Collectors

class WebAppComboBox(project: Project) : AppServiceComboBox<WebAppConfig>(project) {
    override fun refreshItems() {
        Azure.az(AzureWebApp::class.java).refresh()
        super.refreshItems()
    }

    override fun loadAppServiceModels(): MutableList<WebAppConfig> {
        val webApps = Azure.az(AzureWebApp::class.java).webApps()
        return webApps.stream().parallel()
                .sorted { a, b -> a.name.compareTo(b.name, true) }
                .map { webApp -> convertAppServiceToConfig({ WebAppConfig() }, webApp) }
                .filter { a -> a.subscription != null }
                .collect(Collectors.toList())
    }

    override fun createResource() {
        val webAppCreationDialog = RiderWebAppCreationDialog(project)
        webAppCreationDialog.setDeploymentVisible(false)
        webAppCreationDialog.setOkActionListener { webAppConfig ->
            setValue(webAppConfig)
            AzureTaskManager.getInstance().runLater(webAppCreationDialog::close)
        }
        webAppCreationDialog.show()
    }
}

fun Row.webAppComboBox(project: Project): Cell<WebAppComboBox> {
    val component = WebAppComboBox(project)
    return cell(component)
}