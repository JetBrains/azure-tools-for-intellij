/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.actions

import com.intellij.httpClient.RestClientIcons
import com.intellij.httpClient.http.request.HttpRequestLanguage
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.rider.azure.model.FunctionAppHttpTriggerAttribute
import com.jetbrains.rider.azure.model.FunctionAppTriggerType
import java.io.IOException

class TriggerAzureFunctionAction(
    private val functionName: String,
    private val triggerType: FunctionAppTriggerType,
    private val httpTriggerAttribute: FunctionAppHttpTriggerAttribute?
) : AnAction(
    "Trigger '$functionName'",
    "Create trigger function HTTP request...",
    RestClientIcons.Http_requests_filetype
) {
    companion object {
        private val LOG = logger<TriggerAzureFunctionAction>()

        private const val HTTP_TEMPLATE_NAME = "Trigger Azure HTTP Function"
        private const val OTHER_TEMPLATE_NAME = "Trigger Azure Function"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val scratchFileName = "Trigger_$functionName.http"
        val scratchFileService = ScratchFileService.getInstance()
        val scratchRootType = ScratchRootType.getInstance()

        val scratchFileText = when (triggerType) {
            FunctionAppTriggerType.HttpTrigger -> generateHttpClientRequestForHttpTrigger(
                project = project,
                templateName = HTTP_TEMPLATE_NAME,
                functionName = functionName,
                httpTriggerAttribute = httpTriggerAttribute
            )

            FunctionAppTriggerType.Other -> generateHttpClientRequestForOtherTrigger(
                project = project,
                templateName = OTHER_TEMPLATE_NAME,
                functionName = functionName
            )
        } ?: ""
        val scratchFile =
            scratchFileService.findFile(scratchRootType, scratchFileName, ScratchFileService.Option.existing_only)
                ?: scratchRootType.createScratchFile(
                    project,
                    scratchFileName,
                    HttpRequestLanguage.INSTANCE,
                    scratchFileText,
                    ScratchFileService.Option.create_if_missing
                )

        if (scratchFile != null) {
            FileEditorManager.getInstance(project).openFile(scratchFile, true)
        }
    }

    private fun generateHttpClientRequestForHttpTrigger(
        project: Project,
        templateName: String,
        functionName: String,
        httpTriggerAttribute: FunctionAppHttpTriggerAttribute?
    ): String? {

        val requestMethod = if (httpTriggerAttribute == null || httpTriggerAttribute.methods.isEmpty()) {
            "GET"
        } else {
            httpTriggerAttribute.methods.firstOrNull()
        } ?: "GET"

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

    private fun createContentFromTemplate(
        project: Project,
        templateName: String,
        requestMethod: String,
        functionName: String,
        urlPath: String
    ): String? {
        val template = FileTemplateManager.getInstance(project).findInternalTemplate(templateName)
        if (template != null) {
            try {
                val properties = FileTemplateManager.getInstance(project).defaultProperties
                properties.setProperty("requestMethod", requestMethod.uppercase())
                properties.setProperty("functionName", functionName)
                properties.setProperty("urlPath", urlPath)
                return template.getText(properties)
            } catch (e: IOException) {
                LOG.warn(e)
            }
        }
        return null
    }
}