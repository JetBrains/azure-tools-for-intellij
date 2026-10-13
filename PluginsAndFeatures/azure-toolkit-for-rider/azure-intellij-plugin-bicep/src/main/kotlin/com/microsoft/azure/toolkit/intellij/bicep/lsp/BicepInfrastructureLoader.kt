@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.rethrowControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension

@Service(Service.Level.APP)
internal class BicepInfrastructureLoader {
    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Azure/bicep/releases/latest"

        fun getInstance(): BicepInfrastructureLoader = service<BicepInfrastructureLoader>()
        private val LOG = logger<BicepInfrastructureLoader>()
    }

    private val downloadMutex = Mutex()

    suspend fun findLatestVersion(): String? = withContext(Dispatchers.IO) {
        val response = HttpRequests.request(LATEST_RELEASE_URL)
            .accept("application/vnd.github+json")
            .readString()
        val release = Json.parseToJsonElement(response) as? JsonObject ?: return@withContext null
        (release["tag_name"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    }

    suspend fun downloadLatestLsInfrastructure(project: Project) {
        try {
            val version = findLatestVersion() ?: return
            downloadLsInfrastructure(project, BicepLS(version))
        } catch (exception: Exception) {
            rethrowControlFlowException(exception)
            LOG.warn("Unable to update Bicep language server infrastructure", exception)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    suspend fun downloadLsInfrastructure(project: Project, bicepLS: BicepLS): Boolean = downloadMutex.withLock {
        withBackgroundProgress(project, BicepBundle.message("progress.title.load.infra")) {
            reportSequentialProgress { reporter ->
                val infrastructureService = BicepInfrastructureService.getInstance(project)
                if (infrastructureService.isValid(bicepLS)) return@reportSequentialProgress true

                if (!infrastructureService.cleanServerDirectorySafe(bicepLS)) return@reportSequentialProgress false

                val downloadedResource = reporter.nextStep(
                    endFraction = 70,
                    text = BicepBundle.message("progress.title.load", bicepLS.presentableName)
                ) {
                    downloadResourceSafe(bicepLS, infrastructureService.downloadedArchivePath(bicepLS))
                } ?: return@reportSequentialProgress false

                LOG.info("Loaded ${bicepLS.presentableName} to `$downloadedResource`")

                val extractedDirectory = infrastructureService.extractedDirectoryPath(bicepLS)
                val stagingDirectory = extractedDirectory.resolveSibling(".bicep-${UUID.randomUUID()}")
                try {
                    val unpackedResource = reporter.nextStep(
                        endFraction = 90,
                        text = BicepBundle.message("progress.title.unpack", bicepLS.presentableName)
                    ) {
                        unzipResourceSafe(downloadedResource, bicepLS, stagingDirectory)
                    } ?: return@reportSequentialProgress false

                    LOG.info("Unpacked ${bicepLS.presentableName} to `$unpackedResource`")

                    val runtimeConfigPath = infrastructureService.runtimeConfigPath(stagingDirectory)
                    reporter.nextStep(
                        endFraction = 100,
                        text = BicepBundle.message("progress.title.patch", bicepLS.presentableName)
                    ) {
                        patchResourceSafe(bicepLS, runtimeConfigPath)
                    }

                    if (!infrastructureService.isValid(bicepLS, stagingDirectory)) return@reportSequentialProgress false

                    Files.move(stagingDirectory, extractedDirectory)
                    true
                } finally {
                    if (stagingDirectory.exists()) stagingDirectory.deleteRecursively()
                }
            }
        }
    }

    private fun downloadResourceSafe(bicepLS: BicepLS, targetFile: Path): Path? = runCatching {
        downloadResource(bicepLS, targetFile)
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to download ${bicepLS.presentableName}. Abort LS installation", exception)
    }.getOrNull()

    private fun downloadResource(bicepLS: BicepLS, targetFile: Path): Path {
        if (targetFile.exists()) return targetFile

        return HttpRequests.request(bicepLS.formattedUrl)
            .connect { request ->
                LOG.warn("Started loading of ${bicepLS.presentableName}")
                val downloadedPath = request.saveToFile(targetFile, ProgressManager.getGlobalProgressIndicator())
                LOG.warn("Loaded ${bicepLS.presentableName} to `$downloadedPath`")
                downloadedPath
            }
    }

    private fun unzipResourceSafe(resourcePath: Path, bicepLS: BicepLS, target: Path): Path? = runCatching {
        unzipResource(resourcePath, bicepLS, target)
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to extract ${bicepLS.presentableName} archive. Abort LS installation", exception)
        Files.delete(resourcePath)
    }.getOrNull()

    private fun unzipResource(resourcePath: Path, bicepLS: BicepLS, target: Path): Path? {
        val decompressor = when (resourcePath.extension) {
            "zip" -> Decompressor.Zip(resourcePath)
            "gz", "tgz" -> Decompressor.Tar(resourcePath)
            else -> {
                LOG.warn(
                    "Downloaded resource '${bicepLS.presentableName}' has unexpected extension '${resourcePath.extension}'. " +
                            "Abort installation"
                )
                return null
            }
        }
        if (target.exists()) return target
        decompressor.extract(target)
        Files.delete(resourcePath)
        return target
    }

    private fun patchResourceSafe(bicepLS: BicepLS, runtimeConfigPath: Path) = runCatching {
        patchInfrastructureFiles(runtimeConfigPath)
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to patch ${bicepLS.presentableName} archive", exception)
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    private fun patchInfrastructureFiles(runtimeConfigPath: Path) {
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

    private fun <T> Result<T>.onFailureNoPce(action: (exception: Throwable) -> Unit): Result<T> {
        val wrappedAction = { exception: Throwable ->
            if (exception is ProcessCanceledException)
                throw exception
            else
                action(exception)
        }
        return this.onFailure(wrappedAction)
    }
}
