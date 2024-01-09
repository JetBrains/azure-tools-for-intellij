/**
 * Copyright (c) 2020-2022 JetBrains s.r.o.
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

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.storage.azurite.actions.CleanAzuriteAction
import org.jetbrains.plugins.azure.storage.azurite.actions.ShowAzuriteSettingsAction
import org.jetbrains.plugins.azure.storage.azurite.actions.StartAzuriteAction
import org.jetbrains.plugins.azure.storage.azurite.actions.StopAzuriteAction

class AzuriteSessionDescriptor(project: Project)
    : SimpleServiceViewDescriptor(RiderAzureBundle.message("service.azurite.name"), CommonIcons.Azurite), Disposable, AzuriteSessionListener {

    companion object {
        val defaultToolbarActions = DefaultActionGroup(
                StartAzuriteAction(),
                StopAzuriteAction(),
                CleanAzuriteAction(),
                ShowAzuriteSettingsAction()
        )
    }

    private val myLock = Any()
    private var processHandler: ColoredProcessHandler? = null
    private var workspace: String? = null

    private val consoleView: ConsoleView = TextConsoleBuilderFactory
            .getInstance()
            .createBuilder(project)
            .apply { setViewer(true) }
            .console

    init {
        val service = AzuriteService.getInstance()
        val activeProcessHandler = service.processHandler
        val activeWorkSpace = service.workspace
        if (activeProcessHandler != null && activeWorkSpace != null) {
            connectToSession(activeProcessHandler, activeWorkSpace)
        }

        application.messageBus
                .connect(this)
                .subscribe(AzuriteSessionListener.TOPIC, this)

        Disposer.register(project, this)
        Disposer.register(this, consoleView)
    }

    override fun getToolbarActions() = defaultToolbarActions

    @Suppress("DialogTitleCapitalization")
    override fun getPresentation() = PresentationData().apply {
        locationString = workspace
        setIcon(CommonIcons.Azurite)
        addText(RiderAzureBundle.message("service.azurite.name"), SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun getContentComponent() = BorderLayoutPanel().apply {
        border = JBUI.Borders.empty()
        add(consoleView.component)
    }

    private fun connectToSession(activeProcessHandler: ColoredProcessHandler, activeWorkspace: String) {
        if (processHandler == activeProcessHandler) return

        synchronized(myLock) {
            processHandler?.detachProcess()
            processHandler = activeProcessHandler
            workspace = activeWorkspace
        }

        consoleView.print(RiderAzureBundle.message("action.azurite.reattach.workspace", activeWorkspace) + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        consoleView.attachToProcess(activeProcessHandler)
        consoleView.print(RiderAzureBundle.message("action.azurite.reattach.finished") + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
    }

    private fun disconnectFromSession() {
        if (processHandler == null) return

        synchronized(myLock) {
            processHandler?.detachProcess()
            processHandler = null
            workspace = null
        }
    }

    override fun dispose() {
        Disposer.dispose(consoleView)
    }

    override fun sessionStarted(processHandler: ColoredProcessHandler, workspace: String) {
        connectToSession(processHandler, workspace)
    }

    override fun sessionStopped() {
        disconnectFromSession()
    }
}