/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy

import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack
import com.azure.resourcemanager.appservice.models.NetFrameworkVersion
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.NaturalComparator
import com.jetbrains.rider.azureFunctions.common.AzureFunctionsWorkerRuntime
import com.jetbrains.rider.azureFunctions.localSettings.AzureFunctionsLocalSettingsService
import com.jetbrains.rider.azureFunctions.localSettings.getWorkerRuntime
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.RdTargetFrameworkId
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.environment.MSBuildEvaluator
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val LOG = logger<PublishableProjectModel>()

private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9]+(?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
private val netAppVersionRegex = Regex("net([0-9]+(?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
private val netFxAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

/**
 * Returns the target frameworks in MSBuild format (e.g. `net8.0`) that can be passed to `dotnet publish`.
 * The list is sorted with [NaturalComparator], so the lowest target framework goes first.
 * For .NET Framework projects the list is empty because `TargetFramework` is not passed to `WebPublish`.
 */
fun PublishableProjectModel.availableTargetFrameworks(): List<String> {
    if (!isDotNetCore) return emptyList()

    return projectOutputs
        .map { it.tfmInMsbuildFormat }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedWith(NaturalComparator.INSTANCE)
}

/**
 * Returns [saved] if it is one of the [availableTargetFrameworks], otherwise the lowest available target framework.
 */
fun PublishableProjectModel.resolveTargetFramework(saved: String?): String? {
    val available = availableTargetFrameworks()
    if (!saved.isNullOrEmpty() && saved in available) return saved

    val fallback = available.firstOrNull()
    if (!saved.isNullOrEmpty() && isDotNetCore) {
        LOG.warn("Target framework '$saved' is not available for the project '$projectName', using '$fallback'")
    }

    return fallback
}

/**
 * Returns the framework id (e.g. `.NETCoreApp,Version=v8.0`) of the target framework [tfm] in MSBuild format.
 * If [tfm] is not specified or is not found among the project outputs,
 * the current target framework of the project is used.
 */
fun PublishableProjectModel.frameworkIdFor(project: Project, tfm: String?): String? {
    if (!tfm.isNullOrEmpty()) {
        val frameworkId = projectOutputs.firstOrNull { it.tfmInMsbuildFormat == tfm }?.tfm
        if (!frameworkId.isNullOrEmpty()) return frameworkId
    }

    val frameworks = project.solution.projectModelTasks.targetFrameworks[projectModelId]
    return frameworks?.currentTargetFrameworkId?.valueOrNull?.framework?.id
}

fun PublishableProjectModel.getStackAndVersion(
    project: Project,
    operatingSystem: OperatingSystem,
    isFunction: Boolean,
    targetFramework: String? = null
): PublishableProjectRuntime? {
    if (operatingSystem == OperatingSystem.DOCKER) return null

    if (isDotNetCore) {
        val dotnetVersion = getProjectDotNetVersion(project, this, targetFramework)
        if (operatingSystem == OperatingSystem.LINUX) {
            val stackName = if (isFunction) "DOTNET" else "DOTNETCORE"
            val stack =
                if (dotnetVersion != null) RuntimeStack(stackName, dotnetVersion)
                else RuntimeStack(stackName, "10.0")

            return PublishableProjectRuntime(stack, dotnetVersion, null)
        } else {
            val version =
                if (dotnetVersion != null) NetFrameworkVersion.fromString("v$dotnetVersion")
                else NetFrameworkVersion.fromString("v10.0")

            return PublishableProjectRuntime(null, null, version)
        }
    } else {
        val netFrameworkVersion = getProjectNetFrameworkVersion(project, this, targetFramework)
        val version =
            if (netFrameworkVersion == null)
                NetFrameworkVersion.fromString("v3.5")
            else if (netFrameworkVersion.startsWith("4"))
                NetFrameworkVersion.fromString("v4.8")
            else
                NetFrameworkVersion.fromString("v3.5")

        return PublishableProjectRuntime(null, null, version)
    }
}

data class PublishableProjectRuntime(
    val runtimeStack: RuntimeStack?,
    val dotnetVersion: String?,
    val frameworkVersion: NetFrameworkVersion?
)

@Suppress("JetBrainsInternalApiUsage")
suspend fun PublishableProjectModel.getFunctionStack(
    project: Project,
    operatingSystem: OperatingSystem,
    targetFramework: String? = null
): FunctionRuntimeStack {
    val functionLocalSettings = AzureFunctionsLocalSettingsService
        .getInstance(project)
        .getLocalSettingsFor(this)
    val workerRuntime = functionLocalSettings?.getWorkerRuntime() ?: AzureFunctionsWorkerRuntime.DOTNET_ISOLATED
    val path = Path.of(this@getFunctionStack.projectFilePath)
    val azureFunctionVersion = getAzureFunctionsVersionProjectProperty(path, null, project)
        ?.trimStart('v', 'V')
        ?: "4"
    val dotnetVersion = getProjectDotNetVersion(project, this, targetFramework)
    return FunctionRuntimeStack(
        workerRuntime.value,
        functionRuntimeVersionFromProjectProperty(azureFunctionVersion),
        if (operatingSystem == OperatingSystem.LINUX) "${workerRuntime.value}|$dotnetVersion" else ""
    )
}

private suspend fun getAzureFunctionsVersionProjectProperty(
    projectFilePath: Path,
    projectTfm: RdTargetFrameworkId?,
    project: Project
): String? {
    val evaluator = MSBuildEvaluator.getInstance(project)
    val request = MSBuildEvaluator.PropertyRequest(
        projectFilePath.absolutePathString(),
        projectTfm,
        listOf("AzureFunctionsVersion")
    )
    val result = evaluator.evaluatePropertiesSuspending(request)
    return result["AzureFunctionsVersion"]
}

private fun functionRuntimeVersionFromProjectProperty(azureFunctionVersion: String) = when (azureFunctionVersion) {
    "4" -> "~4"
    "3" -> "~3"
    "2" -> "~2"
    "1" -> "~1"
    else -> "~4"
}

private fun getProjectDotNetVersion(
    project: Project,
    publishableProject: PublishableProjectModel,
    targetFramework: String?
): String? {
    val currentFramework = publishableProject.frameworkIdFor(project, targetFramework) ?: return null
    return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value
        ?: netCoreAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value
}

private fun getProjectNetFrameworkVersion(
    project: Project,
    publishableProject: PublishableProjectModel,
    targetFramework: String?
): String? {
    val currentFramework = publishableProject.frameworkIdFor(project, targetFramework) ?: return null
    return netFxAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value
}
