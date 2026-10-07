/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.toolkit.intellij.legacy.WebAppBasePropertyView
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotPropertyViewPresenter

class WebAppDeploymentSlotPropertyView(virtualFile: VirtualFile) : WebAppBasePropertyView(virtualFile) {
    companion object {
        private const val ID = "com.microsoft.intellij.helpers.webapp.WebAppDeploymentSlotPropertyView"

        fun create(
            sid: String,
            resId: String,
            slotName: String,
            virtualFile: VirtualFile
        ): WebAppBasePropertyView {
            val view = WebAppDeploymentSlotPropertyView(virtualFile)
            view.onLoadWebAppProperty(sid, resId, slotName)
            return view
        }
    }

    override fun getId() = ID

    override fun createPresenter(): WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>? =
        DeploymentSlotPropertyViewPresenter() as? WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>
}