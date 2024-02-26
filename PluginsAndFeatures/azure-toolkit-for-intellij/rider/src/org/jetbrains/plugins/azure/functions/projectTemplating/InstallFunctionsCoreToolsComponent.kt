/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.projectTemplating

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.ui.components.ComponentFactories
import com.jetbrains.rider.ui.components.base.Viewable
import com.microsoft.intellij.AzureConfigurable.AZURE_CONFIGURABLE_PREFIX
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsConstants
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import javax.swing.JComponent
import javax.swing.JPanel

class InstallFunctionsCoreToolsComponent(reloadTemplates: Runnable) : Viewable<JComponent> {

    val pane = JPanel(MigLayout("fill, ins 0, flowy, gap ${JBUI.scale(2)}", "[push]", "[min!][grow]"))

    override fun getView(): JComponent {
        return panel {
            row { cell(pane).align(Align.FILL).resizableColumn() }.resizableRow()
        }.apply { border = IdeBorderFactory.createEmptyBorder(JBInsets(10, 20, 10, 20)) }
    }

    init {
        val topPane = JPanel(MigLayout("ins 0, gap ${JBUI.scale(1)}, flowy, fill, left", "[push]")).apply {
            add(ComponentFactories.titleLabel(message("template.project.function_app.install.title")))
            add(ComponentFactories.multiLineLabelPane(message("template.project.function_app.install.core_tools_install_and_configure_request")),
                    "growx, gapbottom ${JBUI.scale(1)}")

            if (Registry.`is`("azure.function_app.core_tools.feed.enabled")) {
                add(ComponentFactories.hyperlinkLabel(message("template.project.function_app.install.core_tools_download_request")) {
                    val project = ProjectManager.getInstance().defaultProject
                    FunctionsCoreToolsInfoProvider.retrieveForVersion(
                            project, FunctionsCoreToolsConstants.FUNCTIONS_CORETOOLS_LATEST_SUPPORTED_VERSION, allowDownload = true)

                    reloadTemplates.run()
                })
            }

            add(ComponentFactories.hyperlinkLabel(message("template.project.function_app.install.core_tools_configure_request")) {
                val project = ProjectManager.getInstance().defaultProject
                // TODO: FIX_LOCALIZATION: Using displayName parameter here for Settings ID need to be fixed to use ID to avoid localization issues.
                ShowSettingsUtilImpl.showSettingsDialog(project, AZURE_CONFIGURABLE_PREFIX + message("settings.app_services.function_app.name"), "")

                reloadTemplates.run()
            })
        }

        pane.add(topPane, "growx")
    }
}