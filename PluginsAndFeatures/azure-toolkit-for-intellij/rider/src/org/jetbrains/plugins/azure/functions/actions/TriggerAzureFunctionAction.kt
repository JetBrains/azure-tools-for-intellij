/**
 * Copyright (c) 2019-2024 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.actions

import com.intellij.httpClient.http.request.HttpRequestLanguage
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.httpClient.RestClientIcons
import com.jetbrains.rider.azure.model.FunctionAppHttpTriggerAttribute
import com.jetbrains.rider.azure.model.FunctionAppTriggerType
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.io.IOException

class TriggerAzureFunctionAction(
    private val functionName: String,
    private val triggerType: FunctionAppTriggerType,
    private val httpTriggerAttribute: FunctionAppHttpTriggerAttribute?)
    : AnAction(
        message("action.function_app.trigger_function_app.name", functionName),
        message("action.function_app.trigger_function_app.description"),
        RestClientIcons.Http_requests_filetype) {

    companion object {
        private val logger by lazy { Logger.getInstance(TriggerAzureFunctionAction::class.java) }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val scratchFileName = "Trigger_$functionName.http"
        val scratchFileService = ScratchFileService.getInstance()
        val scratchRootType = ScratchRootType.getInstance()
        val scratchFile = scratchFileService.findFile(scratchRootType, scratchFileName, ScratchFileService.Option.existing_only)
                ?: scratchRootType.createScratchFile(
                        project,
                        scratchFileName,
                        HttpRequestLanguage.INSTANCE,
                        when (triggerType) {
                            FunctionAppTriggerType.HttpTrigger -> generateHttpClientRequestForHttpTrigger(
                                project = project,
                                templateName = message("action.function_app.trigger_function_app.template.http.name"),
                                functionName = functionName,
                                httpTriggerAttribute = httpTriggerAttribute
                            )

                            FunctionAppTriggerType.Other -> generateHttpClientRequestForOtherTrigger(
                                project = project,
                                templateName = message("action.function_app.trigger_function_app.template.other.name"),
                                functionName = functionName
                            )
                        } ?: "",
                        ScratchFileService.Option.create_if_missing)

        if (scratchFile != null) {
            FileEditorManager.getInstance(project).openFile(scratchFile, true)
        }
    }

    private fun generateHttpClientRequestForHttpTrigger(
        project: Project,
        templateName: String,
        functionName: String,
        httpTriggerAttribute: FunctionAppHttpTriggerAttribute?): String? {

        val requestMethod = if (httpTriggerAttribute == null || httpTriggerAttribute.methods.isEmpty()) {
            "GET"
        } else {
            httpTriggerAttribute.methods.first()
        }

        val urlPath = httpTriggerAttribute?.let {
            if (!it.routeForHttpClient.isNullOrEmpty()) {
                it.routeForHttpClient
            } else if (!it.route.isNullOrEmpty()) {
                it.route
            } else {
                functionName
            }
        } ?: functionName

        return createContentFromTemplate(project, templateName, requestMethod, functionName, urlPath)
    }

    private fun generateHttpClientRequestForOtherTrigger(project: Project, templateName: String, functionName: String) =
        createContentFromTemplate(project, templateName, "POST", functionName, functionName)

    private fun createContentFromTemplate(project: Project, templateName: String, requestMethod: String, functionName: String, urlPath: String): String? {
        val template = FileTemplateManager.getInstance(project).findInternalTemplate(templateName)
        if (template != null) {
            try {
                val properties = FileTemplateManager.getInstance(project).defaultProperties
                properties.setProperty("requestMethod", requestMethod.uppercase())
                properties.setProperty("functionName", functionName)
                properties.setProperty("urlPath", urlPath)
                return template.getText(properties)
            } catch (e: IOException) {
                logger.warn(e)
            }
        }
        return null
    }
}