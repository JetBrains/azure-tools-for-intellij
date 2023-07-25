package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class RiderWebAppConfigurationType : ConfigurationTypeBase(
    "AzureDotNetWebAppPublish",
    "Azure - Publish Web App",
    "Azure Publish Web App configuration",
    IntelliJAzureIcons.getIcon(AzureIcons.WebApp.DEPLOY)
) {
    init {
        addFactory(RiderWebAppConfigurationFactory(this))
    }
}