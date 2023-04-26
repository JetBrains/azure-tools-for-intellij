package com.microsoft.intellij

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager
import com.microsoft.azure.toolkit.ide.common.store.DefaultMachineStore
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore
import com.microsoft.azure.toolkit.intellij.common.task.IntellijAzureTaskManager
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.azuretools.authmanage.CommonSettings
import com.microsoft.azuretools.azurecommons.util.FileUtil
import com.microsoft.intellij.helpers.IDEHelperImpl
import com.microsoft.intellij.helpers.UiHelperImpl
import com.microsoft.intellij.util.PluginHelper
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.components.PluginComponent
import com.microsoft.tooling.msservices.components.PluginSettings
import com.microsoft.tooling.msservices.serviceexplorer.Node

class AzureActionsListener: AppLifecycleListener, PluginComponent {
    companion object{
        private const val AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ"
        private const val AZURE_TOOLS_FOLDER_DEPRECATED = "AzureToolsForIntelliJ"
    }

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        DefaultLoader.setPluginComponent(this)
        DefaultLoader.setUiHelper(UiHelperImpl())
        DefaultLoader.setIdeHelper(IDEHelperImpl())
        AzureTaskManager.register(IntellijAzureTaskManager())
        AzureRxTaskManager.register()
        AzureStoreManager.register(
            DefaultMachineStore(PluginHelper.getTemplateFile("azure.json")),
            IntellijStore.getInstance(),
            IntelliJSecureStore.getInstance()
        )
        AzureInitializer.initialize()
        IntellijAzureActionManager.register()
        Node.setNode2Actions(mutableMapOf())

        initAuthManage()
        val am = ActionManager.getInstance()
        val toolbarGroup = am.getAction(IdeActions.GROUP_MAIN_TOOLBAR) as DefaultActionGroup
        toolbarGroup.addAll(am.getAction("AzureToolbarGroup") as DefaultActionGroup)
        val popupGroup = am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP) as DefaultActionGroup
        popupGroup.add(am.getAction("AzurePopupGroup"))
    }

    private fun initAuthManage() {
        val baseFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER).toString()
        val deprecatedFolder = FileUtil.getDirectoryWithinUserHome(AZURE_TOOLS_FOLDER_DEPRECATED).toString()
        CommonSettings.setUpEnvironment(baseFolder, deprecatedFolder)
    }

    override fun getSettings(): PluginSettings {
        TODO("Not yet implemented")
    }

    override fun getPluginId(): String = "com.intellij.resharper.azure"
}