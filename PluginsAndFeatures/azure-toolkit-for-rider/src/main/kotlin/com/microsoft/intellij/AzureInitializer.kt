package com.microsoft.intellij

object AzureInitializer {
    fun initialize() {
        ProxyUtils.initProxy()
        initializeAzureConfiguration()
    }

    private fun initializeAzureConfiguration() {
        IntellijConfigInitializer.initialize()
    }
}