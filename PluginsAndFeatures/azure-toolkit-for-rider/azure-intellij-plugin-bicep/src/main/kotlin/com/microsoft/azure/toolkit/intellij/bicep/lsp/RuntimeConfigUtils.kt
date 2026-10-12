/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal fun parseRuntimeConfig(path: Path): JsonObject? {
    val json = Json { prettyPrint = true }
    val content = Files.readString(path, StandardCharsets.UTF_8)
    val root = try {
        json.parseToJsonElement(content)
    } catch (_: Throwable) {
        return null
    }
    return root as? JsonObject
}

internal fun writeRuntimeConfig(path: Path, root: JsonObject) {
    val json = Json { prettyPrint = true }
    Files.writeString(path, json.encodeToString(JsonElement.serializer(), root), StandardCharsets.UTF_8)
}
