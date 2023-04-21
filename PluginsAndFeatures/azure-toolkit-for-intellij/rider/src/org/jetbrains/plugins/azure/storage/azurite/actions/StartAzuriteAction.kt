/**
 * Copyright (c) 2020-2023 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.storage.azurite.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.AzureNotifications
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.orWhenNullOrEmpty
import org.jetbrains.plugins.azure.storage.azurite.Azurite
import org.jetbrains.plugins.azure.storage.azurite.AzuriteService
import java.io.File

class StartAzuriteAction
    : AnAction(
        message("action.azurite.start.name"),
        message("action.azurite.start.description"),
        AllIcons.Actions.Execute) {

    companion object {
        private const val AZURITE_PROCESS_TIMEOUT_MILLIS = 15000
    }

    private val logger = Logger.getInstance(StartAzuriteAction::class.java)
    private val azuriteService = service<AzuriteService>()

    override fun update(e: AnActionEvent) {
        if (azuriteService.isRunning) {
            e.presentation.isEnabled = false
            return
        }

        val properties = PropertiesComponent.getInstance()
        val packagePath = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE) ?: return
        val azuritePackage = Azurite.PackageDescriptor.createPackage(packagePath)
        e.presentation.isEnabled = !azuritePackage.isEmptyPath
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        logger.info("Start Azurite storage emulator...")

        if (azuriteService.isRunning) {
            logger.info("Skip start Azurite - already running")
            return
        }

        val properties = PropertiesComponent.getInstance()
        val nodeJsInterpreterRef = NodeJsInterpreterRef.create(properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_INTERPRETER) ?: "project")
        val nodeJsInterpreter = nodeJsInterpreterRef.resolve(project)
        if (nodeJsInterpreter == null) {
            logger.warn("Can not start Azurite - invalid configuration (no Node interpreter is configured)")
            showInvalidConfigurationNotification(project)
            return
        }

        val nodeJsLocalInterpreter = runCatching {
            NodeJsLocalInterpreter.castAndValidate(nodeJsInterpreter)
        }.getOrLogException(logger)

        if (nodeJsLocalInterpreter == null) {
            logger.warn("Can not start Azurite - invalid configuration (local Node interpreter could not be validated)")
            showInvalidConfigurationNotification(project)
            return
        }

        val packagePath = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE)
        if (packagePath.isNullOrEmpty()) {
            logger.warn("Can not start Azurite - invalid configuration (no Azurite package is configured)")
            showInvalidConfigurationNotification(project)
            return
        }

        val azuritePackage = Azurite.PackageDescriptor.createPackage(packagePath)
        val azuriteJsFile = azuritePackage.findBinFile("azurite", null)!!
        val azuriteWorkspaceLocation = AzureRiderSettings.getAzuriteWorkspacePath(properties, project).absolutePath

        logger.debug("Node JS interpreter: ${nodeJsLocalInterpreter.interpreterSystemDependentPath}")
        logger.debug("Azurite JS file: ${azuriteJsFile.absolutePath}")
        logger.debug("Azurite workspace: $azuriteWorkspaceLocation")

        val application = ApplicationManager.getApplication()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, message("service.azurite.starting.generic"), true, PerformInBackgroundOption.DEAF) {
            override fun run(indicator: ProgressIndicator) {

                indicator.text = message("service.azurite.starting.check.table.storage")
                val includeTableStorageParameters = supportsTableStorage(
                        nodeJsLocalInterpreter.interpreterSystemDependentPath, azuriteJsFile.absolutePath)
                logger.info("Azurite supports table storage: $includeTableStorageParameters")

                if (indicator.isCanceled) return

                indicator.text = message("service.azurite.starting.generic")
                application.invokeLaterOnWriteThread {
                    application.runWriteAction {
                        val commandLine = GeneralCommandLine(
                                nodeJsLocalInterpreter.interpreterSystemDependentPath,
                                azuriteJsFile.absolutePath,

                                "--blobHost",
                                properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_HOST_DEFAULT),
                                "--blobPort",
                                properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_PORT_DEFAULT),

                                "--queueHost",
                                properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_HOST_DEFAULT),
                                "--queuePort",
                                properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_PORT_DEFAULT),

                                "--location",
                                azuriteWorkspaceLocation)

                        if (includeTableStorageParameters) {
                            commandLine.addParameters(
                                    "--tableHost",
                                    properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_TABLE_HOST_DEFAULT),
                                    "--tablePort",
                                    properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_TABLE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_TABLE_PORT_DEFAULT)
                            )
                        }

                        if (properties.getBoolean(AzureRiderSettings.PROPERTY_AZURITE_LOOSE_MODE))
                            commandLine.addParameter("--loose")

                        properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PATH)?.let {
                            if (it.isNotEmpty()) {
                                commandLine.addParameter("--cert")
                                commandLine.addParameter(it)
                            }
                        }
                        properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_KEY_PATH)?.let {
                            if (it.isNotEmpty()) {
                                commandLine.addParameter("--key")
                                commandLine.addParameter(it)
                            }
                        }
                        properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PASSWORD)?.let {
                            if (it.isNotEmpty()) {
                                commandLine.addParameter("--pwd")
                                commandLine.addParameter(it)
                            }
                        }
                        
                        commandLine.environment.putAll(EnvironmentUtil.getEnvironmentMap())

                        if (indicator.isCanceled) return@runWriteAction

                        azuriteService.start(commandLine, azuriteWorkspaceLocation)
                    }
                }
            }
        })
    }

    private fun showInvalidConfigurationNotification(project: Project) = AzureNotifications.notify(
            project = project,
            title = message("action.azurite.start.configure.title"),
            content = message("action.azurite.start.configure.content"),
            type = NotificationType.WARNING,
            action = object : AnAction(message("action.azurite.start.configure.action.configure")) {
                override fun actionPerformed(e: AnActionEvent) = Azurite.showSettings(e.project)
            })

    private fun supportsTableStorage(nodeJsInterpreterPath: String, azuriteJsFilePath: String): Boolean {

        val nodeJsInterpreterExecutable = File(nodeJsInterpreterPath)
        val azuriteJsExecutable = File(azuriteJsFilePath)
        if (!nodeJsInterpreterExecutable.exists() || !azuriteJsExecutable.exists())
            return false

        try {
            val commandLine = GeneralCommandLine(
                    nodeJsInterpreterExecutable.path,
                    azuriteJsExecutable.path,
                    "--help"
            )

            logger.debug("Executing ${commandLine.commandLineString}...")

            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess(AZURITE_PROCESS_TIMEOUT_MILLIS, true)

            logger.debug("Result: ${output.stdout}")

            return output.stdoutLines
                    .any { it.contains("--tableHost", true) }
        } catch (e: Exception) {
            logger.error("Error while determining whether Azurite version at '$azuriteJsFilePath' supports table storage", e)
            return false
        }
    }
}
