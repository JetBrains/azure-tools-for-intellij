package com.microsoft.azure.toolkit.intellij.legacy.common

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.openapi.project.Project

abstract class RiderAzureRunConfigurationBase<T>(project: Project, factory: ConfigurationFactory, name: String?):
    LocatableConfigurationBase<T>(project, factory, name)