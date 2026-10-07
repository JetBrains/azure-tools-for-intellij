/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.storage

data class StorageAccountConfig(
    val subscriptionId: String,
    val name: String
)