/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

private const val REACTOR_NETTY_LOGGER = "reactor.netty"
private const val REACTOR_NETTY_HTTP_CLIENT_LOGGER = "reactor.netty.http.client"
private const val MAX_CAUSE_DEPTH = 20
private val REMOTE_HOST_REGEX = Regex("""R:([^/\s\]]+)""")

/**
 * Runs [block] and reports HTTP connection errors (e.g., response timeouts) observed by the Azure SDK HTTP client
 * for the application with the given [appHostName] to the [messenger].
 *
 * Such errors are usually retried by the Azure SDK internally and are only written to the IDE log,
 * so the user doesn't see why the deployment takes so long.
 */
suspend fun <T> withHttpConnectionErrorReporting(
    appHostName: String?,
    messenger: IAzureMessager?,
    block: suspend () -> T
): T {
    if (appHostName.isNullOrBlank() || messenger == null) return block()

    val hostPrefix = appHostName.substringBefore('.')
    // Keep a strong reference to the logger while the handler is registered: JUL loggers are weakly referenced
    val logger = Logger.getLogger(REACTOR_NETTY_LOGGER)
    val handler = HttpConnectionErrorHandler(hostPrefix, messenger)
    logger.addHandler(handler)
    try {
        return block()
    } finally {
        logger.removeHandler(handler)
    }
}

private class HttpConnectionErrorHandler(
    private val hostPrefix: String,
    private val messenger: IAzureMessager
) : Handler() {
    private val formatter = SimpleFormatter()

    override fun publish(record: LogRecord?) {
        if (record == null) return
        runCatching {
            if (record.level.intValue() < Level.WARNING.intValue()) return
            val thrown = record.thrown ?: return
            if (record.loggerName?.startsWith(REACTOR_NETTY_HTTP_CLIENT_LOGGER) != true) return

            val message = formatter.formatMessage(record) ?: return
            val host = REMOTE_HOST_REGEX.find(message)?.groupValues?.get(1) ?: return
            if (!host.startsWith("$hostPrefix.", ignoreCase = true)) return

            val cause = generateSequence(thrown) { it.cause }.take(MAX_CAUSE_DEPTH).last()
            messenger.warning("Connection to $host failed: ${cause.javaClass.simpleName}: ${cause.message}. The request may be retried.")
        }
    }

    override fun flush() {
    }

    override fun close() {
    }
}
