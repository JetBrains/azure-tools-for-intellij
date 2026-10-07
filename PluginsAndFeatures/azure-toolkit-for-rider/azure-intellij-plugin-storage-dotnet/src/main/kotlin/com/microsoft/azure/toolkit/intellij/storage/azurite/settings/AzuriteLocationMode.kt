/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.settings

enum class AzuriteLocationMode(val description: String) {
    Managed("Managed by Rider"),
    Project("Store data in current solution folder"),
    Custom("Use custom location")
}