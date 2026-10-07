/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import com.intellij.execution.process.ProcessOutputType
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage

class RiderRunProcessHandlerMessager(private val handler: RunProcessHandler) : IntellijAzureMessager() {
    override fun show(message: IAzureMessage): Boolean {
        val raw = message.rawMessage
        when (raw.type) {
            IAzureMessage.Type.DEBUG -> handler.println(raw.message.toString(), ProcessOutputType.SYSTEM)
            IAzureMessage.Type.ERROR -> handler.println(raw.content, ProcessOutputType.STDERR)
            else -> handler.println(raw.message.toString(), ProcessOutputType.STDOUT)
        }

        return true
    }
}