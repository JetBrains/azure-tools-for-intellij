@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension

@Service(Service.Level.APP)
internal class BicepInfrastructureLoader {
    companion object {
        fun getInstance(): BicepInfrastructureLoader = service<BicepInfrastructureLoader>()
        private val LOG = logger<BicepInfrastructureLoader>()
    }

    fun cleanTmpDirectorySafe(): Boolean {
        return runCatching {
            cleanTmpDirectory()
        }.onFailureNoPce { exception ->
            LOG.warn(
                "Unable to clean download directory `${BicepLS.extractedDirectoryName}`. Abort LS installation",
                exception
            )
        }.isSuccess
    }

    @OptIn(ExperimentalPathApi::class)
    private fun cleanTmpDirectory() {
        BicepInfrastructureService.getInstance().bicepTmpPath
            .resolve(BicepLS.extractedDirectoryName)
            .takeIf { it.exists() }
            ?.deleteRecursively()
    }

    suspend fun downloadLsInfrastructure(project: Project): Boolean {
        return withBackgroundProgress(project, BicepBundle.message("progress.title.load.infra")) {
            reportSequentialProgress { reporter ->
                if (BicepInfrastructureService.getInstance().isPresent()) return@reportSequentialProgress true

                val downloadedResource = reporter.nextStep(
                    endFraction = 70,
                    text = BicepBundle.message("progress.title.load", BicepLS.presentableName)
                ) {
                    downloadResourceSafe()
                } ?: return@reportSequentialProgress false

                LOG.info("Loaded ${BicepLS.presentableName} to `$downloadedResource`")

                val unpackedResource = reporter.nextStep(
                    endFraction = 90,
                    text = BicepBundle.message("progress.title.unpack", BicepLS.presentableName)
                ) {
                    unzipResourceSafe(downloadedResource)
                } ?: return@reportSequentialProgress false

                LOG.info("Unpacked ${BicepLS.presentableName} to `$unpackedResource`")

                reporter.nextStep(
                    endFraction = 100,
                    text = BicepBundle.message("progress.title.patch", BicepLS.presentableName)
                ) {
                    patchResourceSafe()
                }

                BicepInfrastructureService.getInstance().isPresent()
            }
        }
    }

    private fun downloadResourceSafe(): Path? = runCatching {
        downloadResource()
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to download ${BicepLS.presentableName}. Abort LS installation", exception)
    }.getOrNull()

    private fun downloadResource(): Path {
        val effectiveUrl = BicepLS.formattedUrl
        val targetFile = BicepInfrastructureService.getInstance().bicepTmpPath.resolve(effectiveUrl.substringAfterLast("/"))
        if (targetFile.exists()) return targetFile

        return HttpRequests.request(effectiveUrl)
            .connect { request ->
                LOG.warn("Started loading of ${BicepLS.presentableName}")
                val downloadedPath = request.saveToFile(targetFile, ProgressManager.getGlobalProgressIndicator())
                LOG.warn("Loaded ${BicepLS.presentableName} to `$downloadedPath`")
                downloadedPath
            }
    }

    private fun unzipResourceSafe(resourcePath: Path): Path? = runCatching {
        unzipResource(resourcePath)
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to extract ${BicepLS.presentableName} archive. Abort LS installation", exception)
        Files.delete(resourcePath)
    }.getOrNull()

    private fun unzipResource(resourcePath: Path): Path? {
        val decompressor = when (resourcePath.extension) {
            "zip" -> Decompressor.Zip(resourcePath)
            "gz", "tgz" -> Decompressor.Tar(resourcePath)
            else -> {
                LOG.warn(
                    "Downloaded resource '${BicepLS.presentableName}' has unexpected extension '${resourcePath.extension}'. " +
                            "Abort installation"
                )
                return null
            }
        }
        val target = BicepInfrastructureService.getInstance().bicepTmpPath.resolve(BicepLS.extractedDirectoryName)
        if (target.exists()) return target
        decompressor.extract(target)
        Files.delete(resourcePath)
        return target
    }

    private fun patchResourceSafe() = runCatching {
        patchInfrastructureFiles()
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to patch ${BicepLS.presentableName} archive", exception)
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    private fun patchInfrastructureFiles() {
        val runtimeConfigPath = BicepInfrastructureService.getInstance().runtimeConfigPath()
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
