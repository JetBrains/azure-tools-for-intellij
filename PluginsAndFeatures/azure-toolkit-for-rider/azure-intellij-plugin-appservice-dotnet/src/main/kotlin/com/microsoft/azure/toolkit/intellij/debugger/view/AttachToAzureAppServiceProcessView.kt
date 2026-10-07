/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger.view

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.attach.XAttachDebuggerProvider
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachDialogHostType
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachDialogState
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachHostItem
import com.intellij.xdebugger.impl.ui.attach.dialog.AttachToProcessViewWithHosts
import com.intellij.xdebugger.impl.ui.attach.dialog.items.columns.AttachDialogColumnsLayout
import com.microsoft.azure.toolkit.intellij.debugger.AzureAttachDialogHostType
import com.microsoft.azure.toolkit.intellij.debugger.attachHosts.AzureAttachHostFactory
import com.microsoft.azure.toolkit.intellij.debugger.canBeDebugged
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttachToAzureAppServiceProcessView(
    private val project: Project,
    state: AttachDialogState,
    columnsLayout: AttachDialogColumnsLayout,
    attachDebuggerProviders: List<XAttachDebuggerProvider>
) : AttachToProcessViewWithHosts(project, state, columnsLayout, attachDebuggerProviders) {

    companion object {
        private const val SELECTED_APP_SERVICE_KEY = "ATTACH_DIALOG_SELECTED_APP_SERVICE"
    }

    override val addHostButtonAction = AddConnectionButtonAction()
    override val hostsComboBoxAction = AttachHostsComboBoxAction(true)

    override fun getHostActions(
        hosts: Set<AttachHostItem>,
        selectHost: (host: AttachHostItem) -> Unit
    ): List<AnAction> {
        return hosts.filterIsInstance<AttachAzureAppServiceItem>().mapNotNull { host ->
            if (!host.isRunning()) return@mapNotNull null
            return@mapNotNull SelectAppServiceAction(host, selectHost)
        }
    }

    override fun getHostType(): AttachDialogHostType = AzureAttachDialogHostType

    override suspend fun getHosts(): List<AttachHostItem> {
        if (!Azure.az(AzureAccount::class.java).isLoggedIn) return emptyList()

        return withContext(Dispatchers.IO) {
            val appServicePlans = async { loadAppServicePlans() }
            val functions = async { getAllFunctions() }
            val webApps = async { getAllWebApps() }

            appServicePlans.await()
            awaitAll(functions, webApps).flatten().filter { it.canBeDebugged() }.toServiceItems()
        }
    }

    override fun getSavedHost(allHosts: Set<AttachHostItem>): AttachHostItem? {
        val savedValue = PropertiesComponent.getInstance(project).getValue(SELECTED_APP_SERVICE_KEY) ?: return null
        return allHosts.filterIsInstance<AttachAzureAppServiceItem>().firstOrNull { it.toString() == savedValue }
    }

    override fun isAddingConnectionEnabled(): Boolean = false

    override fun openSettings() {
        throw UnsupportedOperationException("Editing settings for Azure is not supported")
    }

    override fun openSettingsAndCreateTemplate() {
        throw UnsupportedOperationException("Editing settings for Azure is not supported")
    }

    override fun saveSelectedHost(host: AttachHostItem?) {
        if (host !is AttachAzureAppServiceItem) return

        val containerPresentationKey = host.toString()
        PropertiesComponent.getInstance(project).setValue(SELECTED_APP_SERVICE_KEY, containerPresentationKey)
    }

    private suspend fun getAllFunctions(): List<FunctionApp> {
        val functionApps = Azure.az(AzureFunctions::class.java).functionApps()
        coroutineScope {
            functionApps.forEach {
                launch { it.remote }
                launch { it.slots().list() }
            }
        }
        return functionApps
    }

    private suspend fun getAllWebApps(): List<WebApp> {
        val webApps = Azure.az(AzureWebApp::class.java).webApps()
        coroutineScope {
            webApps.forEach {
                launch { it.remote }
                launch { it.slots().list() }
            }
        }
        return webApps
    }

    private suspend fun loadAppServicePlans() {
        val appServicePlans = Azure.az(AzureAppService::class.java).plans()
        coroutineScope {
            appServicePlans.forEach { appServicePlan ->
                launch { appServicePlan.remote }
            }
        }
    }

    private fun List<AppServiceAppBase<*, *, *>>.toServiceItems(): List<AttachAzureAppServiceItem> {
        return mapNotNull {
            val attachHost = AzureAttachHostFactory.create(project, it) ?: return@mapNotNull null
            AttachAzureAppServiceItem(attachHost)
        }
    }

    private inner class SelectAppServiceAction(
        private val item: AttachHostItem,
        private val selectItem: (host: AttachHostItem) -> Unit
    ) : AnAction({ item.getPresentation() }, item.getIcon()) {
        override fun actionPerformed(e: AnActionEvent) {
            selectItem(item)
            updateProcesses()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = true
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }
}
