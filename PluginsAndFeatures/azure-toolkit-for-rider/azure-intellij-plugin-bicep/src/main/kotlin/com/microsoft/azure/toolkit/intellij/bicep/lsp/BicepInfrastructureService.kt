/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.provider.asNioPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.provider.toEelApi
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.intellij.util.text.VersionComparatorUtil
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@Service(Service.Level.PROJECT)
internal class BicepInfrastructureService(private val project: Project) {
    companion object {
        private const val AZURE_TOOLS_DIRECTORY_NAME = ".azure-tools-for-rider"
        private const val BICEP_DIRECTORY_NAME = "bicep"
        private const val RUNTIME_CONFIG_FILENAME = "Bicep.LangServer.runtimeconfig.json"
        private const val MAX_RETAINED_SERVER_VERSIONS = 5

        fun getInstance(project: Project): BicepInfrastructureService = project.service<BicepInfrastructureService>()
        private val LOG = logger<BicepInfrastructureService>()
    }

    private suspend fun serversDirectoryPath(): Path {
        val eelApi = project.getEelDescriptor().toEelApi()
        return eelApi.userInfo.home.asNioPath()
            .resolve(AZURE_TOOLS_DIRECTORY_NAME)
            .resolve(BICEP_DIRECTORY_NAME)
    }

    private suspend fun serverDirectoryPath(bicepLS: BicepLS): Path =
        serversDirectoryPath()
            .resolve(bicepLS.version)

    suspend fun cleanServerDirectorySafe(bicepLS: BicepLS): Boolean {
        return runCatching {
            cleanServerDirectory(bicepLS)
        }.onFailure { exception ->
            rethrowControlFlowException(exception)
            LOG.warn(
                "Unable to clean download directory `${bicepLS.extractedDirectoryName}`. Abort LS installation",
                exception
            )
        }.isSuccess
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun cleanServerDirectory(bicepLS: BicepLS) {
        extractedDirectoryPath(bicepLS)
            .takeIf { it.exists() }
            ?.deleteRecursively()

        val serversDirectory = serversDirectoryPath()
        if (!serversDirectory.isDirectory()) return

        serversDirectory.listDirectoryEntries()
            .filter { it.isDirectory(LinkOption.NOFOLLOW_LINKS) && it.name != bicepLS.version }
            .sortedWith { first, second -> VersionComparatorUtil.compare(second.name, first.name) }
            .drop(MAX_RETAINED_SERVER_VERSIONS - 1)
            .forEach { it.deleteRecursively() }
    }

    suspend fun downloadedArchivePath(bicepLS: BicepLS): Path =
        serverDirectoryPath(bicepLS)
            .resolve(bicepLS.formattedUrl.substringAfterLast("/"))

    suspend fun extractedDirectoryPath(bicepLS: BicepLS): Path =
        serverDirectoryPath(bicepLS)
            .resolve(bicepLS.extractedDirectoryName)

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    suspend fun findLatestVersion(): String? {
        val serversDirectory = serversDirectoryPath()
        if (!serversDirectory.isDirectory()) {
            return null
        }

        return serversDirectory.listDirectoryEntries()
            .filter { it.isDirectory() && isValid(BicepLS(it.name)) }
            .map { it.name }
            .maxWithOrNull { first, second -> VersionComparatorUtil.compare(first, second) }
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    suspend fun runtimeConfigPath(bicepLS: BicepLS): Path =
        runtimeConfigPath(extractedDirectoryPath(bicepLS))

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun runtimeConfigPath(extractedDirectory: Path): Path =
        extractedDirectory.resolve(RUNTIME_CONFIG_FILENAME)

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    suspend fun findExecutablePath(bicepLS: BicepLS): Path {
        return findExecutablePath(bicepLS, extractedDirectoryPath(bicepLS))
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun findExecutablePath(bicepLS: BicepLS, extractedDirectory: Path): Path {
        return extractedDirectory.resolve(bicepLS.executableName)
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    suspend fun isValid(bicepLS: BicepLS): Boolean {
        return isValid(bicepLS, extractedDirectoryPath(bicepLS))
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isValid(bicepLS: BicepLS, extractedDirectory: Path): Boolean {
        val executable = findExecutablePath(bicepLS, extractedDirectory)
        if (!executable.isRegularFile()) return false

        val runtimeConfigPath = runtimeConfigPath(extractedDirectory)
        if (!runtimeConfigPath.isRegularFile()) return false

        val root = parseRuntimeConfig(runtimeConfigPath) ?: return false
        val runtimeOptionsObj = root["runtimeOptions"] as? JsonObject ?: return false
        val rollForwardValue = runtimeOptionsObj["rollForward"]

        return (rollForwardValue as? JsonPrimitive)?.content == "Major"
    }
}
