/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.util.NlsSafe
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import org.jetbrains.annotations.Nls

internal data object BicepLS {
    private const val URL_TEMPLATE = "https://github.com/Azure/bicep/releases/download/%s/bicep-langserver.zip"

    val formattedUrl: @NlsSafe String
        get() {
            val supportedVersion = "v0.46.1"
            return URL_TEMPLATE.format(supportedVersion)
        }

    val extractedDirectoryName: @NlsSafe String
        get() = "bicep-langserver"

    val executableName: @NlsSafe String
        get() = "Bicep.LangServer.dll"

    val presentableName: @Nls String
        get() = BicepBundle.message("progress.title.load.ls")
}
