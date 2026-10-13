/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.util.NlsSafe
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import org.jetbrains.annotations.Nls

internal data class BicepLS(val version: String) {
    companion object {
        private const val DEFAULT_VERSION = "v0.46.1"
        private const val URL_TEMPLATE = "https://github.com/Azure/bicep/releases/download/%s/bicep-langserver.zip"

        fun getDefaultBicepLS(): BicepLS = BicepLS(DEFAULT_VERSION)
    }

    val formattedUrl: @NlsSafe String
        get() = URL_TEMPLATE.format(version)

    val extractedDirectoryName: @NlsSafe String
        get() = "bicep-langserver"

    val executableName: @NlsSafe String
        get() = "Bicep.LangServer.dll"

    val presentableName: @Nls String
        get() = BicepBundle.message("progress.title.load.ls")
}
