/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.components.BaseState
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings.Companion.AZURE_FUNCTIONS_TOOLS_FOLDER
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings.Companion.AZURE_TOOLS_FOLDER
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class AzureFunctionSettingState : BaseState() {
    var functionV2Path by string(null)
    var functionV3Path by string(null)
    var functionV4Path by string(null)
    var functionDownloadPath by string(
        Path.of(System.getProperty("user.home"))
            .resolve(AZURE_TOOLS_FOLDER)
            .resolve(AZURE_FUNCTIONS_TOOLS_FOLDER)
            .absolutePathString()
    )
    var checkForFunctionMissingPackages by property(true)
}