package org.jetbrains.plugins.azure.util

fun isFunctionCoreToolsExecutable(value: String?) =
    value.equals("func", ignoreCase = true) ||
            value.equals("func.cmd", ignoreCase = true) ||
            value.equals("func.exe", ignoreCase = true)