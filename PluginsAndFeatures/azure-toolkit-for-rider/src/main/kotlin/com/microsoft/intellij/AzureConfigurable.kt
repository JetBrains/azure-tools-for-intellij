package com.microsoft.intellij

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.microsoft.intellij.ui.AzurePanel
import javax.swing.JComponent

class AzureConfigurable : SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {
    companion object {
        private const val AZURE_CONFIGURABLE_ID = "com.microsoft.intellij.RiderAzureConfigurable"
        private const val DISPLAY_NAME = "Azure"
    }

    private val panel = AzurePanel()

    override fun getDisplayName() = DISPLAY_NAME
    override fun getId() = AZURE_CONFIGURABLE_ID

    override fun createComponent(): JComponent {
        panel.init()
        return panel.getPanel()
    }

    override fun isModified() = panel.isModified()

    override fun apply() {
        panel.apply()
    }

    override fun reset() {
        panel.reset()
    }
}