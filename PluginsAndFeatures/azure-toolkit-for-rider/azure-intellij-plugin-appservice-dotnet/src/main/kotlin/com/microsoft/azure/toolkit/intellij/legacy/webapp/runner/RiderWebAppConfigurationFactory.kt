package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class RiderWebAppConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    companion object {
        private const val FACTORY_ID = "Azure Web App"
        private const val FACTORY_NAME = "Azure Web App"
    }

    override fun getId() = FACTORY_ID

    override fun getName() = FACTORY_NAME

    override fun createTemplateConfiguration(project: Project) =
        RiderWebAppConfiguration(project, this, project.name)

    override fun createConfiguration(name: String?, template: RunConfiguration) =
        RiderWebAppConfiguration(template.project, this, name)
}