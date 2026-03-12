/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.system.CpuArch
import com.jetbrains.rider.CPUKind
import com.jetbrains.rider.util.OSKind

data class FunctionToolingFeedFilter(
    val os: String,
    val architectures: List<String>,
    val sizes: List<String>
)

fun getReleaseFilterForCurrentSystem(cpuKind: CPUKind? = null): FunctionToolingFeedFilter {
    val os = getOsName(cpuKind)
    val arch = getArchitectures(cpuKind)
    val sizes = getSizes(os)
    return FunctionToolingFeedFilter(os, arch, sizes)
}

private fun getOsName(cpuKind: CPUKind?): String {
    cpuKind?.osKind?.let {
        return when (it) {
            OSKind.Windows -> "Windows"
            OSKind.MacOS -> "MacOS"
            OSKind.Linux -> "Linux"
        }
    }
    return when {
        SystemInfo.isWindows -> "Windows"
        SystemInfo.isMac -> "MacOS"
        SystemInfo.isLinux -> "Linux"
        else -> "Unknown"
    }
}

private fun getArchitectures(cpuKind: CPUKind?): List<String> {
    cpuKind?.let {
        return when (it) {
            CPUKind.WinArm64,
            CPUKind.MacOsArm -> listOf("arm64", "x64")
            else -> listOf("x64")
        }
    }

    return when {
        (SystemInfo.isWindows && CpuArch.isArm64()) ||
                (SystemInfo.isMac && CpuArch.isArm64()) -> listOf("arm64", "x64")
        SystemInfo.isWindows && !CpuArch.isIntel64() -> listOf("x86")
        else -> listOf("x64")
    }
}

private fun getSizes(os: String): List<String> =
    if (os == "Windows") listOf("minified", "full") else listOf("full")