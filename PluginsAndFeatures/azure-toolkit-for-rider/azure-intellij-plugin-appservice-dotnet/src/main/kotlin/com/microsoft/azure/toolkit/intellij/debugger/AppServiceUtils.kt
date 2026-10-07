/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.debugger

import com.azure.resourcemanager.appservice.models.WebAppBase
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppNodeProvider
import com.microsoft.azure.toolkit.ide.appservice.webapp.WebAppNodeProvider
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.Icon

fun AppServiceAppBase<*, *, *>.operatingSystem(): OperatingSystem {
    return runtime?.operatingSystem ?: appServicePlan?.operatingSystem ?: OperatingSystem.WINDOWS
}

fun AppServiceAppBase<*, *, *>.getIcon(): Icon? {
    val azureIcon = getAzureIcon() ?: return null
    return azureIcon.asIntellij()
}

fun AppServiceAppBase<*, *, *>.getAzureIcon(): AzureIcon? {
    return getIconProvider()?.getIcon(this)
}

fun AzureIcon.asIntellij(): Icon {
    return IntelliJAzureIcons.getIcon(this)
}

fun AppServiceAppBase<*, *, *>.canBeDebugged(): Boolean {
    return isRunning() && !isDraft && isDebugSupported()
}

fun AppServiceAppBase<*, *, *>.isDebugSupported(): Boolean {
    val os = operatingSystem()

    return when (this) {
        is WebApp -> os != OperatingSystem.DOCKER
        is FunctionApp -> os == OperatingSystem.WINDOWS
        else -> false
    }
}

fun AppServiceAppBase<*, *, *>.isRunning(): Boolean {
    val formalStatus = formalStatus ?: return false
    return when(formalStatus) {
        AzResource.FormalStatus.RUNNING,
        AzResource.FormalStatus.WRITING,
        AzResource.FormalStatus.READING -> true

        AzResource.FormalStatus.STOPPED,
        AzResource.FormalStatus.CREATING,
        AzResource.FormalStatus.FAILED,
        AzResource.FormalStatus.DELETED,
        AzResource.FormalStatus.UNKNOWN,
        AzResource.FormalStatus.DELETING -> false
    }
}

// TODO: Move it to the base plugin (https://github.com/microsoft/azure-maven-plugins/pull/2512)
suspend fun AppServiceAppBase<*, *, *>.enableWebSockets() {
    withContext(Dispatchers.Default) {
        invokeDoModify({
            val updatable =
                getUpdatable()
                    ?: throw UnsupportedOperationException("Given app service type is not supported: $this")
            updatable.withWebSocketsEnabled(true).apply()
        }, AzResource.Status.UPDATING)
    }
}

private fun AppServiceAppBase<*, *, *>.invokeDoModify(body: Runnable, status: String?) {
    val instance = this
    val clazz = AbstractAzResource::class.java
    val method = clazz.getDeclaredMethod("doModify", Runnable::class.java, String::class.java)

    method.apply {
        isAccessible = true
        invoke(instance, body, status)
    }
}

private fun AppServiceAppBase<*, *, *>.getUpdatable(): WebAppBase.Update<*>? {
    return when (this) {
        is FunctionApp -> remote?.update()
        is WebApp -> remote?.update()
        else -> throw UnsupportedOperationException("Give app service type is not supported: $this")
    }
}

private fun AppServiceAppBase<*, *, *>.getIconProvider(): AzureIconProvider<AppServiceAppBase<*, *, *>>? {
    return when (this) {
        is WebApp -> WebAppNodeProvider.WEBAPP_ICON_PROVIDER
        is FunctionApp -> FunctionAppNodeProvider.FUNCTIONAPP_ICON_PROVIDER
        else -> null
    }
}