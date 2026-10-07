/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.microsoft.azure.toolkit.intellij.common.BaseEditor
import com.microsoft.azure.toolkit.intellij.legacy.appservice.table.AppSettingsTable
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter

abstract class WebAppBasePropertyView(virtualFile: VirtualFile) : BaseEditor(virtualFile), WebAppBasePropertyMvpView {
    companion object {
        private const val TXT_NA = "N/A"
    }

    private val presenter: WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>?

    private lateinit var resourceGroupLabel: Cell<JBLabel>
    private lateinit var appServicePlanLabel: Cell<JBLabel>
    private lateinit var statusLabel: Cell<JBLabel>
    private lateinit var urlLabel: Cell<JBLabel>
    private lateinit var locationLabel: Cell<JBLabel>
    private lateinit var pricingTierLabel: Cell<JBLabel>
    private lateinit var sidLabel: Cell<JBLabel>
    private lateinit var settingsTable: AppSettingsTable

    private val mainPanel = panel {
        collapsibleGroup("Overview") {
            row("Resource Group:") {
                resourceGroupLabel = copyableLabel("Loading...")
            }
            row("App Service Plan:") {
                appServicePlanLabel = copyableLabel("Loading...")
            }
            row("Status:") {
                statusLabel = copyableLabel("Loading...")
            }
            row("URL:") {
                urlLabel = copyableLabel("Loading...")
            }
            row("Location:") {
                locationLabel = copyableLabel("Loading...")
            }
            row("Pricing Tier:") {
                pricingTierLabel = copyableLabel("Loading...")
            }
            row("Subscription ID:") {
                sidLabel = copyableLabel("Loading...")
            }
        }.apply {
            expanded = true
        }
        collapsibleGroup("App Settings") {
            row {
                settingsTable = AppSettingsTable().apply {
                    emptyText.text = "No app setting configured"
                }
                cell(settingsTable)
                    .align(Align.FILL)
            }
        }.apply {
            expanded = true
        }
    }

    init {
        presenter = createPresenter()
        presenter?.onAttachView(this)
    }

    protected abstract fun getId(): String

    protected abstract fun createPresenter(): WebAppBasePropertyViewPresenter<WebAppBasePropertyMvpView, AppServiceAppBase<*, *, *>>?

    override fun getName() = getId()

    override fun getComponent() = mainPanel

    override fun onLoadWebAppProperty(sid: String, webAppId: String, slotName: String?) {
        presenter?.onLoadWebAppProperty(sid, webAppId, slotName)
    }

    override fun showProperty(property: WebAppProperty) {
        val resourceGroup = property.getValue(WebAppPropertyViewPresenter.KEY_RESOURCE_GRP) as? String ?: TXT_NA
        resourceGroupLabel.component.text = resourceGroup
        val appServicePlan = property.getValue(WebAppPropertyViewPresenter.KEY_PLAN) as? String ?: TXT_NA
        appServicePlanLabel.component.text = appServicePlan
        val status = property.getValue(WebAppPropertyViewPresenter.KEY_STATUS) as? String ?: TXT_NA
        statusLabel.component.text = status
        val location = property.getValue(WebAppPropertyViewPresenter.KEY_LOCATION) as? String ?: TXT_NA
        locationLabel.component.text = location
        val pricingTier = property.getValue(WebAppPropertyViewPresenter.KEY_PRICING) as? String ?: TXT_NA
        pricingTierLabel.component.text = pricingTier
        val sid = property.getValue(WebAppPropertyViewPresenter.KEY_SUB_ID) as? String ?: TXT_NA
        sidLabel.component.text = sid
        val urlProperty = property.getValue(WebAppPropertyViewPresenter.KEY_URL) as? String
        if (urlProperty != null) {
            urlLabel.component.text = "https://$urlProperty"
        } else {
            urlLabel.component.text = TXT_NA
        }

        val appSettings: Map<String, String> =
            property.getValue(WebAppPropertyViewPresenter.KEY_APP_SETTING) as? Map<String, String> ?: emptyMap()
        settingsTable.setAppSettings(appSettings)
    }

    override fun showPropertyUpdateResult(isSuccess: Boolean) {
    }

    override fun showGetPublishingProfileResult(isSuccess: Boolean) {
    }

    override fun dispose() {
        presenter?.onDetachView()
    }
}

fun Row.copyableLabel(text: String) = cell(JBLabel(text).apply { setCopyable(true) })