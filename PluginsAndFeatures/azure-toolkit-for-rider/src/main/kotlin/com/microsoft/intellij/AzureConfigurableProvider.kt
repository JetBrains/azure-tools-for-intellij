package com.microsoft.intellij

import com.intellij.openapi.options.ConfigurableProvider

class AzureConfigurableProvider : ConfigurableProvider() {
    override fun canCreateConfigurable() = true
    override fun createConfigurable() = AzureConfigurable()
}