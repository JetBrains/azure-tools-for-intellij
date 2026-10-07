/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

internal data object BicepLS : LsInfrastructure {
    private const val URL_TEMPLATE = "https://github.com/Azure/bicep/releases/download/%s/bicep-langserver.zip"
    private const val RUNTIME_CONFIG_FILENAME = "Bicep.LangServer.runtimeconfig.json"

    override val formattedUrl: String
        get() {
            val supportedVersion = "v0.46.1"
            return URL_TEMPLATE.format(supportedVersion)
        }

    override val extractedDirectoryName: String
        get() = "bicep-langserver"

    override val executableName: String
        get() = "Bicep.LangServer.dll"

    override val presentableName: String
        get() = BicepBundle.message("progress.title.load.ls")

    override fun findExecutablePath(): Path {
        return PLUGIN_TMP_PATH.resolve(extractedDirectoryName).resolve(executableName)
    }

    override fun isValid(): Boolean {
        if (!isPresent()) return false

        val runtimeConfigPath = runtimeConfigPath()
        if (!runtimeConfigPath.exists()) return false

        val root = parseRuntimeConfig(runtimeConfigPath) ?: return false
        val runtimeOptionsObj = root["runtimeOptions"] as? JsonObject ?: return false
        val rollForwardValue = runtimeOptionsObj["rollForward"]

        return (rollForwardValue as? JsonPrimitive)?.content == "Major"
    }

    override fun patchInfrastructureFiles() {
        val runtimeConfigPath = runtimeConfigPath()
        try {
            if (!runtimeConfigPath.exists()) {
                createDefaultRuntimeConfig(runtimeConfigPath)
                return
            }

            val root = parseRuntimeConfig(runtimeConfigPath) ?: return
            val updated = addRollForwardIfMissing(root) ?: return
            writeRuntimeConfig(runtimeConfigPath, updated)
        } catch (_: Throwable) {
            // Intentionally ignore: patching should be best-effort and never break the flow
        }
    }

    private fun runtimeConfigPath(): Path =
        PLUGIN_TMP_PATH.resolve(extractedDirectoryName).resolve(RUNTIME_CONFIG_FILENAME)

    private fun createDefaultRuntimeConfig(path: Path) {
        val template = """
            {
              "runtimeOptions": {
                "tfm": "net8.0",
                "rollForward": "Major",
                "framework": {
                  "name": "Microsoft.NETCore.App",
                  "version": "8.0.0"
                },
                "configProperties": {
                  "System.Reflection.Metadata.MetadataUpdater.IsSupported": false,
                  "System.Runtime.Serialization.EnableUnsafeBinaryFormatterSerialization": false
                }
              }
            }
        """.trimIndent()
        Files.writeString(path, template, StandardCharsets.UTF_8)
    }

    private fun parseRuntimeConfig(path: Path): JsonObject? {
        val json = Json { prettyPrint = true }
        val content = Files.readString(path, StandardCharsets.UTF_8)
        val root = try {
            json.parseToJsonElement(content)
        } catch (_: Throwable) {
            return null
        }
        return root as? JsonObject
    }

    /**
     * Returns a new JsonObject with rollForward set to "Major" if it was missing, otherwise null.
     */
    private fun addRollForwardIfMissing(rootObj: JsonObject): JsonObject? {
        val runtimeOptionsObj = rootObj["runtimeOptions"] as? JsonObject
        val hasRollForward = runtimeOptionsObj?.containsKey("rollForward") == true
        if (hasRollForward) return null

        val newRuntimeOptions = buildJsonObject {
            if (runtimeOptionsObj != null) for ((k, v) in runtimeOptionsObj) put(k, v)
            put("rollForward", JsonPrimitive("Major"))
        }

        return buildJsonObject {
            for ((k, v) in rootObj) if (k != "runtimeOptions") put(k, v)
            put("runtimeOptions", newRuntimeOptions)
        }
    }

    private fun writeRuntimeConfig(path: Path, root: JsonObject) {
        val json = Json { prettyPrint = true }
        Files.writeString(path, json.encodeToString(JsonElement.serializer(), root), StandardCharsets.UTF_8)
    }
}