/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.storage.azurite

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.ide.util.PropertiesComponent
import com.intellij.javascript.nodejs.util.NodePackageDescriptor
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.microsoft.intellij.AzureConfigurable
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.storage.azurite.actions.StartAzuriteAction

object Azurite {
    const val PackageName = "azurite"
    val PackageDescriptor = NodePackageDescriptor(PackageName)

    const val ManagedPathSuffix = "azurite"
    const val ProjectPathSuffix = ".idea/azurite"

    fun showSettings(project: Project?) {
        // TODO: FIX_LOCALIZATION: Using displayName parameter here for Settings ID need to be fixed to use ID to avoid localization issues.
        ShowSettingsUtilImpl.showSettingsDialog(project, AzureConfigurable.AZURE_CONFIGURABLE_PREFIX + RiderAzureBundle.message("settings.azurite.name"), "")
    }

    fun autoStartAzurite(project: Project) {
        val properties = PropertiesComponent.getInstance()
        if (properties.getBoolean(AzureRiderSettings.PROPERTY_FUNCTIONS_AZURITE_AUTOSTART, true)) {
            ActionUtil.invokeAction(
                    StartAzuriteAction(),
                    SimpleDataContext.getProjectContext(project),
                    ActionPlaces.INTENTION_MENU,
                    null,
                    null)
        }
    }
}