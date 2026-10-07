/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class StorageDisposable : Disposable {
    companion object {
        fun getInstance() = service<StorageDisposable>()
    }

    override fun dispose() {
    }
}