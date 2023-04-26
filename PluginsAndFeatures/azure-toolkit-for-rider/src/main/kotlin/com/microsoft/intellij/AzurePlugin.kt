package com.microsoft.intellij

import com.intellij.ide.plugins.PluginManager

class AzurePlugin {
    companion object {
        const val PLUGIN_ID = "com.intellij.resharper.azure"
        val PLUGIN_VERSION: String = PluginManager.getPlugins().single { it.pluginId.idString == PLUGIN_ID }.version
    }
}