/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings

import com.intellij.openapi.components.*
import com.microsoft.azure.toolkit.intellij.legacy.function.isFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver

@State(
    name = "com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings",
    storages = [(Storage("AzureSettings.xml"))]
)
@Service
class AzureFunctionSettings : SimplePersistentStateComponent<AzureFunctionSettingState>(AzureFunctionSettingState()) {
    companion object {
        fun getInstance() = service<AzureFunctionSettings>()

        const val AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ"
        const val AZURE_FUNCTIONS_TOOLS_FOLDER = "AzureFunctionsCoreTools"
    }

    var azureCoreToolsPathEntries: List<AzureCoreToolsPathEntry>
        get() {
            return buildList {
                add(AzureCoreToolsPathEntry("v4", resolveFromEnvironment(state.functionV4Path)))
                add(AzureCoreToolsPathEntry("v3", resolveFromEnvironment(state.functionV3Path)))
                add(AzureCoreToolsPathEntry("v2", resolveFromEnvironment(state.functionV2Path)))
            }
        }
        set(entries) {
            for (entry in entries) {
                when (entry.functionsVersion) {
                    "v4" -> state.functionV4Path = resolveFromEnvironment(entry.coreToolsPath)
                    "v3" -> state.functionV3Path = resolveFromEnvironment(entry.coreToolsPath)
                    "v2" -> state.functionV2Path = resolveFromEnvironment(entry.coreToolsPath)
                    else -> continue
                }
            }
        }

    var functionDownloadPath
        get() = state.functionDownloadPath ?: ""
        set(value) {
            state.functionDownloadPath = value
        }

    var checkForFunctionMissingPackages
        get() = state.checkForFunctionMissingPackages
        set(value) {
            state.checkForFunctionMissingPackages = value
        }

    private fun resolveFromEnvironment(coreToolsPathValue: String?): String {
        if (isFunctionCoreToolsExecutable(coreToolsPathValue)) {
            return FunctionCliResolver.resolveFunc() ?: ""
        }

        return coreToolsPathValue ?: ""
    }
}

data class AzureCoreToolsPathEntry(var functionsVersion: String, var coreToolsPath: String)