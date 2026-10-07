/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.CPUKind
import com.jetbrains.rider.util.OSKind
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

fun Path.isFunctionCoreTools() = nameWithoutExtension.equals("func", ignoreCase = true)

fun Path.resolveFunctionCoreToolsExecutable(cpuKind: CPUKind? = null): Path =
    if (cpuKind?.osKind == OSKind.Windows || (cpuKind == null && SystemInfo.isWindows)) resolve("func.exe")
    else resolve("func")