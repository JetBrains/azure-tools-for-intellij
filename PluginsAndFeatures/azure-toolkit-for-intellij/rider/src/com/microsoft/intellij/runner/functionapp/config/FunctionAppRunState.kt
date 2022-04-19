/**
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.toolkit.intellij.common.AzureRunProfileState
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.appserviceplan.AzureAppServicePlanMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import com.microsoft.intellij.RunProcessHandler
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.getAppUrl
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.openAppInBrowser
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.refreshAzureExplorer
import com.microsoft.intellij.runner.database.config.deploy.DatabaseDeployUtil.getOrCreateSqlDatabaseFromConfig
import com.microsoft.intellij.runner.database.model.DatabasePublishModel
import com.microsoft.intellij.runner.functionapp.config.runstate.FunctionAppDeployStateUtil.addConnectionString
import com.microsoft.intellij.runner.functionapp.config.runstate.FunctionAppDeployStateUtil.deployToAzureFunctionApp
import com.microsoft.intellij.runner.functionapp.config.runstate.FunctionAppDeployStateUtil.functionAppStart
import com.microsoft.intellij.runner.functionapp.config.runstate.FunctionAppDeployStateUtil.getOrCreateFunctionAppFromConfiguration
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import com.microsoft.intellij.runner.functionapp.model.FunctionAppSettingModel
import org.jetbrains.plugins.azure.RiderAzureBundle.message

data class FunctionAppDeployResult(val app: WebAppBase, val sqlDatabase: SqlDatabase?)

class FunctionAppRunState(project: Project, private val myModel: FunctionAppSettingModel) :
        AzureRunProfileState<FunctionAppDeployResult>(project) {

    private var isFunctionAppCreated = false
    private var isDatabaseCreated = false

    companion object {
        private const val TARGET_FUNCTION_NAME = "FunctionApp"
        private const val TARGET_FUNCTION_DEPLOYMENT_SLOT_NAME = "FunctionDeploymentSlot"
    }

    override fun getDeployTarget(): String =
            if (myModel.functionAppModel.isDeployToSlot) TARGET_FUNCTION_DEPLOYMENT_SLOT_NAME
            else TARGET_FUNCTION_NAME

    override fun executeSteps(processHandler: RunProcessHandler,
                              telemetryMap: MutableMap<String, String>): FunctionAppDeployResult {

        val publishableProject = myModel.functionAppModel.publishableProject
                ?: throw RuntimeException(message("process_event.publish.project.not_defined"))

        val subscriptionId = myModel.functionAppModel.subscription?.subscriptionId()
                ?: throw RuntimeException(message("process_event.publish.subscription.not_defined"))

        val collectArtifactsTimeoutMs = PropertiesComponent.getInstance().getInt(
                AzureRiderSettings.PROPERTY_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_NAME,
                AzureRiderSettings.VALUE_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_DEFAULT) * 60000L

        val app = getOrCreateFunctionAppFromConfiguration(myModel.functionAppModel, processHandler)
        tryConfigureAzureFunctionRuntimeStack(app, subscriptionId, processHandler)
        deployToAzureFunctionApp(project, publishableProject, app, processHandler, collectArtifactsTimeoutMs)

        isFunctionAppCreated = true

        var database: SqlDatabase? = null

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            database = getOrCreateSqlDatabaseFromConfig(myModel.databaseModel, processHandler)

            val databaseUri = AzureMvpModel.getInstance().getResourceUri(subscriptionId, database.id())
            if (databaseUri != null)
                processHandler.setText(message("process_event.publish.sql_db.url", databaseUri))

            if (myModel.databaseModel.connectionStringName.isEmpty())
                throw RuntimeException(message("process_event.publish.connection_string.not_defined"))

            if (myModel.databaseModel.sqlServerAdminLogin.isEmpty())
                throw RuntimeException(message("process_event.publish.sql_server.admin_login_not_defined"))

            if (myModel.databaseModel.sqlServerAdminPassword.isEmpty())
                throw RuntimeException(message("process_event.publish.sql_server.admin_password_not_defined"))

            addConnectionString(
                    subscriptionId,
                    app,
                    database,
                    myModel.databaseModel.connectionStringName,
                    myModel.databaseModel.sqlServerAdminLogin,
                    myModel.databaseModel.sqlServerAdminPassword,
                    processHandler)
        }

        isDatabaseCreated = true

        functionAppStart(app, processHandler)

        val url = getAppUrl(app)
        processHandler.setText(message("process_event.publish.url", url))
        processHandler.setText(message("process_event.publish.done"))

        return FunctionAppDeployResult(app, database)
    }

    private fun tryConfigureAzureFunctionRuntimeStack(app: WebAppBase, subscriptionId: String, processHandler: RunProcessHandler) {
        if (app !is FunctionApp) return

        val functionRuntimeStack = myModel.functionAppModel.functionRuntimeStack
        processHandler.setText(message("process_event.publish.updating_runtime", functionRuntimeStack.runtime(), functionRuntimeStack.version()))

        if (myModel.functionAppModel.operatingSystem == OperatingSystem.LINUX) {
            val appServicePlan = AzureAppServicePlanMvpModel
                    .getAppServicePlanById(subscriptionId, app.appServicePlanId())

            processHandler.setText(message("process_event.publish.updating_runtime.linux", functionRuntimeStack.linuxFxVersionForDedicatedPlan))

            // For Linux, we have to set the correct FunctionRuntimeStack
            app.update()
                    .withExistingLinuxAppServicePlan(appServicePlan)
                    .withBuiltInImage(functionRuntimeStack)
                    .apply()

            // For Linux dynamic (consumption) plan, we have to set SCM_DO_BUILD_DURING_DEPLOYMENT=false
            if (appServicePlan.pricingTier() == FunctionAppPublishModel.dynamicPricingTier) {

                processHandler.setText(message("process_event.publish.updating_appsettings.scm_build"))

                app.update()
                        .withAppSetting("SCM_DO_BUILD_DURING_DEPLOYMENT", "false")
                        .apply()
            }
        } else {
            // For Windows, we have to set the correct runtime and version
            app.update()
                    .withRuntime(functionRuntimeStack.runtime())
                    .withRuntimeVersion(functionRuntimeStack.version())
                    .apply()
        }
    }

    override fun onSuccess(result: FunctionAppDeployResult, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()

        // Refresh for both cases (when create new function app and publish into existing one)
        // to make sure separate functions are updated as well
        refreshAzureExplorer(listenerId = "FunctionModule")

        val app = result.app
        val sqlDatabase = result.sqlDatabase
        refreshAppsAfterPublish(app, myModel.functionAppModel)

        if (sqlDatabase != null) {
            refreshDatabaseAfterPublish(sqlDatabase, myModel.databaseModel)
        }

        showPublishNotification(message("notification.publish.publish_complete"), NotificationType.INFORMATION)

        val isOpenBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        if (isOpenBrowser) {
            openAppInBrowser(app, processHandler)
        }
    }

    override fun onFail(error: Throwable, processHandler: RunProcessHandler) {
        if (processHandler.isProcessTerminated || processHandler.isProcessTerminating) return

        if (isFunctionAppCreated) {
            AzureFunctionAppMvpModel.refreshSubscriptionToFunctionAppMap()
            AzureStorageAccountMvpModel.refreshStorageAccountsMap()
        }

        if (isDatabaseCreated)
            AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()

        showPublishNotification(message("notification.publish.publish_failed"), NotificationType.ERROR)

        processHandler.println(error.message, ProcessOutputTypes.STDERR)
        processHandler.notifyComplete()
    }

    private fun refreshAppsAfterPublish(app: WebAppBase, model: FunctionAppPublishModel) {
        model.resetOnPublish(app)
        AzureFunctionAppMvpModel.refreshSubscriptionToFunctionAppMap()
        AzureStorageAccountMvpModel.refreshStorageAccountsMap()
    }

    private fun refreshDatabaseAfterPublish(sqlDatabase: SqlDatabase, model: DatabasePublishModel) {
        model.resetOnPublish(sqlDatabase)
        AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
    }

    private fun showPublishNotification(text: String, type: NotificationType) {
        val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("Azure Web App Publish Message")
                .createNotification(text, type)

        Notifications.Bus.notify(notification, project)
    }
}