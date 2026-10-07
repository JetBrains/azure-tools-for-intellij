@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.coroutines.mapConcurrent
import com.intellij.platform.util.progress.reportProgressScope
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension


internal fun cleanTmpDirectorySafe(infrastructurePiece: LsInfrastructure): Boolean {
    return runCatching {
        cleanTmpDirectory(infrastructurePiece)
    }.onFailureNoPce { exception ->
        LOG.warn(
            "Unable to clean download directory `${infrastructurePiece.extractedDirectoryName}`. Abort LS installation",
            exception
        )
    }.isSuccess
}

@OptIn(ExperimentalPathApi::class)
private fun cleanTmpDirectory(infrastructurePiece: LsInfrastructure) {
    PLUGIN_TMP_PATH.resolve(infrastructurePiece.extractedDirectoryName)
        .takeIf { it.exists() }
        ?.deleteRecursively()
}

internal suspend fun downloadLsInfrastructure(project: Project): Boolean {
    return withBackgroundProgress(project, BicepBundle.message("progress.title.load.infra")) {
        reportSequentialProgress { reporter ->
            val allNecessaryResources = LsInfrastructure.allKnown()
                .filterNot(LsInfrastructure::isPresent)
                .toList()

            val downloadedResources = reporter.nextStep(endFraction = 70) {
                reportProgressScope(allNecessaryResources.size) { innerReporter ->
                    allNecessaryResources
                        .mapConcurrent { infrastructurePiece ->
                            innerReporter.itemStep(
                                BicepBundle.message(
                                    "progress.title.load",
                                    infrastructurePiece.presentableName
                                )
                            ) {
                                downloadResourceSafe(infrastructurePiece)
                            }
                        }
                        .filterNotNull()
                }
            }

            LOG.info(
                "Loaded ${downloadedResources.size} of ${allNecessaryResources.size} necessary resources: " +
                        downloadedResources.joinToString(separator = ";")
            )

            val unpackedResources = reporter.nextStep(90) {
                reportProgressScope(downloadedResources.size) { innerReporter ->
                    downloadedResources.mapConcurrent { (lsInfrastructure, path) ->
                        innerReporter.itemStep(
                            BicepBundle.message(
                                "progress.title.unpack",
                                lsInfrastructure.presentableName
                            )
                        ) {
                            unzipResourceSafe(path, lsInfrastructure)
                        }
                    }
                        .filterNotNull()
                }
            }

            LOG.info(
                "Unpacked ${unpackedResources.size} of ${downloadedResources.size} loaded resources: " +
                        unpackedResources.joinToString(separator = ";")
            )

            reporter.nextStep(100) {
                reportProgressScope(unpackedResources.size) { innerReporter ->
                    unpackedResources.mapConcurrent { (lsInfrastructure, _) ->
                        innerReporter.itemStep(
                            BicepBundle.message(
                                "progress.title.patch",
                                lsInfrastructure.presentableName
                            )
                        ) {
                            patchResourceSafe(lsInfrastructure)
                        }
                    }
                }
            }

            allNecessaryResources.all(LsInfrastructure::isPresent)
        }
    }
}

internal fun downloadResourceSafe(
    infrastructurePiece: LsInfrastructure
): Pair<LsInfrastructure, Path>? {
    val downloadedResource = runCatching {
        downloadResource(infrastructurePiece)
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to download ${infrastructurePiece.presentableName}. Abort LS installation", exception)
    }.getOrNull() ?: return null

    return infrastructurePiece to downloadedResource
}

private fun downloadResource(
    infrastructurePiece: LsInfrastructure
): Path? {
    val effectiveUrl = infrastructurePiece.formattedUrl
    if (effectiveUrl == null) {
        LOG.warn("Unable to prepare effective url for ${infrastructurePiece::class.simpleName} and current OS")
        return null
    }
    val targetFile = PLUGIN_TMP_PATH.resolve(effectiveUrl.substringAfterLast("/"))
    if (targetFile.exists()) return targetFile

    return HttpRequests.request(effectiveUrl)
        .connect { request ->
            LOG.warn("Started loading of ${infrastructurePiece.presentableName}")
            val downloadedPath = request.saveToFile(targetFile, ProgressManager.getGlobalProgressIndicator())
            LOG.warn("Loaded ${infrastructurePiece.presentableName} to `$downloadedPath`")
            downloadedPath
        }
}

internal fun unzipResourceSafe(
    resourcePath: Path,
    infrastructurePiece: LsInfrastructure
): Pair<LsInfrastructure, Path>? {
    val unpackedResource = runCatching {
        unzipResource(resourcePath, infrastructurePiece)
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to extract ${infrastructurePiece.presentableName} archive. Abort LS installation", exception)
        Files.delete(resourcePath)
    }.getOrNull() ?: return null

    return infrastructurePiece to unpackedResource
}

private fun unzipResource(resourcePath: Path, infrastructurePiece: LsInfrastructure): Path? {
    val decompressor = when (resourcePath.extension) {
        "zip" -> Decompressor.Zip(resourcePath)
        "gz", "tgz" -> Decompressor.Tar(resourcePath)
        else -> {
            LOG.warn(
                "Downloaded resource '${infrastructurePiece.presentableName}' has unexpected extension '${resourcePath.extension}'. " +
                        "Abort installation"
            )
            return null
        }
    }
    val target = PLUGIN_TMP_PATH.resolve(infrastructurePiece.extractedDirectoryName)
    if (target.exists()) return target
    decompressor.extract(target)
    Files.delete(resourcePath)
    return target
}

internal fun patchResourceSafe(infrastructurePiece: LsInfrastructure) {
    runCatching {
        infrastructurePiece.patchInfrastructureFiles()
    }.onFailureNoPce { exception ->
        LOG.warn("Unable to patch ${infrastructurePiece.presentableName} archive", exception)
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


internal val PLUGIN_TMP_PATH = Path.of(PathManager.getSystemPath()).resolve("bicep")

private val LOG
    get() = Logger.getInstance("bicepInfrastructureLoader")
