/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.actions

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFile
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.AzureFileType
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppDeploymentSlotPropertyViewProvider
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppPropertyViewProvider
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppDeploymentSlotPropertyViewProvider
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppPropertyViewProvider
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager

object OpenAppServicePropertyViewAction {
    val SUBSCRIPTION_ID = Key<String>("subscriptionId")
    val RESOURCE_ID = Key<String>("resourceId")
    val WEBAPP_ID = Key<String>("webAppId")
    val SLOT_NAME = Key<String>("slotName")

    fun openWebAppPropertyView(webApp: WebApp, project: Project) {
        val sid = webApp.subscriptionId
        val resourceId = webApp.id
        if (sid.isEmpty() && resourceId.isEmpty()) return

        val fileEditorManager = FileEditorManager.getInstance(project) ?: return
        val type = WebAppPropertyViewProvider.TYPE
        val itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId)
        if (itemVirtualFile != null) {
            openFile(fileEditorManager, itemVirtualFile)
        } else {
            val newItemVirtualFile = createVirtualFile(webApp.name, sid, resourceId)
            newItemVirtualFile.fileType = AzureFileType(type, IntelliJAzureIcons.getIcon(AzureIcons.WebApp.MODULE))
            openFile(fileEditorManager, newItemVirtualFile)
        }
    }

    fun openDeploymentSlotPropertyView(slot: WebAppDeploymentSlot, project: Project) {
        val sid = slot.subscriptionId
        val resourceId = slot.id
        if (sid.isEmpty() && resourceId.isEmpty()) return

        val fileEditorManager = FileEditorManager.getInstance(project) ?: return
        val type = WebAppDeploymentSlotPropertyViewProvider.TYPE
        val itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId)
        if (itemVirtualFile != null) {
            openFile(fileEditorManager, itemVirtualFile)
        } else {
            val userData = buildMap {
                put(SUBSCRIPTION_ID, sid)
                put(RESOURCE_ID, resourceId)
                put(WEBAPP_ID, slot.parent.id)
                put(SLOT_NAME, slot.name)
            }
            val name = "${slot.parent.name}-${slot.name}"
            val newItemVirtualFile = createVirtualFile(name, userData)
            newItemVirtualFile.fileType =
                AzureFileType(type, IntelliJAzureIcons.getIcon(AzureIcons.WebApp.DEPLOYMENT_SLOT))
            openFile(fileEditorManager, newItemVirtualFile)
        }
    }

    fun openFunctionAppPropertyView(functionApp: FunctionApp, project: Project) {
        val sid = functionApp.subscriptionId
        val resourceId = functionApp.id
        if (sid.isEmpty() && resourceId.isEmpty()) return

        val fileEditorManager = FileEditorManager.getInstance(project) ?: return
        val type = FunctionAppPropertyViewProvider.TYPE
        val itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId)
        if (itemVirtualFile != null) {
            openFile(fileEditorManager, itemVirtualFile)
        } else {
            val newItemVirtualFile = createVirtualFile(functionApp.name, sid, resourceId)
            newItemVirtualFile.fileType = AzureFileType(type, IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE))
            openFile(fileEditorManager, newItemVirtualFile)
        }
    }

    fun openFunctionAppDeploymentSlotPropertyView(slot: FunctionAppDeploymentSlot, project: Project) {
        val sid = slot.subscriptionId
        val resourceId = slot.id
        if (sid.isEmpty() && resourceId.isEmpty()) return

        val fileEditorManager = FileEditorManager.getInstance(project) ?: return
        val type = FunctionAppDeploymentSlotPropertyViewProvider.TYPE
        val itemVirtualFile = searchExistingFile(fileEditorManager, type, resourceId)
        if (itemVirtualFile != null) {
            openFile(fileEditorManager, itemVirtualFile)
        } else {
            val userData = buildMap {
                put(SUBSCRIPTION_ID, sid)
                put(RESOURCE_ID, resourceId)
                put(WEBAPP_ID, slot.parent.id)
                put(SLOT_NAME, slot.name)
            }
            val name = "${slot.parent.name}-${slot.name}"
            val newItemVirtualFile = createVirtualFile(name, userData)
            newItemVirtualFile.fileType =
                AzureFileType(type, IntelliJAzureIcons.getIcon(AzureIcons.WebApp.DEPLOYMENT_SLOT))
            openFile(fileEditorManager, newItemVirtualFile)
        }
    }

    private fun searchExistingFile(
        fileEditorManager: FileEditorManager,
        fileType: String,
        resourceId: String
    ): LightVirtualFile? {
        val openedFiled = fileEditorManager.openFiles
        for (editedFile in openedFiled) {
            val fileResourceId = editedFile.getUserData(RESOURCE_ID) ?: continue
            if (fileResourceId == resourceId && editedFile.fileType.name == fileType) {
                return editedFile as? LightVirtualFile
            }
        }

        return null
    }

    private fun createVirtualFile(name: String, sid: String, resId: String): LightVirtualFile {
        val itemVirtualFile = LightVirtualFile(name)
        itemVirtualFile.putUserData(RESOURCE_ID, resId)
        itemVirtualFile.putUserData(SUBSCRIPTION_ID, sid)
        return itemVirtualFile
    }

    private fun createVirtualFile(name: String, userData: Map<Key<String>, String>): LightVirtualFile {
        val itemVirtualFile = LightVirtualFile(name)
        userData.entries.forEach { (key, value) -> itemVirtualFile.putUserData(key, value) }
        return itemVirtualFile
    }

    private fun openFile(fileEditorManager: FileEditorManager, itemVirtualFile: LightVirtualFile) {
        AzureTaskManager.getInstance().runLater { fileEditorManager.openFile(itemVirtualFile, true, true) }
    }
}