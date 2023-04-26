package com.microsoft.intellij

import com.intellij.util.net.HttpConfigurable
import com.intellij.util.net.ssl.CertificateManager
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager
import javax.net.ssl.HttpsURLConnection

object ProxyUtils {
    fun initProxy() {
        val instance = HttpConfigurable.getInstance()
        if (instance != null && instance.USE_HTTP_PROXY) {
            val proxy = ProxyInfo.builder()
                .source("intellij")
                .host(instance.PROXY_HOST)
                .port(instance.PROXY_PORT)
                .username(instance.proxyLogin)
                .password(instance.plainProxyPassword)
                .build()
            Azure.az().config().setProxyInfo(proxy)
            ProxyManager.getInstance().applyProxy()
        }

        setSslContext()
    }

    private fun setSslContext() {
        val certificateManager = CertificateManager.getInstance()
        Azure.az().config().sslContext = certificateManager.sslContext
        HttpsURLConnection.setDefaultSSLSocketFactory(certificateManager.sslContext.socketFactory)
    }
}