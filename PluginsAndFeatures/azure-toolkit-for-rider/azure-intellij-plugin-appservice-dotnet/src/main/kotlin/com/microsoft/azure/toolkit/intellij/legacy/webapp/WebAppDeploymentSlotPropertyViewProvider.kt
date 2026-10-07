/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction

class WebAppDeploymentSlotPropertyViewProvider : WebAppBasePropertyViewProvider() {
    companion object {
        const val TYPE = "DEPLOYMENT_SLOT_PROPERTY"
    }

    override fun getType() = TYPE

    override fun createEditor(
        project: Project,
        virtualFile: VirtualFile
    ): FileEditor {
        val sid = virtualFile.getUserData(OpenAppServicePropertyViewAction.SUBSCRIPTION_ID)
            ?: error("Unable to determine subscription id")
        val webAppId = virtualFile.getUserData(OpenAppServicePropertyViewAction.WEBAPP_ID)
            ?: error("Unable to determine web app id")
        val slotName = virtualFile.getUserData(OpenAppServicePropertyViewAction.SLOT_NAME)
            ?: error("Unable to determine slot name")
        return WebAppDeploymentSlotPropertyView.create(sid, webAppId, slotName, virtualFile)
    }
}