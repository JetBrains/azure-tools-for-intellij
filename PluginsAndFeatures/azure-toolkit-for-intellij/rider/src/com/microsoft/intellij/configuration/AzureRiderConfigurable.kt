/**
 * Copyright (c) 2018-2023 JetBrains s.r.o.
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

package com.microsoft.intellij.configuration

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.microsoft.intellij.configuration.ui.AzureFunctionsConfigurationPanel
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.identity.managed.AzureManagedIdentityConfigurationPanel
import org.jetbrains.plugins.azure.storage.azurite.AzuriteConfigurationPanel

class AzureRiderConfigurable(private val project: Project) :
        SearchableConfigurable.Parent.Abstract(), OptionsContainingConfigurable, Configurable.VariableProjectAppLevel {

    companion object {
        private const val AZURE_CONFIGURATION_ID = "com.intellij"
    }

    private val disposable = Disposer.newDisposable()

    private var configurables = listOf<Configurable>()

    override fun getId() = AZURE_CONFIGURATION_ID

    override fun getDisplayName() = message("common.azure")

    override fun buildConfigurables(): Array<Configurable> {
        configurables = listOf<Configurable>(
                AzureFunctionsConfigurationPanel(disposable),
                AzureManagedIdentityConfigurationPanel(disposable, project),
                AzuriteConfigurationPanel(disposable)
        )
        return configurables.toTypedArray()
    }

    override fun processListOptions() = hashSetOf<String>()
    override fun isProjectLevel() = false

    override fun disposeUIResources() {
        Disposer.dispose(disposable)
    }
}