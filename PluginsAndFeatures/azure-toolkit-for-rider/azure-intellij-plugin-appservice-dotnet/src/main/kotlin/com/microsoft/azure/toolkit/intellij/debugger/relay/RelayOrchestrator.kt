package com.microsoft.azure.toolkit.intellij.debugger.relay

import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class RelayOrchestrator(private val first: Relay, private val second: Relay) {
    private val firstToSecond = Channel<ByteArray>(Channel.BUFFERED)
    private val secondToFirst = Channel<ByteArray>(Channel.BUFFERED)

    suspend fun start() {
        try {
            withContext(Dispatchers.IO) {
                val firstJob = launch { first.process(firstToSecond, secondToFirst) }
                val secondJob = launch { second.process(secondToFirst, firstToSecond) }

                firstJob.invokeOnCompletion { secondJob.cancel() }
                secondJob.invokeOnCompletion { firstJob.cancel() }
            }
        } catch (e: Throwable) {
            when(e) {
                is CancellationException -> throw e
                is IOException -> logger<RelayOrchestrator>().warn(e)
                else -> logger<RelayOrchestrator>().error(e)
            }
        }
    }
}

