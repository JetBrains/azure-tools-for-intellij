/**
 * Copyright (c) 2020-2024 JetBrains s.r.o.
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

import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.storage.azurite.actions.CleanAzuriteAction
import org.jetbrains.plugins.azure.storage.azurite.actions.ShowAzuriteSettingsAction
import org.jetbrains.plugins.azure.storage.azurite.actions.StartAzuriteAction
import org.jetbrains.plugins.azure.storage.azurite.actions.StopAzuriteAction

class AzuriteNotStartedSessionDescriptor : SimpleServiceViewDescriptor(RiderAzureBundle.message("service.azurite.name"), CommonIcons.Azurite) {
    companion object {
        private val defaultToolbarActions = DefaultActionGroup(
                StartAzuriteAction(),
                StopAzuriteAction(),
                CleanAzuriteAction(),
                ShowAzuriteSettingsAction()
        )
    }

    @Suppress("DialogTitleCapitalization")
    override fun getPresentation() = PresentationData().apply {
        setIcon(CommonIcons.Azurite)
        addText(RiderAzureBundle.message("service.azurite.name"), SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun getContentComponent() = JBPanelWithEmptyText().withEmptyText(RiderAzureBundle.message("service.azurite.not_started"))

    override fun getToolbarActions() = defaultToolbarActions
}