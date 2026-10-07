package com.microsoft.azure.toolkit.intellij.debugger.relay

import com.intellij.openapi.diagnostic.logger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class WebSocketRelay(private val requestProvider: HttpRequestBuilder.() -> Unit) : Relay {
    private val logger = logger<WebSocketRelay>()
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 30.seconds
        }
    }

    override suspend fun process(receiveFrom: ReceiveChannel<ByteArray>, sendTo: SendChannel<ByteArray>) {
        try {
            client.use { client ->
                client.webSocket(request = {
                    requestProvider()
                }) {
                    withContext(Dispatchers.IO) {
                        val receiveJob = launch { receiveFromWebSocket(sendTo) }
                        val sendJob = launch { sendToWebSocket(receiveFrom) }

                        receiveJob.invokeOnCompletion { sendJob.cancel() }
                        sendJob.invokeOnCompletion { receiveJob.cancel() }
                    }
                }
            }
        } catch (exception: Throwable) {
            when (exception) {
                is WebSocketException -> logger.warn("WebSocketRelay connection problem", exception)
                else -> throw exception
            }
        } finally {
            logger.info("WebSocketRelay connection closed")
        }
    }

    private suspend fun DefaultWebSocketSession.receiveFromWebSocket(sendTo: SendChannel<ByteArray>) {
        for (frame in incoming) {
            when (frame) {
                is Frame.Binary -> {
                    val data = frame.readBytes()
                    sendTo.send(data)
                }

                is Frame.Close -> {
                    logger.info("Close frame received. Shutting down.")
                    break
                }

                else -> {
                    logger.warn("Unexpected frame type received ${frame.frameType}. Ignoring.")
                }
            }
        }
    }

    private suspend fun DefaultWebSocketSession.sendToWebSocket(receiveFrom: ReceiveChannel<ByteArray>) {
        for (data in receiveFrom) {
            send(data)
        }
    }
}