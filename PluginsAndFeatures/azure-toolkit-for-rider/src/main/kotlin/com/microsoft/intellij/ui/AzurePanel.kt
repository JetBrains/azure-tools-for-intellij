package com.microsoft.intellij.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.AzureConfiguration

class AzurePanel {
    fun init() {
        val config = Azure.az().config()
        setData(config)
    }

    private lateinit var originalConfig: AzureConfiguration

    private fun setData(config: AzureConfiguration) {
        originalConfig = config
    }

    private fun getData(): AzureConfiguration {
        val data = AzureConfiguration()
        return data
    }

    fun getPanel(): JBScrollPane {
        val panel = panel {

        }
        return JBScrollPane(panel)
    }

    fun isModified(): Boolean {
        val original = originalConfig
        val newConfig = getData()

        return false
    }

    fun apply() {
        val newConfig = getData()
    }

    fun reset() {
        setData(originalConfig)
    }
}