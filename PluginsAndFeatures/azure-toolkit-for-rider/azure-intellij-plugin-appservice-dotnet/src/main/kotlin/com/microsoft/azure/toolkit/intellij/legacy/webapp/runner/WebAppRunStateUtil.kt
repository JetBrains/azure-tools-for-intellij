package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.build.tasks.BuildStatus
import com.jetbrains.rider.build.tasks.BuildTaskThrottler
import com.jetbrains.rider.model.BuildResultKind
import com.jetbrains.rider.model.CustomTargetExtraProperty
import com.jetbrains.rider.model.CustomTargetWithExtraProperties
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.run.configurations.publishing.base.MsBuildPublishingService
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipOutputStream


private val LOG = Logger.getInstance("#com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppRunStateUtil")

fun prepareArtifact(
        project: Project,
        publishableProject: PublishableProjectModel,
        configuration: String,
        platform: String,
        processHandler: RunProcessHandler
): File {
    processHandler.setText("Collecting ${publishableProject.projectName} project artifacts...")
    if (configuration.isNotEmpty() && platform.isNotEmpty()) {
        processHandler.setText("Using configuration: $configuration and platform: $platform")
    }
    val outDir = collectProjectArtifacts(project, publishableProject, configuration, platform)

    processHandler.setText("Creating ${publishableProject.projectName} project ZIP...")
    return zipProjectArtifacts(outDir, processHandler)
}

/**
 * Collect a selected project artifacts and get a [File] with out folder
 * Note: For a DotNET Framework projects publish a copy of project folder as is
 *       For a DotNet Core projects generate an publishable output by [MsBuildPublishingService]
 *
 * @param project IDEA [Project] instance
 * @param publishableProject contains information about project to be published (isDotNetCore, path to project file)
 * @param configuration optional configuration to pass to MSBuild
 * @param platform optional platform to pass to MSBuild
 *
 * @return [File] to project content to be published
 */
private fun collectProjectArtifacts(
        project: Project,
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?
): File {
    val publishService = MsBuildPublishingService.getInstance(project)
    val (tempDirMsBuildProperty, outPath) = publishService.getPublishToTempDirParameterAndPath()

    val extraProperties = mutableListOf<CustomTargetExtraProperty>()
    if (!configuration.isNullOrEmpty()) extraProperties.add(CustomTargetExtraProperty("Configuration", configuration))
    if (!platform.isNullOrEmpty()) extraProperties.add(CustomTargetExtraProperty("Platform", platform))

    val buildStatus =
            if (publishableProject.isDotNetCore) {
                invokeMsBuild(project, publishableProject, listOf(tempDirMsBuildProperty) + extraProperties, false, true, true)
            } else {
                webPublishToFileSystem(project, publishableProject.projectFilePath, outPath, extraProperties, false, true)
            }

    val buildResult = buildStatus.buildResultKind
    if (buildResult != BuildResultKind.Successful && buildResult != BuildResultKind.HasWarnings) {
        val errorMessage = "Failed collecting project artifacts. Please see Build output"
        LOG.error(errorMessage)
        throw RuntimeException(errorMessage)
    } else {
        requestRunWindowFocus(project)
    }

    return outPath.toFile().canonicalFile
}

private fun invokeMsBuild(
        project: Project,
        projectModel: PublishableProjectModel,
        extraProperties: List<CustomTargetExtraProperty>,
        diagnosticsMode: Boolean,
        silentMode: Boolean = false,
        noRestore: Boolean = false
): BuildStatus {
    val buildParameters = BuildParameters(
            CustomTargetWithExtraProperties(
                    "Publish",
                    extraProperties
            ), listOf(projectModel.projectFilePath), diagnosticsMode, silentMode, noRestore = noRestore
    )

    return BuildTaskThrottler.getInstance(project).buildSequentiallySync(buildParameters)
}

private fun webPublishToFileSystem(
        project: Project,
        pathToProject: String,
        outPath: Path,
        extraProperties: List<CustomTargetExtraProperty>,
        diagnosticsMode: Boolean = false,
        silentMode: Boolean = false
): BuildStatus {
    val buildParameters = BuildParameters(
            CustomTargetWithExtraProperties(
                    "WebPublish",
                    extraProperties + listOf(
                            CustomTargetExtraProperty("WebPublishMethod", "FileSystem"),
                            CustomTargetExtraProperty("PublishUrl", outPath.toString()))
            ), listOf(pathToProject), diagnosticsMode, silentMode
    )

    return BuildTaskThrottler.getInstance(project).buildSequentiallySync(buildParameters)
}

/**
 * Move focus to Run tool window to switch back to the running process
 */
private fun requestRunWindowFocus(project: Project) {
    try {
        ToolWindowManager.getInstance(project).invokeLater {
            val window = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)
            if (window != null && window.isAvailable)
                window.show(null)
        }
    } catch (e: Throwable) {
        LOG.error(e)
    }
}

private fun zipProjectArtifacts(
        fromFile: File,
        processHandler: RunProcessHandler,
        deleteOriginal: Boolean = true
): File {
    if (!fromFile.exists())
        throw FileNotFoundException("Original file '${fromFile.path}' not found")

    try {
        val toZip = FileUtil.createTempFile(fromFile.nameWithoutExtension, ".zip", true)
        packToZip(fromFile, toZip)
        processHandler.setText("Project ZIP is created: ${toZip.path}")

        if (deleteOriginal)
            FileUtil.delete(fromFile)

        return toZip
    } catch (t: Throwable) {
        val errorMessage = "Unable to create a ZIP file: $t"
        LOG.error(errorMessage)
        processHandler.setText(errorMessage)
        throw RuntimeException("Unable to create a ZIP file", t)
    }
}

private fun packToZip(
        fileToZip: File,
        zipFileToCreate: File,
        filter: FileFilter? = null
) {
    if (!fileToZip.exists()) {
        val message = "Source file or directory '${fileToZip.path}' does not exist"
        LOG.error(message)
        throw FileNotFoundException(message)
    }

    ZipOutputStream(FileOutputStream(zipFileToCreate)).use { zipOutput ->
        ZipUtil.addDirToZipRecursively(zipOutput, null, fileToZip, "", filter, null)
    }
}