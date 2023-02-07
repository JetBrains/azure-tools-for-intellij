///**
// * Copyright (c) 2022 JetBrains s.r.o.
// *
// * All rights reserved.
// *
// * MIT License
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// *
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package org.jetbrains.plugins.azure.functions
//
//import com.intellij.ide.util.PropertiesComponent
//import com.intellij.notification.*
//import com.intellij.openapi.application.PathManager
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.progress.ProgressManager
//import com.intellij.openapi.progress.Task
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.startup.StartupActivity
//import com.microsoft.intellij.configuration.AzureRiderSettings
//import org.jetbrains.plugins.azure.functions.projectTemplating.FunctionsCoreToolsTemplateManager
//import java.io.File
//
//@Suppress("DialogTitleCapitalization")
//@Deprecated("To be removed with 2022.3")
//class CleanupDeprecatedAzureCoreToolsConfigurationActivity : StartupActivity {
//
//    companion object {
//        private const val notificationGroupName = "Azure Functions"
//    }
//
//    @Suppress("DEPRECATION")
//    override fun runActivity(project: Project) {
//
//        val properties = PropertiesComponent.getInstance()
//        if (!properties.getBoolean(AzureRiderSettings.PROPERTY_FUNCTIONS_MIGRATE_CORETOOLS_PATH_NOTIFICATION, true))
//            return
//
//        // Is an old Azure Core Tools directory present?
//        val coreToolsDirectory = File(PathManager.getConfigPath()).resolve("azure-functions-coretools")
//        if (coreToolsDirectory.exists()) {
//            val notification = createMigrationNotification(project, coreToolsDirectory)
//
//            Notifications.Bus.notify(notification, project)
//        }
//    }
//
//    @Suppress("DEPRECATION")
//    private fun createMigrationNotification(project: Project, coreToolsDirectory: File): Notification {
//
//        val notification = NotificationGroupManager.getInstance()
//                .getNotificationGroup(notificationGroupName)
//                .createNotification(
//                        title = "Azure Functions",
//                        subtitle = "Unused version detected",
//                        content = "Rider detected an unused version of the Azure Core Tools that can be safely deleted.\n\nPath: '${coreToolsDirectory.absolutePath}'",
//                        type = NotificationType.INFORMATION
//                )
//
//        notification.appendDeleteCoreToolsAction(
//                project = project,
//                coreToolsDirectory = coreToolsDirectory)
//
//        notification.appendDisableCoreToolsMigrationAction()
//
//        return notification
//    }
//
//    private fun Notification.appendDeleteCoreToolsAction(project: Project, coreToolsDirectory: File) {
//        addAction(NotificationAction.createSimple("Delete unused") {
//            deleteCoreTools(project, coreToolsDirectory)
//            expire()
//        })
//    }
//
//    private fun Notification.appendDisableCoreToolsMigrationAction() {
//        addAction(NotificationAction.createSimple("Don't show again") {
//            disableNotification()
//            expire()
//        })
//    }
//
//    private fun deleteCoreTools(project: Project, coreToolsDirectory: File) {
//
//        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Deleting unused Azure Functions Core Tools...", false) {
//            override fun run(indicator: ProgressIndicator) {
//                indicator.isIndeterminate = true
//
//                @Suppress("DEPRECATION")
//                val logger = Logger.getInstance(CleanupDeprecatedAzureCoreToolsConfigurationActivity::class.java)
//
//                // Remove old Azure Core Tools directory
//                if (coreToolsDirectory.exists()) {
//                    try {
//                        coreToolsDirectory.deleteRecursively()
//                        logger.info("Finished cleanup of old Azure Core Tools directory: ${coreToolsDirectory.path}")
//                    } catch (e: Exception) {
//                        logger.warn("Error during cleanup of old Azure Core Tools directory: ${coreToolsDirectory.path}", e)
//                    }
//
//                    try {
//                        FunctionsCoreToolsTemplateManager.tryReload()
//                        logger.info("Finished removing Azure Functions project templates from old Azure Core Tools directory: ${coreToolsDirectory.path}")
//                    } catch (e: Exception) {
//                        logger.warn("Error while removing Azure Functions project templates from old Azure Core Tools directory: ${coreToolsDirectory.path}", e)
//                    }
//                }
//
//                // Remove old settings
//                val properties = PropertiesComponent.getInstance()
//                listOf("AzureFunctionsCoreToolsPath", "AzureFunctionsCoreToolsAllowPrerelease", "AzureFunctionCoreToolsCheckUpdates")
//                        .forEach {
//                            if (properties.getValue(it) != null) {
//                                properties.unsetValue(it)
//                                logger.info("Finished cleanup of old Azure Core Tools setting: $it")
//                            }
//                        }
//            }
//        })
//    }
//
//    @Suppress("DEPRECATION")
//    private fun disableNotification() = PropertiesComponent.getInstance()
//            .setValue(AzureRiderSettings.PROPERTY_FUNCTIONS_MIGRATE_CORETOOLS_PATH_NOTIFICATION, false)
//}