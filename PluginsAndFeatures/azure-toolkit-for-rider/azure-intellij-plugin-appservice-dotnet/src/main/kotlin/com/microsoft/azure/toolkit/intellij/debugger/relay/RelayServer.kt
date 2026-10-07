package com.microsoft.azure.toolkit.intellij.debugger.relay

import com.azure.core.credential.TokenRequestContext
import com.azure.identity.implementation.util.ScopeUtil
import com.intellij.remote.RemoteCredentials
import com.intellij.remote.RemoteCredentialsHolder
import com.intellij.remote.SshConnectionConfigPatch
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import okhttp3.internal.closeQuietly

abstract class RelayServer(protected val appServiceApp: AppServiceAppBase<*, *, *>) {
    private val socketServer = SocketServer("localhost")
    private val wsRequestProvider: HttpRequestBuilder.() -> Unit = {
        url.takeFrom("wss://${appServiceApp.kuduHostName}/AppServiceTunnel/Tunnel.ashx")
        bearerAuth(getCredential(appServiceApp))
        header("Cache-Control", "no-cache")
        customizeWsRequest()
    }

    suspend fun start() = supervisorScope {
        try {
            while (true) {
                val socket = socketServer.acceptConnection()
                launch {
                    RelayOrchestrator(WebSocketRelay(wsRequestProvider), SocketRelay(socket)).start()
                }
            }
        } finally {
            socketServer.close()
        }
    }

    val remoteCredentials: RemoteCredentials by lazy {
        RemoteCredentialsHolder().apply {
            port = socketServer.port
            connectionConfigPatch = SshConnectionConfigPatch().apply {
                hostKeyVerifier = SshConnectionConfigPatch.HostKeyVerifier().apply {
                    strictHostKeyChecking = SshConnectionConfigPatch.HostKeyVerifier.StrictHostKeyChecking.NO
                }
            }

            customizeCredentials()
        }
    }

    protected abstract fun RemoteCredentialsHolder.customizeCredentials()
    protected open fun HttpRequestBuilder.customizeWsRequest() {}

    private fun getCredential(app: AppServiceAppBase<*, *, *>): String {
        val account = Azure.az(AzureAccount::class.java).account()
        val scopes = ScopeUtil.resourceToScopes(account.environment.managementEndpoint)
        val request = TokenRequestContext().addScopes(*scopes)
        val token = account.getTokenCredential(app.subscriptionId).getToken(request).block()
        return token?.token ?: ""
    }

    protected inner class SocketServer(hostName: String, port: Int = 0) {
        private val selectorManager = ActorSelectorManager(Dispatchers.IO)
        private val serverSocket: ServerSocket = runBlocking {
             aSocket(selectorManager).tcp().bind(InetSocketAddress(hostName, port))
        }

        val port = (serverSocket.localAddress as InetSocketAddress).port

        suspend fun acceptConnection() = serverSocket.accept()
        fun close() = serverSocket.closeQuietly()
    }
}

