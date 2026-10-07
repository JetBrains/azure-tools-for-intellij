/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp

import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.toolkit.intellij.legacy.WebAppBasePropertyView
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter

class WebAppPropertyView(virtualFile: VirtualFile) : WebAppBasePropertyView(virtualFile) {
    companion object {
        private const val ID = "com.microsoft.intellij.helpers.webapp.WebAppPropertyView"

        fun create(
            sid: String,
            resId: String,
            virtualFile: VirtualFile
        ): WebAppBasePropertyView {
            val view = WebAppPropertyView(virtualFile)
            view.onLoadWebAppProperty(sid, resId, null)
            return view
        }
    }

    override fun getId() = ID

    override fun createPresenter(): WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>? =
        WebAppPropertyViewPresenter() as? WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>
}