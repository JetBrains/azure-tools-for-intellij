/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.dotnetRuntime

import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack
import com.azure.resourcemanager.appservice.models.NetFrameworkVersion
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.azure.resourcemanager.appservice.models.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem

data class DotNetRuntime(
    val operatingSystem: OperatingSystem,
    val stack: RuntimeStack?,
    val frameworkVersion: NetFrameworkVersion?,
    val functionStack: FunctionRuntimeStack?,
    val isDocker: Boolean
)

private const val FUNCTIONS_WORKER_RUNTIME = "FUNCTIONS_WORKER_RUNTIME"
private const val FUNCTIONS_EXTENSION_VERSION = "FUNCTIONS_EXTENSION_VERSION"

fun WebAppBase.getDotNetRuntime(): DotNetRuntime {
    val os = operatingSystem()
    if (os == com.azure.resourcemanager.appservice.models.OperatingSystem.LINUX) {
        val linuxFxVersion = linuxFxVersion()
        if (linuxFxVersion.startsWith("docker", true)) {
            return DotNetRuntime(
                OperatingSystem.LINUX,
                null,
                null,
                null,
                true
            )
        } else {
            if (appSettings.containsKey(FUNCTIONS_WORKER_RUNTIME)) {
                val runtime = requireNotNull(appSettings[FUNCTIONS_WORKER_RUNTIME]).value()
                val version = requireNotNull(appSettings[FUNCTIONS_EXTENSION_VERSION]).value()
                return DotNetRuntime(
                    OperatingSystem.LINUX,
                    null,
                    null,
                    FunctionRuntimeStack(runtime, version, linuxFxVersion()),
                    false
                )
            } else {
                val stack = linuxFxVersion.substringBefore('|', "DOTNETCORE")
                val version = linuxFxVersion.substringAfter('|', "8.0")
                return DotNetRuntime(
                    OperatingSystem.LINUX,
                    RuntimeStack(stack, version),
                    null,
                    null,
                    false
                )
            }
        }
    } else {
        if (appSettings.containsKey(FUNCTIONS_WORKER_RUNTIME)) {
            val runtime = requireNotNull(appSettings[FUNCTIONS_WORKER_RUNTIME]).value()
            val version = requireNotNull(appSettings[FUNCTIONS_EXTENSION_VERSION]).value()
            return DotNetRuntime(
                OperatingSystem.WINDOWS,
                null,
                null,
                FunctionRuntimeStack(runtime, version, linuxFxVersion()),
                false
            )
        } else {
            return DotNetRuntime(
                OperatingSystem.WINDOWS,
                null,
                netFrameworkVersion(),
                null,
                false
            )
        }
    }
}
