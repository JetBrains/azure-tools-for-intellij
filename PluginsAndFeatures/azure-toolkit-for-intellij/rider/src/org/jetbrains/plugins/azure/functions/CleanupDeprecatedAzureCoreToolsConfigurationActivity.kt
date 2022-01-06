/**
 * Copyright (c) 2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.io.File

@Deprecated("To be removed with 2022.3")
class CleanupDeprecatedAzureCoreToolsConfigurationActivity : StartupActivity {

    override fun runActivity(project: Project) {

        @Suppress("DEPRECATION")
        val logger = Logger.getInstance(CleanupDeprecatedAzureCoreToolsConfigurationActivity::class.java)

        // Remove old Azure Core Tools directory
        val coreToolsDirectory = File(PathManager.getConfigPath()).resolve("azure-functions-coretools")
        if (coreToolsDirectory.exists()) {
            try {
                coreToolsDirectory.deleteRecursively()
                logger.info("Finished cleanup of old Azure Core Tools directory: ${coreToolsDirectory.path}")
            } catch (e: Exception) {
                logger.warn("Error during cleanup of old Azure Core Tools directory: ${coreToolsDirectory.path}", e)
            }
        }

        // Remove old settings
        val properties = PropertiesComponent.getInstance()
        listOf("AzureFunctionsCoreToolsPath", "AzureFunctionsCoreToolsAllowPrerelease", "AzureFunctionCoreToolsCheckUpdates")
                .forEach {
                    if (properties.getValue(it) != null) {
                        properties.unsetValue(it)
                        logger.info("Finished cleanup of old Azure Core Tools setting: $it")
                    }
                }
    }
}