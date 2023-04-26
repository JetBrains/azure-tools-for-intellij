package com.microsoft.intellij.util

import com.intellij.ide.plugins.PluginManager
import com.microsoft.intellij.AzurePlugin
import kotlin.io.path.absolutePathString

object PluginUtil {
    fun getPluginRootDirectory(): String {
        val pluginDescriptor = PluginManager.getPlugins().single { it.pluginId.idString == AzurePlugin.PLUGIN_ID }
        return pluginDescriptor.pluginPath.absolutePathString()
    }
}