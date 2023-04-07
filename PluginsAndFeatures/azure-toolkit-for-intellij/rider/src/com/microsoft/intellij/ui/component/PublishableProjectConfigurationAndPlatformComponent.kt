/**
 * Copyright (c) 2023 JetBrains s.r.o.
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

@file:Suppress("UnstableApiUsage")

package com.microsoft.intellij.ui.component

import com.intellij.openapi.project.Project
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper
import com.microsoft.intellij.ui.util.findFirst
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import javax.swing.JLabel
import javax.swing.JPanel

class PublishableProjectConfigurationAndPlatformComponent(private val project: Project) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent
{

    private val cbConfigurationAndPlatform = PublishRuntimeSettingsCoreHelper.createConfigurationAndPlatformComboBox(project)

    init {
        add(JLabel(RiderAzureBundle.message("run_config.publish.form.configuration.label")))
        add(cbConfigurationAndPlatform, "growx")
    }

    fun getSelectedConfiguration() = getSelectedConfigurationAndPlatform().configuration
    fun getSelectedPlatform() = getSelectedConfigurationAndPlatform().platform

    private fun getSelectedConfigurationAndPlatform() : PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform =
            cbConfigurationAndPlatform.component.selectedItem as PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform

    fun setSelectedConfigurationAndPlatform(configuration: String, platform: String) {
        cbConfigurationAndPlatform.component.model
                .findFirst {
                    it?.configuration == configuration &&
                            it.platform == platform }
                ?.let {
                    cbConfigurationAndPlatform.component.selectedItem = it
                }
    }
}