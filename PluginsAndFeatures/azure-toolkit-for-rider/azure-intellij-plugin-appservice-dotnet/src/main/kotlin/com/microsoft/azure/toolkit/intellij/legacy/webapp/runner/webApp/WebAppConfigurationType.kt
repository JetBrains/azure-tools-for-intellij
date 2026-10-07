/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class WebAppConfigurationType : SimpleConfigurationType(
    "AzureWebAppDeploy",
    "Azure - Web App",
    "Azure Publish Web App configuration",
    NotNullLazyValue.createValue { IntelliJAzureIcons.getIcon(AzureIcons.WebApp.DEPLOY) }
) {
    override fun createTemplateConfiguration(project: Project) =
        WebAppConfiguration(project, this, project.name)

    override fun createConfiguration(name: String?, template: RunConfiguration) =
        WebAppConfiguration(template.project, this, name)

    override fun getOptionsClass() = WebAppConfigurationOptions::class.java
}