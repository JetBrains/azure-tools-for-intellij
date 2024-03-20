package org.jetbrains.plugins.azure.functions

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.ui.EditorNotifications
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.firstOrNull
import com.jetbrains.rider.nuget.RiderNuGetFacade
import com.jetbrains.rider.nuget.RiderNuGetHost
import com.jetbrains.rider.projectView.workspace.*
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class FunctionMissingNugetPackageService(private val project: Project, private val scope: CoroutineScope) : LifetimedService() {
    companion object {
        fun getInstance(project: Project) = project.service<FunctionMissingNugetPackageService>()

        private val packageNames = setOf(
            "Microsoft.Azure.WebJobs.Extensions.Storage.Blobs",
            "Microsoft.Azure.WebJobs.Extensions.Storage.Queues",
            "Microsoft.Azure.WebJobs.Extensions.CosmosDB",
            "Microsoft.Azure.WebJobs.Extensions.DurableTask",
            "Microsoft.Azure.WebJobs.Extensions.EventGrid",
            "Microsoft.Azure.WebJobs.Extensions.EventHubs",
            "Microsoft.Azure.WebJobs.Extensions.ServiceBus",
            "Microsoft.Azure.WebJobs.Extensions.Sql",
            "Microsoft.Azure.WebJobs.Extensions.Dapr",
            "Microsoft.Azure.Functions.Worker.Extensions.Storage.Blobs",
            "Microsoft.Azure.Functions.Worker.Extensions.Storage.Queues",
            "Microsoft.Azure.Functions.Worker.Extensions.CosmosDB",
            "Microsoft.Azure.Functions.Worker.Extensions.EventGrid",
            "Microsoft.Azure.Functions.Worker.Extensions.EventHubs",
            "Microsoft.Azure.Functions.Worker.Extensions.Http",
            "Microsoft.Azure.Functions.Worker.Extensions.ServiceBus",
            "Microsoft.Azure.Functions.Worker.Extensions.Timer",
            "Microsoft.Azure.Functions.Worker.Extensions.Sql",
            "Microsoft.Azure.Functions.Worker.Extensions.Dapr",
            "CloudNative.CloudEvents"
        )

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
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprInvoke" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprState" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprServiceInvocationTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprTopicTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "0.17.0-preview01"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
            )
        )
    }

    init {
        val workspaceModelEvents = WorkspaceModelEvents.getInstance(project)
        workspaceModelEvents.addSignal.advise(serviceLifetime) {
            if (it.entity.isDependencyPackage()) {
                val packageName = it.entity.name.substringBefore("/")
                if (packageNames.contains(packageName)) {
                    cache.clear()
                }
            }
        }
        workspaceModelEvents.removeSignal.advise(serviceLifetime) {
            if (it.entity.isDependencyPackage()) {
                val packageName = it.entity.name.substringBefore("/")
                if (packageNames.contains(packageName)) {
                    cache.clear()
                }
            }
        }
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, MutableList<InstallableDependency>>>()

    data class PackageDependency(val id: String, val version: String)
    data class InstallableDependency(val dependency: PackageDependency, val installableProjectPath: Path)

    fun getMissingPackages(file: VirtualFile): List<InstallableDependency>? {
        val fileName = file.name
        val (modificationStamp, dependencies) = cache[fileName] ?: return null
        if (modificationStamp == file.modificationStamp) {
            return dependencies
        }

        cache.remove(fileName)
        return null
    }

    fun checkForMissingPackages(file: VirtualFile) {
        scope.launch {
            val modificationStamp = file.modificationStamp
            val dependencies = getInstallableDependencies(file).toMutableList()
            cache[file.name] = modificationStamp to dependencies

            withContext(Dispatchers.EDT) {
                EditorNotifications.getInstance(project).updateNotifications(file)
            }
        }
    }

    private suspend fun getInstallableDependencies(file: VirtualFile): List<InstallableDependency> {
        val fileContent = withContext(Dispatchers.IO) {
            file.readText()
        }

        if (fileContent.isEmpty()) return emptyList()

        // Check for known marker words
        val knownMarker = markerToTriggerMap
            .filter { fileContent.contains(it.key, true) }
            .firstOrNull()
            ?: return emptyList()

        // Determine project(s) to install into
        val installableProjects = WorkspaceModel.getInstance(project)
            .getProjectModelEntities(file, project)
            .mapNotNull { it.containingProjectEntity() }

        if (installableProjects.isEmpty()) return emptyList()

        // For every known trigger name, verify required dependencies are installed
        val riderNuGetFacade = RiderNuGetHost.getInstance(project).facade

        val installableDependencies = mutableListOf<InstallableDependency>()
        for (installableProject in installableProjects) {
            val path = installableProject.getFile()?.toPath() ?: continue
            for ((triggerName, dependencies) in knownMarker.value) {
                if (fileContent.contains(triggerName, true)) {
                    for (dependency in dependencies) {
                        if (!riderNuGetFacade.isInstalled(installableProject, dependency.id)) {
                            installableDependencies.add(InstallableDependency(dependency, path))
                        }
                    }
                }
            }
        }

        return installableDependencies
    }

    fun installPackage(file: VirtualFile, dependency: InstallableDependency) {
        scope.launch {
            val installableProject = WorkspaceModel.getInstance(project)
                .getProjectModelEntities(dependency.installableProjectPath, project)
                .firstOrNull()

            if (installableProject != null) {
                val riderNuGetFacade = RiderNuGetHost.getInstance(project).facade
                withContext(Dispatchers.EDT) {
                    riderNuGetFacade.installForProject(installableProject.name, dependency.dependency.id, dependency.dependency.version)
                }

                for (i in 0..<30) {
                    if (riderNuGetFacade.isInstalled(installableProject, dependency.dependency.id)) break
                    delay(1000)
                }
            }

            withContext(Dispatchers.EDT) {
                EditorNotifications.getInstance(project).updateNotifications(file)
            }
        }
    }

    private fun RiderNuGetFacade.isInstalled(installableProject: ProjectModelEntity, dependencyId: String) =
        host.nuGetProjectModel
            .projects[installableProject.getId(project)]
            ?.explicitPackages?.any { it.id.equals(dependencyId, ignoreCase = true) }
            ?: false
}