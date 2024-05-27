/**
 * Copyright (c) 2019-2023 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.rider.debugger.IRiderDebuggable
import com.jetbrains.rider.run.configurations.IAutoSelectableRunConfiguration
import com.jetbrains.rider.run.configurations.RiderAsyncRunConfiguration
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import org.jdom.Element

class AzureFunctionsHostConfiguration(
        name: String,
        project: Project,
        factory: ConfigurationFactory,
        val parameters: AzureFunctionsHostConfigurationParameters
) : RiderAsyncRunConfiguration(
        name,
        project,
        factory,
        { AzureFunctionsHostSettingsEditorGroup(it) },
        AzureFunctionsHostExecutorFactory(parameters)
), IRiderDebuggable, IAutoSelectableRunConfiguration {

    private val riderDotNetActiveRuntimeHost = RiderDotNetActiveRuntimeHost.getInstance(project)

    override fun checkConfiguration() {
        super.checkConfiguration()
        parameters.validate(riderDotNetActiveRuntimeHost)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        parameters.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        parameters.writeExternal(element)
    }

    override fun getAutoSelectPriority() = 10
}
