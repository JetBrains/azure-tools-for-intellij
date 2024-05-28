package org.jetbrains.plugins.azure.functions

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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
class FunctionMissingNugetPackageService(
    private val project: Project,
    private val scope: CoroutineScope
) : LifetimedService() {
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
                "BlobTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage.Blobs", "5.3.0")),
                "QueueTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage.Queues", "5.3.0")),
                "CosmosDBTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.CosmosDB", "4.6.1")),
                "OrchestrationTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.DurableTask", "2.13.3")),
                "EventGridTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventGrid", "3.4.1")),
                "EventHubTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "6.3.2")),
                "IoTHubTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "6.3.2")),
                "ServiceBusTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.ServiceBus", "5.15.1")),
                "SqlTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Sql", "3.0.534")),
                "DaprPublish" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.0")),
                "DaprInvoke" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.0")),
                "DaprState" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.0")),
                "DaprServiceInvocationTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.0")),
                "DaprTopicTrigger" to listOf(PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.0")),
            ),

            // Isolated worker
            "Microsoft.Azure.Functions.Worker" to mapOf(
                "BlobTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Storage.Blobs", "6.4.0")),
                "QueueTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Storage.Queues", "5.4.0")),
                "CosmosDBTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.CosmosDB", "4.8.1")),
                "EventGridTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.EventGrid", "3.4.1")),
                "EventHubTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.EventHubs", "6.3.1")),
                "HttpTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Http", "3.2.0")),
                "ServiceBusTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.ServiceBus", "5.18.0")),
                "TimerTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Timer", "4.3.0")),
                "SqlTrigger" to listOf(PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Sql", "3.0.534")),
                "DaprPublish" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.0"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprInvoke" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.0"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprState" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.0"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprServiceInvocationTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.0"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
                "DaprTopicTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.0"),
                    PackageDependency("CloudNative.CloudEvents", "2.7.1")
                ),
            )
        )

        private val LOG = logger<FunctionMissingNugetPackageService>()
    }

    init {
        val workspaceModelEvents = WorkspaceModelEvents.getInstance(project)
        workspaceModelEvents.addSignal.advise(serviceLifetime) {
            if (it.entity.isDependencyPackage()) {
                val packageName = it.entity.name.substringBefore("/")
                if (packageNames.contains(packageName)) {
                    LOG.trace("Package $packageName was installed")
                    cache.clear()
                }
            }
        }
        workspaceModelEvents.removeSignal.advise(serviceLifetime) {
            if (it.entity.isDependencyPackage()) {
                val packageName = it.entity.name.substringBefore("/")
                if (packageNames.contains(packageName)) {
                    LOG.trace("Package $packageName was removed")
                    cache.clear()
                }
            }
        }
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, MutableList<InstallableDependency>>>()

    data class PackageDependency(val id: String, val version: String)
    data class InstallableDependency(val dependency: PackageDependency, val installableProjectPath: Path)

    fun getMissingPackages(file: VirtualFile): List<InstallableDependency>? {
        val filePath = file.path
        LOG.trace("Getting packages for $filePath")

        val (modificationStamp, dependencies) = cache[filePath] ?: return null
        if (modificationStamp == file.modificationStamp) {
            LOG.trace("Found dependencies in cache for $filePath: ${dependencies.joinToString()}")
            return dependencies
        }

        LOG.trace("Removing $filePath from cache")
        cache.remove(filePath)
        return null
    }

    fun checkForMissingPackages(file: VirtualFile) {
        scope.launch {
            val modificationStamp = file.modificationStamp
            val dependencies = getInstallableDependencies(file).toMutableList()
            cache[file.path] = modificationStamp to dependencies
            LOG.trace("Saving dependencies to cache for ${file.path}: ${dependencies.joinToString()}")

            withContext(Dispatchers.EDT) {
                EditorNotifications.getInstance(project).updateNotifications(file)
            }
        }
    }

    fun clearCache() {
        LOG.trace("Clearing the cache")
        cache.clear()
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
            LOG.trace("Installing dependency $dependency for ${file.path}")
            val installableProject = WorkspaceModel.getInstance(project)
                .getProjectModelEntities(dependency.installableProjectPath, project)
                .firstOrNull()

            if (installableProject != null) {
                LOG.trace("Installing dependency $dependency in project ${installableProject.name}")
                val riderNuGetFacade = RiderNuGetHost.getInstance(project).facade
                withContext(Dispatchers.EDT) {
                    riderNuGetFacade.installForProject(
                        installableProject.name,
                        dependency.dependency.id,
                        dependency.dependency.version
                    )
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