/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.base

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.eel.provider.asNioPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.provider.toEelApi
import com.intellij.util.net.ProxyAuthentication
import com.intellij.util.net.ProxyConfiguration.StaticProxyConfiguration
import com.intellij.util.net.ProxySettings
import com.intellij.util.net.ssl.CertificateManager
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.initialize
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager
import com.microsoft.azure.toolkit.ide.common.store.DefaultMachineStore
import com.microsoft.azure.toolkit.intellij.AzureToolkitConstants.AZURE_TOOLKIT_HOME_FOLDER
import com.microsoft.azure.toolkit.intellij.AzureToolkitConstants.AZURE_TOOLKIT_SETTINGS_FILE
import com.microsoft.azure.toolkit.intellij.appservice.PluginInitializationService
import com.microsoft.azure.toolkit.intellij.common.CommonConst
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureCloud
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager
import java.nio.file.Path
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.absolutePathString

class PluginLifecycleActivity : ProjectActivity {
    companion object {
        private val LOG = logger<PluginLifecycleActivity>()
    }

    override suspend fun execute(project: Project) {
        try {
            registerRxTaskManager()
            val azureJson = getFilePathForToolkitMachineStore(project).absolutePathString()
            AzureStoreManager.register(
                DefaultMachineStore(azureJson),
                IntellijStore.getInstance(),
                IntelliJSecureStore.getInstance()
            )
            initProxy()
            initializeConfig()
            restoreSignIn()
            PluginInitializationService.getInstance(project).setInitialized()
        } catch (t: Throwable) {
            LOG.error(t)
        }
    }

    private fun registerRxTaskManager() {
        try {
            AzureRxTaskManager.register()
        } catch (e: IllegalStateException) {
            LOG.warn("An exception during Rx task manager registration. Probably it is already registered", e)
        }
    }

    private suspend fun getFilePathForToolkitMachineStore(project: Project): Path {
        val eelApi = project.getEelDescriptor().toEelApi()
        val home = eelApi.userInfo.home.asNioPath()
        return home.resolve(AZURE_TOOLKIT_HOME_FOLDER).resolve(AZURE_TOOLKIT_SETTINGS_FILE)
    }

    private fun initProxy() {
        val proxySettings = ProxySettings.getInstance().getProxyConfiguration()
        if (proxySettings is StaticProxyConfiguration) {
            val proxyAuthentication = ProxyAuthentication.getInstance()
            val credentials = proxyAuthentication.getKnownAuthentication(proxySettings.host, proxySettings.port)
            if (credentials != null) {
                val proxy = ProxyInfo.builder()
                    .source("intellij")
                    .host(proxySettings.host)
                    .port(proxySettings.port)
                    .username(credentials.userName)
                    .password(credentials.password?.toString() ?: "")
                    .build()
                Azure.az().config().setProxyInfo(proxy)
                ProxyManager.getInstance().applyProxy()
            }
        }

        val certificateManager = CertificateManager.getInstance()
        Azure.az().config().sslContext = certificateManager.sslContext
        HttpsURLConnection.setDefaultSSLSocketFactory(certificateManager.sslContext.socketFactory)
    }

    private fun initializeConfig() {
        val installId = UUID.randomUUID().toString()
        initialize(installId, "Azure Toolkit for IntelliJ", CommonConst.PLUGIN_VERSION)
        val cloud = Azure.az().config().cloud
        if (cloud.isNotBlank()) {
            Azure.az(AzureCloud::class.java).setByName(cloud)
        }
    }

    private fun restoreSignIn() {
        val account = IdeAzureAccount.getInstance()
        if (account.isLoggedIn) return

        IdeAzureAccount.getInstance().restoreSignin()
    }
}
