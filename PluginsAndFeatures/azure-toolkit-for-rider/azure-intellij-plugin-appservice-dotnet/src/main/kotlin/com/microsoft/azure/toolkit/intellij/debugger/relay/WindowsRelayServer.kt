package com.microsoft.azure.toolkit.intellij.debugger.relay/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import com.intellij.remote.RemoteCredentialsHolder
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import io.ktor.client.request.*

class WindowsRelayServer(appServiceApp: AppServiceAppBase<*, *, *>) : RelayServer(appServiceApp) {
    override fun RemoteCredentialsHolder.customizeCredentials() {
        userName = appServiceApp.id
        password = appServiceApp.id
    }

    override fun HttpRequestBuilder.customizeWsRequest() {
        header("Agent-username", appServiceApp.id)
        header("Agent-password", appServiceApp.id)
    }
}