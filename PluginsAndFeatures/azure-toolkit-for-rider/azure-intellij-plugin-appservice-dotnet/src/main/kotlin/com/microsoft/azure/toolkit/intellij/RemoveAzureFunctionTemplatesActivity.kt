/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rider.projectView.projectTemplates.providers.RiderProjectTemplateProvider
import com.microsoft.azure.toolkit.intellij.legacy.function.FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsManager
import kotlin.io.path.Path

internal class RemoveAzureFunctionTemplatesActivity : ProjectActivity {
    companion object {
        private val LOG = logger<RemoveAzureFunctionTemplatesActivity>()
        private const val AZURE_FUNCTIONS_TEMPLATES_MIGRATED = "Rider.Azure.Toolkit.Templates.Migrated"
    }

    private val netIsolatedPath = Path("net-isolated")

    override suspend fun execute(project: Project) {
        val properties = PropertiesComponent.getInstance()

        if (properties.getBoolean(AZURE_FUNCTIONS_TEMPLATES_MIGRATED)) return
        properties.setValue(AZURE_FUNCTIONS_TEMPLATES_MIGRATED, true)

        val templateSources = RiderProjectTemplateProvider
            .getUserTemplateSources()
            .map { it.toPath() }

        val functionCoreToolsFolder = FunctionCoreToolsManager
            .getInstance()
            .getFunctionCoreToolsPathForVersion(FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION)
            ?: return

        templateSources.forEach {
            if (it.startsWith(functionCoreToolsFolder)) {
                LOG.trace { "Removing Azure Functions template $it" }
                RiderProjectTemplateProvider.removeUserTemplateSource(it.toFile())
            } else if (it.contains(netIsolatedPath)) {
                val index = it.lastIndexOf(netIsolatedPath)
                val prefix = it.root.resolve(it.subpath(0, index))
                val sourcesToRemove = templateSources.filter { ts -> ts.startsWith(prefix) }

                sourcesToRemove.forEach { str ->
                    LOG.trace { "Removing Azure Functions template $it" }
                    RiderProjectTemplateProvider.removeUserTemplateSource(str.toFile())
                }
            }
        }
    }
}