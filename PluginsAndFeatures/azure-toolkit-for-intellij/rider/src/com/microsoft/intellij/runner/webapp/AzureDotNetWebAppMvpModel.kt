package com.microsoft.intellij.runner.webapp

import com.jetbrains.rider.util.concurrentMapOf
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.appservice.*
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import java.io.IOException

object AzureDotNetWebAppMvpModel {

    class WebAppDefinition(val name: String,
                           val isCreatingResourceGroup: Boolean,
                           val resourceGroupName: String)

    class AppServicePlanDefinition(val name: String,
                                   val pricingTier: PricingTier,
                                   val region: String)

    //region Web App

    private val subscriptionIdToWebAppsMap = concurrentMapOf<String, List<ResourceEx<WebApp>>>()
    private val webAppToConnectionStringsMap = concurrentMapOf<WebApp, List<ConnectionString>>()

    fun listWebApps(force: Boolean): List<ResourceEx<WebApp>> {
        val webAppList = arrayListOf<ResourceEx<WebApp>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        subscriptions.forEach {
            webAppList.addAll(listWebAppsBySubscriptionId(it.subscriptionId(), force))
        }

        return webAppList
    }

    fun refreshSubscriptionToWebAppMap() {
        listWebApps(true)
    }

    fun listWebApps(operatingSystem: OperatingSystem, force: Boolean): List<ResourceEx<WebApp>> {
        return listWebApps(force).filter { it.resource.operatingSystem() == operatingSystem }
    }

    fun cleanWebApps() {
        subscriptionIdToWebAppsMap.clear()
    }

    private fun listWebAppsBySubscriptionId(subscriptionId: String, force: Boolean): List<ResourceEx<WebApp>> {

        if (!force && subscriptionIdToWebAppsMap.containsKey(subscriptionId)) {
            return subscriptionIdToWebAppsMap.getValue(subscriptionId)
        } else {
            val webAppList = arrayListOf<ResourceEx<WebApp>>()

            try {
                val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
                val webApps = azure.webApps().list()

                webApps.forEach {
                    webAppList.add(ResourceEx(it, subscriptionId))
                }

                subscriptionIdToWebAppsMap[subscriptionId] = webAppList
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return webAppList
        }
    }

    fun createWebAppWithNewWindowsAppServicePlan(subscriptionId: String,
                                                 webApp: WebAppDefinition,
                                                 appServicePlan:AppServicePlanDefinition,
                                                 netFrameworkVersion: NetFrameworkVersion): WebApp {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithNewWindowsAppServicePlan(azure, webApp, appServicePlan)
                .withNetFrameworkVersion(netFrameworkVersion)
                .create()
    }

    fun createWebAppWithNewLinuxAppServicePlan(subscriptionId: String,
                                               webApp: WebAppDefinition,
                                               appServicePlan: AppServicePlanDefinition,
                                               runtime: RuntimeStack): WebApp {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithNewLinuxAppServicePlan(azure, webApp, appServicePlan, runtime)
                .create()
    }

    fun createWebAppWithExistingWindowsAppServicePlan(subscriptionId: String,
                                                      webApp: WebAppDefinition,
                                                      appServicePlanId: String,
                                                      netFrameworkVersion: NetFrameworkVersion): WebApp {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithExistingWindowsAppServicePlan(azure, webApp, appServicePlanId)
                .withNetFrameworkVersion(netFrameworkVersion)
                .create()
    }

    fun createWebAppWithExistingLinuxAppServicePlan(subscriptionId: String,
                                                    webApp: WebAppDefinition,
                                                    appServicePlanId: String,
                                                    runtime: RuntimeStack): WebApp {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithExistingLinuxAppServicePlan(azure, webApp, appServicePlanId, runtime)
                .create()
    }

    fun getConnectionStrings(webApp: WebApp, force: Boolean = false): List<ConnectionString> {
        if (!force && webAppToConnectionStringsMap.containsKey(webApp)) {
            return webAppToConnectionStringsMap.getValue(webApp)
        }

        val connectionStrings = webApp.connectionStrings.values.toList()
        webAppToConnectionStringsMap[webApp] = connectionStrings

        return connectionStrings
    }

    fun checkConnectionStringNameExists(webApp: WebApp, connectionStringName: String, force: Boolean = false): Boolean {
        if (!force) {
            if (!webAppToConnectionStringsMap.containsKey(webApp)) return false
            return webAppToConnectionStringsMap.getValue(webApp).any { it.name() == connectionStringName }
        }

        val connectionStrings = getConnectionStrings(webApp, true)
        return connectionStrings.any { it.name() == connectionStringName }
    }

    //endregion Web App

    //region Check Existence

    /**
     * Check an Azure Resource Group name existence over azure portal
     *
     * Note: Method should be used in configuration validation logic.
     *       Suppress for now, because current configuration validation mechanism does not allow to easily make async call for validation
     */
    @Suppress("unused")
    fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String): Boolean {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.resourceGroups().contain(resourceGroupName)
    }

    //endregion Check Existence

    //region Private Methods and Operators

    private fun webAppWithNewWindowsAppServicePlan(azure: Azure,
                                                   webApp: WebAppDefinition,
                                                   appServicePlan: AppServicePlanDefinition) : WebApp.DefinitionStages.WithCreate {

        val withAppServicePlan = withCreateAppServicePlan(
                azure, appServicePlan.name, appServicePlan.region, appServicePlan.pricingTier, webApp.isCreatingResourceGroup, webApp.resourceGroupName, OperatingSystem.WINDOWS)

        val appWithRegion = azure.webApps().define(webApp.name).withRegion(appServicePlan.region)

        val appWithGroup =
                if (webApp.isCreatingResourceGroup) appWithRegion.withNewResourceGroup(webApp.resourceGroupName)
                else appWithRegion.withExistingResourceGroup(webApp.resourceGroupName)

        return appWithGroup.withNewWindowsPlan(withAppServicePlan)
    }

    private fun webAppWithNewLinuxAppServicePlan(azure: Azure,
                                                 webApp: WebAppDefinition,
                                                 appServicePlan: AppServicePlanDefinition,
                                                 runtime: RuntimeStack): WebApp.DefinitionStages.WithCreate {

        val withAppServicePlan = withCreateAppServicePlan(
                azure, appServicePlan.name, appServicePlan.region, appServicePlan.pricingTier, webApp.isCreatingResourceGroup, webApp.resourceGroupName, OperatingSystem.LINUX)

        val appWithRegion = azure.webApps().define(webApp.name).withRegion(appServicePlan.region)

        val appWithGroup =
                if (webApp.isCreatingResourceGroup) appWithRegion.withNewResourceGroup(webApp.resourceGroupName)
                else appWithRegion.withExistingResourceGroup(webApp.resourceGroupName)

        return appWithGroup.withNewLinuxPlan(withAppServicePlan).withBuiltInImage(runtime)
    }

    private fun withCreateAppServicePlan(azure: Azure,
                                         appServicePlanName: String,
                                         region: String,
                                         pricingTier: PricingTier,
                                         isCreatingResourceGroup: Boolean,
                                         resourceGroupName: String,
                                         operatingSystem: OperatingSystem): AppServicePlan.DefinitionStages.WithCreate {
        val planWithRegion = azure.appServices().appServicePlans()
                .define(appServicePlanName)
                .withRegion(region)

        val planWithGroup =
                if (isCreatingResourceGroup) planWithRegion.withNewResourceGroup(resourceGroupName)
                else planWithRegion.withExistingResourceGroup(resourceGroupName)

        return planWithGroup
                .withPricingTier(pricingTier)
                .withOperatingSystem(operatingSystem)
    }

    private fun webAppWithExistingWindowsAppServicePlan(azure: Azure,
                                                        webApp: WebAppDefinition,
                                                        appServicePlanId: String) : WebApp.DefinitionStages.WithCreate {

        val windowsPlan = withExistingAppServicePlan(azure, appServicePlanId)

        val withExistingWindowsPlan = azure.webApps().define(webApp.name).withExistingWindowsPlan(windowsPlan)

        return if (webApp.isCreatingResourceGroup) withExistingWindowsPlan.withNewResourceGroup(webApp.resourceGroupName)
        else withExistingWindowsPlan.withExistingResourceGroup(webApp.resourceGroupName)
    }

    private fun webAppWithExistingLinuxAppServicePlan(azure: Azure,
                                                      webApp: WebAppDefinition,
                                                      appServicePlanId: String,
                                                      runtime: RuntimeStack) : WebApp.DefinitionStages.WithCreate {

        val linuxPlan = withExistingAppServicePlan(azure, appServicePlanId)

        val withExistingLinuxPlan = azure.webApps().define(webApp.name).withExistingLinuxPlan(linuxPlan)

        val withResourceGroup =
                if (webApp.isCreatingResourceGroup) withExistingLinuxPlan.withNewResourceGroup(webApp.resourceGroupName)
                else withExistingLinuxPlan.withExistingResourceGroup(webApp.resourceGroupName)

        return withResourceGroup.withBuiltInImage(runtime)
    }

    private fun withExistingAppServicePlan(azure: Azure, appServicePlanId: String): AppServicePlan {
        return azure.appServices().appServicePlans().getById(appServicePlanId)
    }

    //endregion Private Methods and Operators
}
