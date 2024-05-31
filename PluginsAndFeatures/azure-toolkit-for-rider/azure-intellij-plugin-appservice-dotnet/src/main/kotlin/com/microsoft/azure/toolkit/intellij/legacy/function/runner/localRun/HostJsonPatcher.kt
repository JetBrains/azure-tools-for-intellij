/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.google.gson.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import java.nio.file.Path
import kotlin.io.path.*

@Service
class HostJsonPatcher {
    companion object {
        fun getInstance(): HostJsonPatcher = service()

        private const val FUNCTION_PROPERTY_NAME = "functions"

        private fun determineHostJsonFile(workingDirectory: String): Path? {
            val workingDirectoryPath = Path(workingDirectory)

            var path = workingDirectoryPath.resolve("host.json")
            if (path.exists()) return path

            path = workingDirectoryPath.resolve("../host.json")
            if (path.exists()) return path

            path = workingDirectoryPath.resolve("../../host.json")
            if (path.exists()) return path

            return null
        }

        private val LOG = logger<HostJsonPatcher>()
    }

    fun tryPatchHostJsonFile(workingDirectory: String, functionNames: String) = tryPatchHostJsonFile(
        determineHostJsonFile(workingDirectory),
        functionNames.split(',', ';', '|', ' ')
    )

    private fun tryPatchHostJsonFile(hostJsonFile: Path?, functionNames: List<String>) {
        if (hostJsonFile == null) {
            LOG.warn("Could not find host.json file to patch.")
            return
        }

        val functions = functionNames
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sortedBy { it }

        LOG.info("Patching " + hostJsonFile.absolutePathString() + " with function names: ${functions.joinToString(", ")}")
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val hostJson = gson.fromJson(hostJsonFile.readText(), JsonElement::class.java).asJsonObject

            val existingFunctionsArray = hostJson.getAsJsonArray(FUNCTION_PROPERTY_NAME)

            if (existingFunctionsArray == null) {
                if (functions.isEmpty()) return

                val functionArray = JsonArray()
                functions.forEach { functionArray.add(JsonPrimitive(it)) }
                hostJson.add(FUNCTION_PROPERTY_NAME, functionArray)
            } else {
                val existingFunctions = existingFunctionsArray.map { it.asString }.sortedBy { it }.toList()
                if (functions == existingFunctions) return

                if (functions.isNotEmpty()) {
                    val functionArray = JsonArray()
                    functions.forEach { functionArray.add(JsonPrimitive(it)) }
                    hostJson.add(FUNCTION_PROPERTY_NAME, functionArray)
                } else {
                    hostJson.remove(FUNCTION_PROPERTY_NAME)
                }
            }

            hostJsonFile.writeText(gson.toJson(hostJson))
        } catch (e: JsonParseException) {
            LOG.error(e)
        }
    }
}