/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DuplicatedCode")

package com.microsoft.azure.toolkit.intellij.appservice

import com.azure.core.exception.HttpResponseException
import com.azure.resourcemanager.appservice.models.OperatingSystem
import com.intellij.execution.ExecutionException
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.toolkit.intellij.legacy.ArtifactService
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service(Service.Level.PROJECT)
class DotNetAppServiceDeployer(private val project: Project) {
    companion object {
        fun getInstance(project: Project): DotNetAppServiceDeployer = project.service()
        private val LOG = logger<DotNetAppServiceDeployer>()
    }

    suspend fun deploy(
        target: WebAppBase<*, *, *>,
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        target.status = AzResource.Status.DEPLOYING

        checkIfTargetIsValid(target)

        val publishProjectResult = ArtifactService
            .getInstance(project)
            .publishProjectToFolder(publishableProject, configuration, platform, updateStatusText)
            .onFailure {
                return Result.failure(it)
            }

        val artifactFolder = publishProjectResult.getOrThrow()
        updateStatusText("Creating ${publishableProject.projectName} project ZIP...")
        val zipFile = packageWebAppArtifactDirectory(artifactFolder)
            ?: return Result.failure(ExecutionException("Unable to create zip archive with project artifacts"))
        updateStatusText("Project ZIP is created: ${zipFile.absolutePath}")

        return deploy(target, zipFile, updateStatusText)
    }

    suspend fun deploy(
        target: FunctionAppBase<*, *, *>,
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        target.status = AzResource.Status.DEPLOYING

        checkIfTargetIsValid(target)

        val publishProjectResult = ArtifactService
            .getInstance(project)
            .publishProjectToFolder(publishableProject, configuration, platform, updateStatusText)
            .onFailure {
                return Result.failure(it)
            }

        val artifactFolder = publishProjectResult.getOrThrow()
        updateStatusText("Creating ${publishableProject.projectName} project ZIP...")
        val zipFile = packageFunctionAppArtifactDirectory(artifactFolder)
            ?: return Result.failure(ExecutionException("Unable to create zip archive with project artifacts"))
        updateStatusText("Project ZIP is created: ${zipFile.absolutePath}")

        return if (target.isFlexConsumptionApp) {
            deployFlexConsumption(target, zipFile, updateStatusText)
        } else {
            deploy(target, zipFile, updateStatusText)
        }
    }

    private fun checkIfTargetIsValid(target: AppServiceAppBase<*, *, *>) {
        if (target !is FunctionAppBase<*, *, *> || target.isFlexConsumptionApp) return

        val appSettings = target.appSettings

        val websiteRunFromPackage = appSettings?.get("WEBSITE_RUN_FROM_PACKAGE")
        if (websiteRunFromPackage == null && !(target.appServicePlan?.pricingTier?.isConsumption == true && target.remote?.operatingSystem() == OperatingSystem.LINUX)) {
            Notification(
                "Azure AppServices",
                "Invalid application settings",
                "The WEBSITE_RUN_FROM_PACKAGE environment variable is not set in the application settings. This can prevent successful deployment.",
                NotificationType.WARNING
            )
                .addAction(NotificationAction.createSimple("Open application on the Portal") {
                    BrowserUtil.open(target.portalUrl)
                })
                .notify(project)
            return
        }

        if (websiteRunFromPackage?.startsWith("http") == true) {
            Notification(
                "Azure AppServices",
                "Invalid application settings",
                "The WEBSITE_RUN_FROM_PACKAGE environment variable is set to an URL in the application settings. This can prevent successful deployment.",
                NotificationType.WARNING
            )
                .addAction(NotificationAction.createSimple("Open application on the Portal") {
                    BrowserUtil.open(target.portalUrl)
                })
                .notify(project)
        }
    }

    private fun packageWebAppArtifactDirectory(artifactFolder: File): File? {
        try {
            val zipFile = Files.createTempFile(artifactFolder.nameWithoutExtension, ".zip").toFile()
            ZipUtil.pack(artifactFolder, zipFile)
            return zipFile
        } catch (e: IOException) {
            LOG.error("Unable to package the artifact directory", e)
            return null
        }
    }

    private fun packageFunctionAppArtifactDirectory(artifactFolder: File): File? {
        try {
            val zipFile = Files.createTempFile(artifactFolder.nameWithoutExtension, ".zip").toFile()
            ZipUtil.pack(artifactFolder, zipFile)
            ZipUtil.removeEntry(zipFile, "local.settings.json")
            return zipFile
        } catch (e: IOException) {
            LOG.error("Unable to package the artifact directory", e)
            return null
        }
    }

    private suspend fun deploy(
        target: AppServiceAppBase<*, *, *>,
        zipFile: File,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        val webAppBase = target.remote
        if (webAppBase == null) {
            LOG.warn("Unable to find remote of the target")
            return Result.failure(ExecutionException("Unable to get remote Azure resource"))
        }

        updateStatusText("Starting deployment...")
        updateStatusText("Trying to deploy artifact to ${webAppBase.name()}...")

        try {
            webAppBase.zipDeployAsync(zipFile)?.awaitSingleOrNull()
        } catch (e: HttpResponseException) {
            LOG.warn("Unable to deploy artifact to Azure resource due to the unsuccessful status code", e)
            return Result.failure(ExecutionException(e.message))
        } catch (e: Exception) {
            LOG.warn("Unable to deploy artifact to Azure resource", e)
            return Result.failure(ExecutionException(e.message))
        }

        updateStatusText("Successfully deployed the artifact to ${webAppBase.defaultHostname()}")

        FileUtil.delete(zipFile)

        startTargetApp(target, updateStatusText)

        return Result.success(Unit)
    }

    private suspend fun deployFlexConsumption(
        target: FunctionAppBase<*, *, *>,
        zipFile: File,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        val kuduManager = target.kuduManager
        if (kuduManager == null) {
            LOG.warn("Unable to find kudu manager")
            return Result.failure(ExecutionException("Unable to find kudu manager"))
        }

        updateStatusText("Starting deployment...")
        updateStatusText("Trying to deploy artifact to ${target.name}...")

        try {
            kuduManager.flexZipDeploy(zipFile)
            kuduManager.checkLatestDeploymentStatus(500.milliseconds.toJavaDuration(), 450)

            updateStatusText("Waiting for sync triggers, it may take some moments...")

            delay(60.seconds)

            val adminClient = target.adminClient
            if (adminClient != null) {
                updateStatusText("Checking the health of the function app...")
                val hostStatus = adminClient.getHostStatus(2.seconds.toJavaDuration(), 15)
                if (hostStatus != true) {
                    return Result.failure(ExecutionException("Deployment was successful but the app appears to be unhealthy. Please check the app logs."))
                }
            }
        } catch (e: Exception) {
            LOG.warn("Unable to deploy artifact to Azure resource", e)
            return Result.failure(ExecutionException(e.message))
        }

        updateStatusText("Successfully deployed the artifact to ${target.hostName}")

        FileUtil.delete(zipFile)

        startTargetApp(target, updateStatusText)

        return Result.success(Unit)
    }

    private fun startTargetApp(target: AppServiceAppBase<*, *, *>, updateStatusText: (String) -> Unit) {
        if (target.formalStatus.isRunning) return

        updateStatusText("Starting the application after deploying artifacts...")
        target.start()
        updateStatusText("Successfully started the application.")
    }
}