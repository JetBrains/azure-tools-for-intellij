/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy

import com.intellij.execution.ExecutionException
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.build.tasks.BuildStatus
import com.jetbrains.rider.build.tasks.BuildTaskThrottler
import com.jetbrains.rider.model.BuildResultKind
import com.jetbrains.rider.model.CustomTargetExtraProperty
import com.jetbrains.rider.model.CustomTargetWithExtraProperties
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.SilentMode
import com.jetbrains.rider.run.configurations.publishing.base.MsBuildPublishingService
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class ArtifactService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): ArtifactService = project.service()
        private val LOG = logger<ArtifactService>()
    }

    suspend fun publishProjectToFolder(
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?,
        updateStatusText: (String) -> Unit
    ): Result<File> {
        updateStatusText("Publishing project ${publishableProject.projectName} to a folder...")

        if (!configuration.isNullOrEmpty() && !platform.isNullOrEmpty()) {
            updateStatusText("Using configuration: $configuration and platform: $platform")
        }
        val artifactDirectoryResult = publishProjectToFolder(publishableProject, configuration, platform)

        return artifactDirectoryResult
    }

    /**
     * Publish project to a folder [File]
     * Note: For .NET Framework projects publish a copy of project folder as is
     *       For .NET projects generate a publishable output by [MsBuildPublishingService]
     *
     * @param publishableProject [PublishableProjectModel] contains information about the project to be published
     * @param configuration optional configuration to pass to MSBuild
     * @param platform optional platform to pass to MSBuild
     *
     * @return [File] to project content to be published
     */
    private suspend fun publishProjectToFolder(
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?
    ): Result<File> {
        val publishService = MsBuildPublishingService.getInstance(project)
        val (tempDirMsBuildProperty, outPath) = publishService.getPublishToTempDirParameterAndPath()

        LOG.trace { "Publishing project ${publishableProject.projectName} to ${outPath.pathString}" }

        val extraProperties = mutableListOf<CustomTargetExtraProperty>()
        if (!configuration.isNullOrEmpty()) {
            extraProperties.add(CustomTargetExtraProperty("Configuration", configuration))
        }
        if (!platform.isNullOrEmpty()) {
            extraProperties.add(CustomTargetExtraProperty("Platform", platform))
        }

        val buildStatus =
            if (publishableProject.isDotNetCore) {
                invokeMsBuild(
                    publishableProject, listOf(tempDirMsBuildProperty) + extraProperties,
                    diagnosticsMode = false,
                    silentMode = SilentMode.Silent,
                    noRestore = true
                )
            } else {
                webPublishToFileSystem(
                    publishableProject.projectFilePath, outPath, extraProperties,
                    diagnosticsMode = false,
                    silentMode = SilentMode.Silent
                )
            }

        val buildResult = buildStatus.buildResultKind
        if (buildResult != BuildResultKind.Successful && buildResult != BuildResultKind.HasWarnings) {
            LOG.error("Unable to publish project")
            return Result.failure(ExecutionException("Failed collecting project artifacts. Please see Build output."))
        } else {
            requestRunWindowFocus()
        }

        return Result.success(outPath.toFile().canonicalFile)
    }

    private suspend fun invokeMsBuild(
        projectModel: PublishableProjectModel,
        extraProperties: List<CustomTargetExtraProperty>,
        diagnosticsMode: Boolean,
        silentMode: SilentMode = SilentMode.Default,
        noRestore: Boolean = false
    ): BuildStatus {
        val buildParameters = BuildParameters(
            CustomTargetWithExtraProperties("Publish", extraProperties),
            listOf(projectModel.projectFilePath),
            diagnosticsMode,
            silentMode,
            noRestore = noRestore
        )

        return BuildTaskThrottler.getInstance(project).buildSequentially(buildParameters)
    }

    private suspend fun webPublishToFileSystem(
        pathToProject: String,
        outPath: Path,
        extraProperties: List<CustomTargetExtraProperty>,
        diagnosticsMode: Boolean = false,
        silentMode: SilentMode = SilentMode.Default
    ): BuildStatus {
        val buildParameters = BuildParameters(
            CustomTargetWithExtraProperties(
                "WebPublish",
                extraProperties + listOf(
                    CustomTargetExtraProperty("WebPublishMethod", "FileSystem"),
                    CustomTargetExtraProperty("PublishUrl", outPath.toString())
                )
            ), listOf(pathToProject),
            diagnosticsMode,
            silentMode
        )

        return BuildTaskThrottler.getInstance(project).buildSequentially(buildParameters)
    }

    /**
     * Move focus to the Run tool window to switch back to the running process
     */
    private fun requestRunWindowFocus() {
        try {
            ToolWindowManager.getInstance(project).invokeLater {
                val window = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)
                if (window != null && window.isAvailable)
                    window.show(null)
            }
        } catch (e: Throwable) {
            LOG.warn(e)
        }
    }
}