/**
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.storage.azurite.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.intellij.AzureConfigurable
import org.jetbrains.plugins.azure.RiderAzureBundle

class ShowAzuriteSettingsAction
    : AnAction(
        RiderAzureBundle.message("action.azurite.show_settings.name"),
        RiderAzureBundle.message("action.azurite.show_settings.description"),
        AllIcons.Actions.EditSource) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // TODO: FIX_LOCALIZATION: Using displayName parameter here for Settings ID need to be fixed to use ID to avoid localization issues.
        ShowSettingsUtilImpl.showSettingsDialog(project, AzureConfigurable.AZURE_CONFIGURABLE_PREFIX + RiderAzureBundle.message("settings.azurite.name"), "")
    }
}