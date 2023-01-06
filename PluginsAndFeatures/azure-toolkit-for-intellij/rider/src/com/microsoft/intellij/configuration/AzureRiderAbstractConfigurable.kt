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
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.microsoft.intellij.AzureConfigurable.AZURE_CONFIGURABLE_PREFIX
import com.microsoft.intellij.configuration.ui.AzureRiderAbstractConfigurablePanel
import com.microsoft.intellij.ui.messages.AzureBundle
import org.jetbrains.annotations.Nls

class AzureRiderAbstractConfigurable(private val panel: AzureRiderAbstractConfigurablePanel, private val isProjectLevelConfigurable: Boolean = true) :
        SearchableConfigurable, OptionsContainingConfigurable, Configurable.VariableProjectAppLevel {

    @Nls
    override fun getDisplayName()= panel.displayName

    override fun getHelpTopic(): String? = null

    override fun processListOptions(): Set<String> = emptySet()

    override fun createComponent() = panel.panel

    override fun apply() = panel.doOKAction()

    override fun isModified() = panel.isModified()

    override fun getId() = AZURE_CONFIGURABLE_PREFIX + displayName

    override fun enableSearch(option: String?) = null

    override fun isProjectLevel() = isProjectLevelConfigurable
}
