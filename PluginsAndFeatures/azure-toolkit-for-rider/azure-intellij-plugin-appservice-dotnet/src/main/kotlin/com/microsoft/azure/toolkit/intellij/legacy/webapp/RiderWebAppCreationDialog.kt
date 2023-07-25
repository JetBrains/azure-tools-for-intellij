package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog
import com.microsoft.azure.toolkit.intellij.legacy.appservice.RiderAppServiceInfoAdvancedPanel
import com.microsoft.azure.toolkit.intellij.legacy.appservice.RiderAppServiceInfoBasicPanel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.auth.IAccountActions
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import javax.swing.JPanel

class RiderWebAppCreationDialog(project: Project) : ConfigDialog<WebAppConfig>(project) {
    private val basicForm: RiderAppServiceInfoBasicPanel<WebAppConfig>
    private val advancedForm: RiderAppServiceInfoAdvancedPanel<WebAppConfig>
    private val panel: JPanel

    init {
        val selectedSubscriptions = Azure.az(AzureAccount::class.java).account().selectedSubscriptions
        if (selectedSubscriptions.isEmpty()) {
            this.close()
            throw AzureToolkitRuntimeException("there are no subscriptions selected in your account.", IAccountActions.SELECT_SUBS)
        }

        basicForm = RiderAppServiceInfoBasicPanel(project, selectedSubscriptions[0]) { WebAppConfig.getWebAppDefaultConfig(project.name) }
        basicForm.setDeploymentVisible(false)
        advancedForm = RiderAppServiceInfoAdvancedPanel(project) { WebAppConfig.getWebAppDefaultConfig(project.name) }

        panel = panel {
            row { cell(basicForm) }
            row { cell(advancedForm) }
        }

        this.init()

        setFrontPanel(basicFormPanel)
    }

    fun setDeploymentVisible(visible: Boolean) {
        basicForm.setDeploymentVisible(visible)
        advancedForm.setDeploymentVisible(visible)
        pack()
    }

    override fun createCenterPanel() = panel

    override fun getDialogTitle() = "Create Web App"

    override fun getBasicFormPanel() = basicForm

    override fun getAdvancedFormPanel() = advancedForm
}