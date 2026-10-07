package com.microsoft.azure.toolkit.intellij.debugger.relay

import com.intellij.openapi.diagnostic.logger
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import java.io.IOException

class SocketRelay(private val socket: Socket) : Relay {
    private val logger = logger<SocketRelay>()
    private val socketReadChannel: ByteReadChannel = socket.openReadChannel()
    private val socketWriteChannel: ByteWriteChannel = socket.openWriteChannel()

    override suspend fun process(receiveFrom: ReceiveChannel<ByteArray>, sendTo: SendChannel<ByteArray>) {
        try {
            withContext(Dispatchers.IO) {
                val receiveJob = launch { readFromSocket(sendTo) }
                val sendJob = launch { writeToSocket(receiveFrom) }

                receiveJob.invokeOnCompletion { sendJob.cancel() }
                sendJob.invokeOnCompletion { receiveJob.cancel() }
            }
        } catch (exception: Throwable) {
            when (exception) {
                is IOException -> logger.warn("SocketRelay connection problem", exception)
                else -> throw exception
            }
        } finally {
            closeSocket()
            logger.info("SocketRelay connection closed")
        }
    }

    private suspend fun readFromSocket(sendTo: SendChannel<ByteArray>) {
        val buffer = ByteArray(16384)

        while (!socketReadChannel.isClosedForRead) {
            val bytesRead = socketReadChannel.readAvailable(buffer)
            if (bytesRead > 0) {
                sendTo.send(buffer.copyOf(bytesRead))
            }
        }
    }

    private suspend fun writeToSocket(receiveFrom: ReceiveChannel<ByteArray>) {
        for (data in receiveFrom) {
            if (socketWriteChannel.isClosedForWrite) break

            socketWriteChannel.writeFully(data)
            socketWriteChannel.flush()
        }
    }

    private fun closeSocket() {
        socket.closeQuietly()
    }
}