/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Path
import kotlin.io.path.exists

@Service(Service.Level.APP)
internal class BicepInfrastructureService {
    companion object {
        private const val RUNTIME_CONFIG_FILENAME = "Bicep.LangServer.runtimeconfig.json"

        fun getInstance(): BicepInfrastructureService = service<BicepInfrastructureService>()
    }

    val bicepTmpPath: Path = Path.of(PathManager.getSystemPath()).resolve("bicep")

    fun runtimeConfigPath(): Path = bicepTmpPath
        .resolve(BicepLS.extractedDirectoryName)
        .resolve(RUNTIME_CONFIG_FILENAME)

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun findExecutablePath(): Path {
        return bicepTmpPath
            .resolve(BicepLS.extractedDirectoryName)
            .resolve(BicepLS.executableName)
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isPresent(): Boolean {
        return findExecutablePath().exists()
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isValid(): Boolean {
        if (!isPresent()) return false

        val runtimeConfigPath = runtimeConfigPath()
        if (!runtimeConfigPath.exists()) return false

        val root = parseRuntimeConfig(runtimeConfigPath) ?: return false
        val runtimeOptionsObj = root["runtimeOptions"] as? JsonObject ?: return false
        val rollForwardValue = runtimeOptionsObj["rollForward"]

        return (rollForwardValue as? JsonPrimitive)?.content == "Major"
    }
}
