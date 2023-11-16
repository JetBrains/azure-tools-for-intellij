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

@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.azure.functions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.rd.util.firstOrNull
import com.jetbrains.rdclient.util.idea.pumpMessages
import com.jetbrains.rider.nuget.RiderNuGetFacade
import com.jetbrains.rider.nuget.RiderNuGetHost
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.projectView.workspace.containingProjectEntity
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.time.Duration
import java.util.function.Function

class AzureCoreToolsMissingNupkgNotificationProvider : EditorNotificationProvider {

    companion object {

        private val waitForInstallDuration = Duration.ofSeconds(30)

        private fun hasKnownFileSuffix(file: VirtualFile): Boolean =
                file.extension.equals("cs", true) ||
                        file.extension.equals("vb", true) ||
                        file.extension.equals("fs", true)

        private val markerToTriggerMap = mapOf(
                // Default worker
                "Microsoft.Azure.WebJobs" to mapOf(
                        "BlobTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage.Blobs", "5.2.1")),
                        "QueueTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage.Queues", "5.2.0")),
                        "CosmosDBTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.CosmosDB", "4.4.0")),
                        "OrchestrationTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.DurableTask", "2.13.0")),
                        "EventGridTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventGrid", "3.3.1")),
                        "EventHubTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "6.0.2")),
                        "IoTHubTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "6.0.2")),
                        "ServiceBusTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.ServiceBus", "5.13.4")),
                        "SqlTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Sql", "3.0.461")),
                        "DaprPublish" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "0.17.0-preview01")),
                        "DaprInvoke" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "0.17.0-preview01")),
                        "DaprState" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "0.17.0-preview01")),
                        "DaprServiceInvocationTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "0.17.0-preview01")),
                        "DaprTopicTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "0.17.0-preview01")),
                ),

                // Isolated worker
                "Microsoft.Azure.Functions.Worker" to mapOf(
                        "BlobTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Storage.Blobs", "6.2.0")),
                        "QueueTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Storage.Queues", "5.2.0")),
                        "CosmosDBTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.CosmosDB", "4.4.2")),
                        "EventGridTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.EventGrid", "3.4.0")),
                        "EventHubTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.EventHubs", "6.0.1")),
                        "HttpTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Http", "3.1.0")),
                        "ServiceBusTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.ServiceBus", "5.14.1")),
                        "TimerTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Timer", "4.3.0")),
                        "SqlTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Sql", "3.0.461")),

                        "DaprPublish" to listOf(
                                PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                                PackageDependency("CloudNative.CloudEvents", "2.7.1")),
                        "DaprInvoke" to listOf(
                                PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                                PackageDependency("CloudNative.CloudEvents", "2.7.1")),
                        "DaprState" to listOf(
                                PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                                PackageDependency("CloudNative.CloudEvents", "2.7.1")),
                        "DaprServiceInvocationTrigger" to listOf(
                                PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                                PackageDependency("CloudNative.CloudEvents", "2.7.1")),
                        "DaprTopicTrigger" to listOf(
                                PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                                PackageDependency("CloudNative.CloudEvents", "2.7.1")),
                )
        )
    }

    private data class PackageDependency(val id: String, val version: String)

    override fun collectNotificationData(project: Project, file: VirtualFile): Function<FileEditor, EditorNotificationPanel?>? {
        if (PropertiesComponent.getInstance(project).getBoolean(AzureRiderSettings.DISMISS_NOTIFICATION_AZURE_FUNCTIONS_MISSING_NUPKG)) return null

        if (!hasKnownFileSuffix(file)) return null

        val fileContent = LoadTextUtil.loadText(file, 4096)

        // Check for known marker words
        val knownMarker = markerToTriggerMap.filter { fileContent.contains(it.key, true) }.firstOrNull() ?: return null

        // Determine project(s) to install into
        val installableProjects = WorkspaceModel.getInstance(project)
                .getProjectModelEntities(file, project)
                .mapNotNull { it.containingProjectEntity() }

        if (installableProjects.isEmpty()) return null

        // For every known trigger name, verify required dependencies are installed
        val riderNuGetFacade = RiderNuGetHost.getInstance(project).facade

        for ((triggerName, dependencies) in knownMarker.value) {
            for (dependency in dependencies) {
                if (fileContent.contains(triggerName, true)) {
                    for (installableProject in installableProjects) {
                        if (!riderNuGetFacade.isInstalled(installableProject, dependency.id)) {
                            return Function { _: FileEditor ->
                                createNotificationPanel(
                                        file,
                                        riderNuGetFacade,
                                        dependency,
                                        installableProject,
                                        project
                                )
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    private fun createNotificationPanel(
            file: VirtualFile,
            riderNuGetFacade: RiderNuGetFacade,
            dependency: PackageDependency,
            installableProject: ProjectModelEntity,
            project: Project
    ): EditorNotificationPanel {
        val panel = EditorNotificationPanel()
                .text(RiderAzureBundle.message("notification.function_app.missing_nupkg.title", dependency.id))

        panel.createActionLabel(RiderAzureBundle.message("notification.function_app.missing_nupkg.action.install"), {
            // Install, wait, and refresh editor notifications
            riderNuGetFacade.installForProject(installableProject.name, dependency.id, dependency.version)

            pumpMessages(waitForInstallDuration) {
                riderNuGetFacade.isInstalled(installableProject, dependency.id)
            }

            EditorNotifications.getInstance(project).updateNotifications(file)
        }, true)

        panel.createActionLabel(RiderAzureBundle.message("notification.function_app.missing_nupkg.action.dismiss"), {
            dismissNotification(file, project)
        }, true)

        return panel
    }

    private fun dismissNotification(file: VirtualFile, project: Project) {
        PropertiesComponent.getInstance(project).setValue(AzureRiderSettings.DISMISS_NOTIFICATION_AZURE_FUNCTIONS_MISSING_NUPKG, true)
        EditorNotifications.getInstance(project).updateNotifications(file)
    }

    private fun RiderNuGetFacade.isInstalled(installableProject: ProjectModelEntity, dependencyId: String) = this.host.nuGetProjectModel
            .projects[installableProject.getId(project)]
            ?.explicitPackages?.any { it.id.equals(dependencyId, ignoreCase = true) }
            ?: false
}