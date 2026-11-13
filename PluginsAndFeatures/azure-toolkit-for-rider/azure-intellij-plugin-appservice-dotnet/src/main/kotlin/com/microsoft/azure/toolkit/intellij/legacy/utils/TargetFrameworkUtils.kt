/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.utils

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.legacy.isSupportedOn
import com.microsoft.azure.toolkit.intellij.legacy.resolveTargetFramework
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager

private val LOG = fileLogger()

/**
 * Validates the target framework of an App Service run configuration.
 *
 * @throws RuntimeConfigurationError if the target framework can't run on App Service with [operatingSystem]
 * @throws RuntimeConfigurationWarning if the saved target framework is not available and will be replaced
 */
@Throws(RuntimeConfigurationException::class)
internal fun checkTargetFramework(
    project: Project,
    publishableProjectPath: String?,
    targetFramework: String?,
    operatingSystem: String?
) {
    val publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
        .firstOrNull { it.projectFilePath == publishableProjectPath }
        ?: return

    val resolved = publishableProject.resolveTargetFramework(targetFramework)
    if (!publishableProject.isSupportedOn(project, resolved, OperatingSystem.fromString(operatingSystem))) {
        throw RuntimeConfigurationError(netFrameworkOnLinuxMessage(resolved))
    }

    if (publishableProject.isDotNetCore && !targetFramework.isNullOrEmpty() && targetFramework != resolved) {
        throw RuntimeConfigurationWarning(targetFrameworkFallbackMessage(targetFramework, resolved))
    }
}

/**
 * Resolves the target framework to deploy the project with.
 * If [saved] is not available, falls back to the lowest available target framework and reports it to [messenger].
 *
 * @throws ExecutionException if the target framework can't run on App Service with [operatingSystem]
 */
internal fun PublishableProjectModel.resolveDeploymentTargetFramework(
    project: Project,
    saved: String?,
    operatingSystem: OperatingSystem?,
    messenger: IAzureMessager?
): String? {
    val resolved = resolveTargetFramework(saved)
    if (isDotNetCore && !saved.isNullOrEmpty() && saved != resolved) {
        val message = targetFrameworkFallbackMessage(saved, resolved)
        LOG.warn("$message (project '$projectName')")
        messenger?.warning(message)
    }

    if (!isSupportedOn(project, resolved, operatingSystem)) {
        throw ExecutionException(netFrameworkOnLinuxMessage(resolved))
    }

    return resolved
}

private fun targetFrameworkFallbackMessage(saved: String, resolved: String?) =
    if (resolved != null)
        "Target framework '$saved' is not available for the project, '$resolved' will be used"
    else
        "Target framework '$saved' is not available for the project, it will be published without a target framework"

private fun netFrameworkOnLinuxMessage(targetFramework: String?) =
    if (!targetFramework.isNullOrEmpty())
        "Target framework '$targetFramework' is .NET Framework, which is not supported on Linux. Choose a Windows app or another target framework"
    else
        ".NET Framework projects are not supported on Linux. Choose a Windows app"
