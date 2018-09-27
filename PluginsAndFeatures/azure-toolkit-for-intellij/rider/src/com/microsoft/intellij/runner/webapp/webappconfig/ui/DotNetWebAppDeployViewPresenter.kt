package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.idea.application
import com.jetbrains.rider.util.idea.lifetime
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.Signal
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader

class DotNetWebAppDeployViewPresenter<V : DotNetWebAppDeployMvpView>(private val myPanel: RiderWebAppSettingPanel) : MvpPresenter<V>() {

    companion object {
        private const val TASK_SUBSCRIPTION = "Collect Azure subscriptions"
        private const val TASK_WEB_APP = "Collect Azure web apps"
        private const val TASK_RESOURCE_GROUP = "Collect Azure resource groups"
        private const val TASK_APP_SERVICE_PLAN = "Collect Azure app service plans"
        private const val TASK_PRICING_TIER = "Collect Azure pricing tiers"
        private const val TASK_LOCATION = "Collect Azure locations"
        private const val TASK_SQL_DATABASE = "Collect Azure SQL databases"
        private const val TASK_SQL_SERVER = "Collect Azure SQL servers"
        private const val TASK_DATABASE_EDITION = "Collect Azure Database Edition"

        private const val CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions."
        private const val CANNOT_LIST_WEB_APP = "Failed to list web apps."
        private const val CANNOT_LIST_RESOURCE_GROUP = "Failed to list resource groups."
        private const val CANNOT_LIST_APP_SERVICE_PLAN = "Failed to list app service plan."
        private const val CANNOT_LIST_LOCATION = "Failed to list locations."
        private const val CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier."
        private const val CANNOT_LIST_SQL_DATABASE = "Failed to list SQL Database."
        private const val CANNOT_LIST_SQL_SERVER = "Failed to list SQL Server."
        private const val CANNOT_LIST_DATABASE_EDITION = "Failed to list SQL Database edition."
        private const val CANNOT_LIST_PUBLISHABLE_PROJECTS = "Failed to list publishable projects."
    }

    private val subscriptionSignal = Signal<List<Subscription>>()
    private val webAppSignal = Signal<List<ResourceEx<WebApp>>>()
    private val resourceGroupSignal = Signal<List<ResourceGroup>>()
    private val appServicePlanSignal = Signal<List<AppServicePlan>>()
    private val pricingTierSignal = Signal<List<PricingTier>>()
    private val locationSignal = Signal<List<Location>>()
    private val sqlDatabaseSignal = Signal<List<SqlDatabase>>()
    private val sqlServerSignal = Signal<List<SqlServer>>()
    private val databaseEditionSignal = Signal<List<DatabaseEditions>>()

    fun onRefresh(lifetime: Lifetime) {
        loadWebApps(lifetime, true)
    }

    fun onLoadWebApps(lifetime: Lifetime) {
        loadWebApps(lifetime, false)
    }

    fun onLoadSubscription(lifetime: Lifetime) {
        subscribe(lifetime, subscriptionSignal, TASK_SUBSCRIPTION, CANNOT_LIST_SUBSCRIPTION,
                { AzureMvpModel.getInstance().selectedSubscriptions },
                { mvpView.fillSubscription(it) })
    }

    fun onLoadResourceGroups(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime, resourceGroupSignal, TASK_RESOURCE_GROUP, CANNOT_LIST_RESOURCE_GROUP,
                { AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(subscriptionId) },
                { mvpView.fillResourceGroup(it) })
    }

    fun onLoadAppServicePlan(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime, appServicePlanSignal, TASK_APP_SERVICE_PLAN, CANNOT_LIST_APP_SERVICE_PLAN,
                { AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(subscriptionId) },
                { mvpView.fillAppServicePlan(it) })
    }

    fun onLoadLocation(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime, locationSignal, TASK_LOCATION, CANNOT_LIST_LOCATION,
                { AzureMvpModel.getInstance().listLocationsBySubscriptionId(subscriptionId) },
                { mvpView.fillLocation(it) })
    }

    fun onLoadPricingTier(lifetime: Lifetime) {
        subscribe(lifetime, pricingTierSignal, TASK_PRICING_TIER, CANNOT_LIST_PRICING_TIER,
                { AzureMvpModel.getInstance().listPricingTier() },
                { mvpView.fillPricingTier(it) })
    }

    fun onLoadSqlDatabase(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime, sqlDatabaseSignal, TASK_SQL_DATABASE, CANNOT_LIST_SQL_DATABASE,
                { AzureDatabaseMvpModel.listSqlDatabasesBySubscriptionId(subscriptionId).filter { it.name() != "master" } },
                { mvpView.fillSqlDatabase(it) })
    }

    fun onLoadSqlServers(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime, sqlServerSignal, TASK_SQL_SERVER, CANNOT_LIST_SQL_SERVER,
                { AzureDatabaseMvpModel.listSqlServersBySubscriptionId(subscriptionId, true).map { it.resource } },
                { mvpView.fillSqlServer(it) })
    }

    fun onLoadDatabaseEdition(lifetime: Lifetime) {
        subscribe(lifetime, databaseEditionSignal, TASK_DATABASE_EDITION, CANNOT_LIST_DATABASE_EDITION,
                { AzureDatabaseMvpModel.listDatabaseEditions() },
                { mvpView.fillDatabaseEdition(it) })
    }

    fun onLoadPublishableProjects(lifetime: Lifetime, project: Project) {
        project.solution.publishableProjectsModel.publishableProjects.advise(project.lifetime.createNested()) {
            if (it.newValueOpt != null) {
                application.invokeLater( {
                    if (lifetime.isTerminated) return@invokeLater
                    try {
                        mvpView.fillPublishableProject(project.solution.publishableProjectsModel.publishableProjects.values.toList())
                    } catch (e: Exception) {
                        errorHandler(CANNOT_LIST_PUBLISHABLE_PROJECTS, e)
                    }
                }, ModalityState.stateForComponent(myPanel.mainPanel))
            }
        }
    }

    private fun loadWebApps(lifetime: Lifetime, forceRefresh: Boolean) {
        subscribe(lifetime, webAppSignal, TASK_WEB_APP, CANNOT_LIST_WEB_APP,
                { AzureDotNetWebAppMvpModel.listWebApps(forceRefresh) },
                { mvpView.renderWebAppsTable(it) })
    }

    private fun <T>subscribe(lifetime: Lifetime,
                             signal: Signal<T>,
                             taskName: String,
                             errorMessage: String,
                             callableFunc: () -> T,
                             invokeLaterCallback: (T) -> Unit) {

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, taskName, false) {
            override fun run(indicator: ProgressIndicator) {
                val value = callableFunc()
                application.invokeLater({
                    if (lifetime.isTerminated) return@invokeLater
                    invokeLaterCallback(value)
                }, ModalityState.stateForComponent(myPanel.mainPanel))
            }

            override fun onThrowable(error: Throwable) {
                errorHandler(errorMessage, error as Exception)
                super.onThrowable(error)
            }
        })
    }

    private fun errorHandler(msg: String, e: Exception) {
        DefaultLoader.getIdeHelper().invokeLater {
            if (isViewDetached) {
                return@invokeLater
            }
            mvpView.onErrorWithException(msg, e)
        }
    }
}
