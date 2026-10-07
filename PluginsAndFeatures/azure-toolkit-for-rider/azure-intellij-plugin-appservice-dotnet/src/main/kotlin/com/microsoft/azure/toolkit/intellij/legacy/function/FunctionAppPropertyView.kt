/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.toolkit.intellij.legacy.WebAppBasePropertyView
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter

class FunctionAppPropertyView(virtualFile: VirtualFile) : WebAppBasePropertyView(virtualFile) {
    companion object {
        private const val ID = "com.microsoft.azure.toolkit.intellij.function.FunctionAppPropertyView"

        fun create(
            sid: String,
            resId: String,
            virtualFile: VirtualFile
        ): WebAppBasePropertyView {
            val view = FunctionAppPropertyView(virtualFile)
            view.onLoadWebAppProperty(sid, resId, null)
            return view
        }
    }

    override fun getId() = ID

    override fun createPresenter(): WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>> =
        object : WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>() {
            override fun getWebAppBase(
                sid: String,
                appId: String,
                slotName: String?
            ) = Azure.az(AzureFunctions::class.java).functionApp(appId)

            override fun updateAppSettings(
                sid: String,
                appId: String,
                name: String?,
                toUpdate: Map<String?, String?>,
                toRemove: Set<String?>
            ) {
                val functionApp = getWebAppBase(sid, appId, name)
                val draft = functionApp?.update() as? FunctionAppDraft
                draft?.appSettings = toUpdate
                toRemove.forEach { key -> draft?.removeAppSetting(key) }
                draft?.updateIfExist()
            }
        }
}