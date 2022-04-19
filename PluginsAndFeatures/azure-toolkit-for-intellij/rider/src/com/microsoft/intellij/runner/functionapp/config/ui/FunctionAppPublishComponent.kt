/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functionapp.config.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.ExistingOrNewSelector
import com.microsoft.intellij.ui.component.PublishableProjectComponent
import com.microsoft.intellij.ui.component.appservice.AppAfterPublishSettingPanel
import com.microsoft.intellij.ui.component.appservice.FunctionAppExistingComponent
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.setComponentsVisible
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsMsBuild
import org.jetbrains.plugins.azure.functions.run.localsettings.FunctionLocalSettingsUtil
import org.jetbrains.plugins.azure.functions.run.localsettings.FunctionsWorkerRuntime
import java.io.File
import javax.swing.JPanel

class FunctionAppPublishComponent(private val lifetime: Lifetime,
                                  private val project: Project,
                                  private val model: FunctionAppPublishModel) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private const val DEFAULT_APP_NAME = "functionapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-"

        private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
        private val netAppVersionRegex = Regex("net([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
    }

    private val pnlFunctionAppSelector = ExistingOrNewSelector(message("run_config.publish.form.function_app.existing_new_selector"))
    val pnlExistingFunctionApp = FunctionAppExistingComponent(lifetime.createNested())
    val pnlCreateFunctionApp = FunctionAppCreateNewComponent(lifetime.createNested())
    private val pnlProject = PublishableProjectComponent(project)
    private val pnlFunctionAppPublishSettings = AppAfterPublishSettingPanel()

    init {
        initProjectPanel()

        add(pnlFunctionAppSelector)
        add(pnlExistingFunctionApp, "growx")
        add(pnlCreateFunctionApp, "growx")
        add(pnlProject, "growx")
        add(pnlFunctionAppPublishSettings, "growx")

        initButtonGroupsState()

        initComponentValidation()
    }

    fun resetFromConfig(config: FunctionAppPublishModel, dateString: String) {
        if (config.publishableProject != null)
            pnlProject.cbProject.selectedItem = config.publishableProject

        pnlCreateFunctionApp.pnlAppName.txtAppName.text =
                if (config.appName.isEmpty()) "$DEFAULT_APP_NAME$dateString"
                else config.appName

        pnlCreateFunctionApp.pnlResourceGroup.txtResourceGroupName.text =
                if (config.resourceGroupName.isEmpty()) "$DEFAULT_RESOURCE_GROUP_NAME$dateString"
                else config.resourceGroupName

        pnlCreateFunctionApp.pnlHostingPlan.txtName.text =
                if (config.appServicePlanName.isEmpty()) "$DEFAULT_PLAN_NAME$dateString"
                else config.appServicePlanName

        pnlCreateFunctionApp.pnlStorageAccount.txtName.text =
                if (config.storageAccountName.isEmpty()) RandomStringUtils.randomAlphanumeric(4).lowercase()
                else config.storageAccountName

        // Is Creating New
        if (config.isCreatingNewApp) pnlFunctionAppSelector.rdoCreateNew.doClick()
        else pnlFunctionAppSelector.rdoUseExisting.doClick()

        // Resource Group
        if (config.isCreatingResourceGroup) pnlCreateFunctionApp.pnlResourceGroup.rdoCreateNew.doClick()
        else pnlCreateFunctionApp.pnlResourceGroup.rdoUseExisting.doClick()

        // Hosting Plan
        if (config.isCreatingAppServicePlan) pnlCreateFunctionApp.pnlHostingPlan.rdoCreateNew.doClick()
        else pnlCreateFunctionApp.pnlHostingPlan.rdoUseExisting.doClick()

        // Operating System
        if (config.operatingSystem == OperatingSystem.WINDOWS) pnlCreateFunctionApp.pnlOperatingSystem.rdoOperatingSystemWindows.doClick()
        else pnlCreateFunctionApp.pnlOperatingSystem.rdoOperatingSystemLinux.doClick()

        // Storage Account
        if (config.isCreatingStorageAccount) pnlCreateFunctionApp.pnlStorageAccount.rdoCreateNew.doClick()
        else pnlCreateFunctionApp.pnlStorageAccount.rdoUseExisting.doClick()

        // Deployment Slots
        pnlExistingFunctionApp.pnlDeploymentSlotSettings.loadFinished.adviseOnce(lifetime.createNested()) { slotsData ->
            val panel = pnlExistingFunctionApp.pnlDeploymentSlotSettings
            if (config.isDeployToSlot.xor(panel.checkBoxIsEnabled.isSelected)) {
                panel.checkBoxIsEnabled.doClick()

                if (config.appId.isNotEmpty() && config.appId == slotsData.appId && config.slotName.isNotEmpty() && slotsData.slots.isNotEmpty()) {
                    val slotToSelect = slotsData.slots.find { it.name() == config.slotName }
                    if (slotToSelect != null)
                        panel.cbDeploymentSlots.selectedItem = slotToSelect
                }
            }
        }

        // Settings
        val isOpenInBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        if (isOpenInBrowser)
            pnlFunctionAppPublishSettings.checkBoxOpenInBrowserAfterPublish.doClick()

        pnlExistingFunctionApp.pnlExistingAppTable.btnRefresh.isEnabled = false
    }

    fun applyConfig(model: FunctionAppPublishModel) {
        model.subscription = pnlCreateFunctionApp.pnlSubscription.cbSubscription.getSelectedValue()
        model.publishableProject = pnlProject.lastSelectedProject

        model.isCreatingNewApp = pnlFunctionAppSelector.isCreateNew
        model.appName = pnlCreateFunctionApp.pnlAppName.appName

        val selectedResource = pnlExistingFunctionApp.pnlExistingAppTable.lastSelectedResource
        val selectedApp = selectedResource?.resource

        model.appId = selectedApp?.id() ?: ""

        if (!model.isCreatingNewApp) {
            model.subscription = AzureMvpModel.getInstance()
                    .selectedSubscriptions
                    .find { it.subscriptionId() == selectedResource?.subscriptionId }
        }

        model.isCreatingResourceGroup = pnlCreateFunctionApp.pnlResourceGroup.isCreateNew
        if (pnlCreateFunctionApp.pnlResourceGroup.isCreateNew) {
            model.resourceGroupName = pnlCreateFunctionApp.pnlResourceGroup.resourceGroupName
        } else {
            model.resourceGroupName = pnlCreateFunctionApp.pnlResourceGroup.cbResourceGroup.getSelectedValue()?.name() ?: ""
        }

        if (pnlFunctionAppSelector.isCreateNew) {
            if (pnlCreateFunctionApp.pnlOperatingSystem.isWindows) {
                model.operatingSystem = OperatingSystem.WINDOWS
            } else {
                model.operatingSystem = OperatingSystem.LINUX
            }
        } else {
            model.operatingSystem = selectedApp?.operatingSystem() ?: OperatingSystem.WINDOWS
        }

        // Set runtime stack based on project config
        val publishableProject = model.publishableProject
        if (publishableProject != null && publishableProject.isDotNetCore) {
            val functionLocalSettings = FunctionLocalSettingsUtil.readFunctionLocalSettings(project, File(publishableProject.projectFilePath).parent)
            val workerRuntime = functionLocalSettings?.values?.workerRuntime ?: FunctionsWorkerRuntime.DotNetDefault

            val coreToolsVersion = FunctionsCoreToolsMsBuild.requestAzureFunctionsVersion(project, publishableProject.projectFilePath) ?: "V4"
            val netCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
            model.functionRuntimeStack = FunctionRuntimeStack(
                    workerRuntime.value,
                    "~" + coreToolsVersion.trimStart('v', 'V'),
                    "${workerRuntime.value}|$netCoreVersion",
                    "${workerRuntime.value}|$netCoreVersion")
        }

        val hostingPlan = pnlCreateFunctionApp.pnlHostingPlan
        model.isCreatingAppServicePlan = hostingPlan.isCreatingNew
        model.appServicePlanId         = hostingPlan.lastSelectedAppServicePlan?.id() ?: model.appServicePlanId
        model.appServicePlanName       = hostingPlan.hostingPlanName
        model.location                 = hostingPlan.cbLocation.getSelectedValue()?.region() ?: model.location
        model.pricingTier              = hostingPlan.cbPricingTier.getSelectedValue() ?: model.pricingTier

        val storageAccount = pnlCreateFunctionApp.pnlStorageAccount
        model.isCreatingStorageAccount = storageAccount.isCreatingNew
        model.storageAccountName       = storageAccount.storageAccountName
        model.storageAccountId         = storageAccount.cbStorageAccount.getSelectedValue()?.id() ?: ""
        model.storageAccountType       = storageAccount.cbStorageAccountType.getSelectedValue() ?: model.storageAccountType

        val slotSettings = pnlExistingFunctionApp.pnlDeploymentSlotSettings
        model.isDeployToSlot = slotSettings.checkBoxIsEnabled.isSelected
        model.slotName = slotSettings.cbDeploymentSlots.getSelectedValue()?.name() ?: model.slotName
    }

    //region Fill Values

    fun fillAppsTable(apps: List<ResourceEx<FunctionApp>>) =
            pnlExistingFunctionApp.fillAppsTable(apps, model.appId)

    fun fillSubscription(subscriptions: List<Subscription>) =
            pnlCreateFunctionApp.fillSubscription(subscriptions, model.subscription)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) =
            pnlCreateFunctionApp.fillResourceGroup(resourceGroups, model.resourceGroupName)

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) =
            pnlCreateFunctionApp.fillAppServicePlan(appServicePlans, model.appServicePlanId)

    fun fillLocation(locations: List<Location>) =
            pnlCreateFunctionApp.fillLocation(locations, model.location)

    fun fillPricingTier(pricingTiers: List<PricingTier>) =
            pnlCreateFunctionApp.fillPricingTier(pricingTiers, model.pricingTier)

    fun fillStorageAccount(storageAccounts: List<StorageAccount>) =
            pnlCreateFunctionApp.fillStorageAccount(storageAccounts, model.storageAccountId)

    fun fillStorageAccountType(storageAccountType: List<StorageAccountSkuType>) =
            pnlCreateFunctionApp.fillStorageAccountType(storageAccountType)

    fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        pnlProject.fillProjectComboBox(publishableProjects, model.publishableProject)

        if (model.publishableProject != null && publishableProjects.contains(model.publishableProject!!)) {
            pnlCreateFunctionApp.setOperatingSystemRadioButtons(model.publishableProject!!.isDotNetCore)
            pnlProject.lastSelectedProject = model.publishableProject
        }
    }

    //endregion Fill Values

    //region Project

    private fun initProjectPanel() {

        pnlProject.canBePublishedAction = { publishableProject -> publishableProject.isAzureFunction }

        pnlProject.listenerAction = { publishableProject ->
            pnlCreateFunctionApp.setOperatingSystemRadioButtons(publishableProject.isDotNetCore)
            pnlExistingFunctionApp.filterAppTableContent(publishableProject.isDotNetCore)

            val functionApp = pnlExistingFunctionApp.pnlExistingAppTable.lastSelectedResource?.resource
            if (functionApp != null)
                checkSelectedProjectAgainstFunctionAppRuntime(functionApp, publishableProject)
        }
    }

    private fun checkSelectedProjectAgainstFunctionAppRuntime(functionApp: FunctionApp, publishableProject: PublishableProjectModel) {
        if (functionApp.operatingSystem() == OperatingSystem.WINDOWS) {
            pnlExistingFunctionApp.setRuntimeMismatchWarning(false)
            return
        }

        // DOTNETCORE|2.0 -> 2.0
        val functionAppFrameworkVersion = functionApp.linuxFxVersion().split('|').getOrNull(1)

        // .NETCoreApp,Version=v2.0 -> 2.0
        val projectNetCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
        pnlExistingFunctionApp.setRuntimeMismatchWarning(
                functionAppFrameworkVersion != projectNetCoreVersion,
                message("run_config.publish.form.function_app.runtime_mismatch_warning", functionAppFrameworkVersion.toString(), projectNetCoreVersion)
        )
    }

    //endregion Project

    private fun getProjectNetCoreFrameworkVersion(publishableProject: PublishableProjectModel): String {
        val defaultVersion = "6.0"
        val currentFramework = getCurrentFrameworkId(publishableProject) ?: return defaultVersion
        return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value // netX.Y
                ?: netCoreAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value // .NETCoreApp,version=vX.Y
                ?: defaultVersion
    }

    private fun getCurrentFrameworkId(publishableProject: PublishableProjectModel): String? {
        val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
        return targetFramework?.currentTargetFrameworkId?.valueOrNull?.framework?.id
    }

    //region Button Group

    private fun initButtonGroupsState() {
        initFunctionAppButtonGroup()
    }

    private fun initFunctionAppButtonGroup() {
        pnlFunctionAppSelector.rdoUseExisting.addActionListener { toggleFunctionAppPanel(false) }
        pnlFunctionAppSelector.rdoCreateNew.addActionListener { toggleFunctionAppPanel(true) }
        toggleFunctionAppPanel(model.isCreatingNewApp)
    }

    private fun toggleFunctionAppPanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateFunctionApp)
        setComponentsVisible(!isCreatingNew, pnlExistingFunctionApp)
    }

    //endregion Button Group
}
