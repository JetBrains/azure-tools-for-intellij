/**
 * Copyright (c) 2020-2021 JetBrains s.r.o.
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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.services.ServiceEventListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import com.intellij.util.application
import com.intellij.util.io.BaseOutputReader
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class AzuriteService : LifetimedService() {
    companion object {
        fun getInstance() = service<AzuriteService>()
        private val logger = logger<AzuriteService>()
    }

    private val sessionLifetimes = SequentialLifetimes(serviceLifetime)

    private val sessionStarted = AtomicBoolean(false)
    var session: AzuriteSession = AzuriteNotStartedSession()
        private set

    var processHandler: ColoredProcessHandler? = null
        private set

    var workspace: String? = null
        private set

    val isRunning: Boolean
        get() {
            return sessionStarted.get() && !sessionLifetimes.isTerminated
        }

    fun start(commandLine: GeneralCommandLine, workspaceLocation: String) {
        if (isRunning) {
            logger.warn("The caller should verify if an existing session is running, before calling start()")
            return
        }

        val sessionLifetime = sessionLifetimes.next()

        val newProcessHandler = object : ColoredProcessHandler(commandLine) {
            // If it's a long-running mostly idle daemon process, 'BaseOutputReader.Options.forMostlySilentProcess()' helps to reduce CPU usage.
            override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.forMostlySilentProcess()
        }

        sessionLifetime.onTermination {
            if (!newProcessHandler.isProcessTerminating && !newProcessHandler.isProcessTerminated) {
                logger.trace("Killing Azurite process")
                newProcessHandler.killProcess()
            }
        }

        newProcessHandler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(e: ProcessEvent, outputType: Key<*>) {}

            override fun processTerminated(e: ProcessEvent) {
                sessionLifetime.executeIfAlive {
                    logger.trace("Terminating Azurite session lifetime")
                    sessionLifetime.terminate(true)
                }
            }

            override fun startNotified(e: ProcessEvent) {}
        })

        sessionLifetime.bracketIfAlive({
            processHandler = newProcessHandler
            workspace = workspaceLocation
        }, {
            processHandler = null
            workspace = null
        })

        newProcessHandler.startNotify()

        setStartedSession()

        publishSessionStartedEvent(newProcessHandler, workspaceLocation)
    }

    private fun setStartedSession() {
        if (sessionStarted.compareAndSet(false, true)) {
            session = AzuriteStartedSession()
            syncServices()
        }
    }

    fun clean(workspace: File) {
        if (isRunning) stop()

        try {
            workspace.deleteRecursively()
            workspace.mkdir()
        } catch (e: Exception) {
            logger.error("Error during clean", e)
        }
    }

    fun stop() {
        sessionLifetimes.terminateCurrent()
        publishSessionStoppedEvent()
    }

    private fun syncServices() = application.messageBus
            .syncPublisher(ServiceEventListener.TOPIC)
            .handle(ServiceEventListener.ServiceEvent.createResetEvent(AzuriteServiceViewContributor::class.java))

    private fun publishSessionStartedEvent(processHandler: ColoredProcessHandler, workspace: String) = application.messageBus
            .syncPublisher(AzuriteSessionListener.TOPIC)
            .sessionStarted(processHandler, workspace)

    private fun publishSessionStoppedEvent() = application.messageBus
            .syncPublisher(AzuriteSessionListener.TOPIC)
            .sessionStopped()
}