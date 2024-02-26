/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.RdProjectTemplate2
import com.jetbrains.rider.projectView.projectTemplates.NewProjectDialogContext
import com.jetbrains.rider.projectView.projectTemplates.ProjectTemplatesSharedModel
import com.jetbrains.rider.projectView.projectTemplates.generators.TypeBasedProjectTemplateGenerator
import com.jetbrains.rider.projectView.projectTemplates.providers.ProjectTemplateCustomizer
import com.jetbrains.rider.projectView.projectTemplates.templateTypes.PredefinedProjectTemplateType
import com.jetbrains.rider.projectView.projectTemplates.utils.hasClassification
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle

class FunctionsCoreToolsTemplateCustomizer : ProjectTemplateCustomizer {
    override fun getCustomProjectTemplateTypes() = setOf(AzureProjectTemplateType())
}

class AzureProjectTemplateType : PredefinedProjectTemplateType() {
    override val group = RiderAzureBundle.message("template.project.function_app.install.group")
    override val icon = CommonIcons.AzureFunctions.TemplateAzureFunc
    override val name = RiderAzureBundle.message("template.project.function_app.name")
    override val order = 90
    override val shouldHide: Boolean
        get() = !FunctionsCoreToolsTemplateManager.areRegistered()

    override fun acceptableForTemplate(projectTemplate: RdProjectTemplate2): Boolean {
        return projectTemplate.hasClassification("Azure Functions")
    }

    override fun createGenerator(lifetime: Lifetime, context: NewProjectDialogContext, sharedModel: ProjectTemplatesSharedModel) =
        object : TypeBasedProjectTemplateGenerator(lifetime, context, sharedModel, projectTemplates, hideSdk = true) {
        override val defaultName = "FunctionApp1"
        override fun customizeTypeRowLabel() = RiderAzureBundle.message("template.project.function_app.type.row.label")
        private val isolatedWorker = RiderAzureBundle.message("template.project.function_app.isolated.worker")

        override fun getType(template: RdProjectTemplate2): String {
            // Isolated worker known template identities:
            if (template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.3.x" ||
                template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.3.x" ||
                template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.4.x" ||
                template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.4.x"
            ) {
                return isolatedWorker
            }

            // Default worker known template identities:
            if (template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.3.x" ||
                template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.3.x" ||
                template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.4.x" ||
                template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.4.x"
            ) {
                return RiderAzureBundle.message("template.project.function_app.default.worker")
            }

            return template.id
        }

        override fun typeComparator(): Comparator<String> = compareBy ({ !it.contains(isolatedWorker, true) }, { it })
    }
}
