/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.intellij.util.text.VersionComparatorUtil
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@Service(Service.Level.APP)
internal class BicepInfrastructureService {
    companion object {
        private const val BICEP_DIRECTORY_NAME = "bicep"
        private const val RUNTIME_CONFIG_FILENAME = "Bicep.LangServer.runtimeconfig.json"

        fun getInstance(): BicepInfrastructureService = service<BicepInfrastructureService>()
        private val LOG = logger<BicepInfrastructureService>()
    }

    private fun serverDirectoryPath(bicepLS: BicepLS): Path =
        Path.of(PathManager.getSystemPath())
            .resolve(BICEP_DIRECTORY_NAME)
            .resolve(bicepLS.version)

    fun cleanServerDirectorySafe(bicepLS: BicepLS): Boolean {
        return runCatching {
            cleanServerDirectory(bicepLS)
        }.onFailure { exception ->
            if (exception is ProcessCanceledException) throw exception
            LOG.warn(
                "Unable to clean download directory `${bicepLS.extractedDirectoryName}`. Abort LS installation",
                exception
            )
        }.isSuccess
    }

    @OptIn(ExperimentalPathApi::class)
    private fun cleanServerDirectory(bicepLS: BicepLS) {
        extractedDirectoryPath(bicepLS)
            .takeIf { it.exists() }
            ?.deleteRecursively()
    }

    fun downloadedArchivePath(bicepLS: BicepLS): Path =
        serverDirectoryPath(bicepLS)
            .resolve(bicepLS.formattedUrl.substringAfterLast("/"))

    fun extractedDirectoryPath(bicepLS: BicepLS): Path =
        serverDirectoryPath(bicepLS)
            .resolve(bicepLS.extractedDirectoryName)

    fun runtimeConfigPath(bicepLS: BicepLS): Path =
        extractedDirectoryPath(bicepLS)
            .resolve(RUNTIME_CONFIG_FILENAME)

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun findLatestVersion(): String? {
        val serversDirectory = Path.of(PathManager.getSystemPath()).resolve(BICEP_DIRECTORY_NAME)
        if (!serversDirectory.isDirectory()) {
            return null
        }

        return serversDirectory.listDirectoryEntries()
            .filter { it.isDirectory() }
            .map { it.name }
            .maxWithOrNull { first, second -> VersionComparatorUtil.compare(first, second) }
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun findExecutablePath(bicepLS: BicepLS): Path {
        return extractedDirectoryPath(bicepLS)
            .resolve(bicepLS.executableName)
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isPresent(bicepLS: BicepLS): Boolean {
        return findExecutablePath(bicepLS).exists()
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isValid(bicepLS: BicepLS): Boolean {
        if (!isPresent(bicepLS)) return false

        val runtimeConfigPath = runtimeConfigPath(bicepLS)
        if (!runtimeConfigPath.exists()) return false

        val root = parseRuntimeConfig(runtimeConfigPath) ?: return false
        val runtimeOptionsObj = root["runtimeOptions"] as? JsonObject ?: return false
        val rollForwardValue = runtimeOptionsObj["rollForward"]

        return (rollForwardValue as? JsonPrimitive)?.content == "Major"
    }
}
