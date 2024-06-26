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

import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rider.projectView.projectTemplates.NewProjectDialogContext
import com.jetbrains.rider.projectView.projectTemplates.ProjectTemplatesSharedModel
import com.jetbrains.rider.projectView.projectTemplates.generators.ProjectTemplateGenerator
import com.jetbrains.rider.projectView.projectTemplates.providers.ProjectTemplateProvider
import com.jetbrains.rider.projectView.projectTemplates.templateTypes.ProjectTemplateType
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JComponent

class FunctionsCoreToolsTemplatesProvider : ProjectTemplateProvider {

    override val isReady = Property(true)

    override fun load(lifetime: Lifetime, context: NewProjectDialogContext): IProperty<Set<ProjectTemplateType>?> {
        isReady.set(false)
        val result = Property<Set<ProjectTemplateType>?>(setOf())

        if (!FunctionsCoreToolsTemplateManager.areRegistered()) {
            FunctionsCoreToolsTemplateManager.tryReload()

            if (!FunctionsCoreToolsTemplateManager.areRegistered()) {
                result.set(setOf(InstallTemplates()))
            }
        }

        isReady.set(true)
        return result
    }

    private class InstallTemplates : ProjectTemplateType {
        override val group = message("template.project.function_app.install.group")
        override val icon = CommonIcons.AzureFunctions.TemplateAzureFunc
        override val name = message("template.project.function_app.install.name")
        override val order = 90

        override fun getKeywords() = setOf(name)

        override fun createGenerator(lifetime: Lifetime, context: NewProjectDialogContext, sharedModel: ProjectTemplatesSharedModel): ProjectTemplateGenerator {
            return object : ProjectTemplateGenerator {
                override val canExpand = AtomicBooleanProperty(false)
                override suspend fun expandTemplate(): suspend () -> Unit { throw Error("Expand template should not be called") }
                override fun getFocusComponentId(): String? = null
                override fun requestFocusComponent(focusComponentId: String?) {  }

                override fun getComponent(): JComponent {
                    return InstallFunctionsCoreToolsComponent {
                        FunctionsCoreToolsTemplateManager.tryReload()
                        context.reloadTemplates.fire()
                    }.getView()
                }
            }
        }
    }
}