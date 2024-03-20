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

package org.jetbrains.plugins.azure.functions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.util.function.Function

class AzureCoreToolsMissingNupkgNotificationProvider : EditorNotificationProvider {
    companion object {
        private fun hasKnownFileSuffix(file: VirtualFile): Boolean =
            file.extension.equals("cs", true) ||
                    file.extension.equals("vb", true) ||
                    file.extension.equals("fs", true)
    }

    override fun collectNotificationData(project: Project, file: VirtualFile): Function<FileEditor, EditorNotificationPanel?>? {
        if (PropertiesComponent.getInstance(project).getBoolean(AzureRiderSettings.DISMISS_NOTIFICATION_AZURE_FUNCTIONS_MISSING_NUPKG)) return null

        if (!hasKnownFileSuffix(file)) return null

        val service = FunctionMissingNugetPackageService.getInstance(project)
        val missingPackages = service.getMissingPackages(file)
        if (missingPackages == null) {
            service.checkForMissingPackages(file)
            return null
        }

        for (missingPackage in missingPackages) {
            return Function { _: FileEditor ->
                createNotificationPanel(file, missingPackage, project)
            }
        }

        return null
    }

    private fun createNotificationPanel(
        file: VirtualFile,
        dependency: FunctionMissingNugetPackageService.InstallableDependency,
        project: Project
    ): EditorNotificationPanel {
        val panel = EditorNotificationPanel()
            .text(RiderAzureBundle.message("notification.function_app.missing_nupkg.title", dependency.dependency.id))

        panel.createActionLabel(RiderAzureBundle.message("notification.function_app.missing_nupkg.action.install"), {
            val service = FunctionMissingNugetPackageService.getInstance(project)
            service.installPackage(file, dependency)
        }, true)

        panel.createActionLabel(RiderAzureBundle.message("notification.function_app.missing_nupkg.action.dismiss"), {
            dismissNotification(file, project)
        }, true)

        return panel
    }

    private fun dismissNotification(file: VirtualFile, project: Project) {
        PropertiesComponent.getInstance(project).setValue(AzureRiderSettings.DISMISS_NOTIFICATION_AZURE_FUNCTIONS_MISSING_NUPKG, true)
        EditorNotifications.getInstance(project).updateNotifications(file)
    }
}