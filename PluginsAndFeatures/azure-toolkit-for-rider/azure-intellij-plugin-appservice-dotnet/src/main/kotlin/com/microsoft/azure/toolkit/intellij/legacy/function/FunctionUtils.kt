/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

// Latest supported version by the Azure Toolkit for Rider
const val FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION = "v4"

fun isFunctionCoreToolsExecutable(value: String?) =
    value.equals("func", ignoreCase = true) ||
            value.equals("func.cmd", ignoreCase = true) ||
            value.equals("func.exe", ignoreCase = true)