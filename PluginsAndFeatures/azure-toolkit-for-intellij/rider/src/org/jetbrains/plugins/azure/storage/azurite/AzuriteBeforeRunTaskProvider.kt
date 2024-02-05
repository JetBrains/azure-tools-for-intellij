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

package org.jetbrains.plugins.azure.storage.azurite

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfiguration
import org.jetbrains.plugins.azure.storage.azurite.actions.StartAzuriteAction
import javax.swing.Icon

class AzuriteBeforeRunTask : BeforeRunTask<AzuriteBeforeRunTask>(AzuriteBeforeRunTaskProvider.providerId)

class AzuriteBeforeRunTaskProvider : BeforeRunTaskProvider<AzuriteBeforeRunTask>() {
    companion object {
        val providerId = Key.create<AzuriteBeforeRunTask>("RunAzuriteTask")
    }

    override fun getId(): Key<AzuriteBeforeRunTask>? = providerId

    override fun getName(): String? = RiderAzureBundle.message("run_config.azurite.before_run_tasks.run_azurite_name")


    override fun isConfigurable() = false

    override fun getIcon(): Icon = CommonIcons.Azurite

    override fun createTask(runConfiguration: RunConfiguration): AzuriteBeforeRunTask {
        val task = AzuriteBeforeRunTask()
        task.isEnabled = runConfiguration is AzureFunctionsHostConfiguration
        return task
    }

    override fun executeTask(context: DataContext,
                             configuration: RunConfiguration,
                             env: ExecutionEnvironment,
                             task: AzuriteBeforeRunTask): Boolean {

        val project = configuration.project

        ApplicationManager.getApplication().invokeLater {
            ActionUtil.invokeAction(
                    StartAzuriteAction(),
                    SimpleDataContext.getProjectContext(project),
                    ActionPlaces.INTENTION_MENU,
                    null,
                    null)
        }

        return true
    }
}