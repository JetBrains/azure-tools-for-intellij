package com.microsoft.intellij

import com.intellij.ide.AppLifecycleListener
import com.microsoft.tooling.msservices.components.PluginComponent
import com.microsoft.tooling.msservices.components.PluginSettings
import com.microsoft.tooling.msservices.serviceexplorer.Node

class AzureActionsListener: AppLifecycleListener, PluginComponent {
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        Node.setNode2Actions(mutableMapOf())
    }

    override fun getSettings(): PluginSettings {
        TODO("Not yet implemented")
    }

    override fun getPluginId(): String = "com.intellij.resharper.azure"
}