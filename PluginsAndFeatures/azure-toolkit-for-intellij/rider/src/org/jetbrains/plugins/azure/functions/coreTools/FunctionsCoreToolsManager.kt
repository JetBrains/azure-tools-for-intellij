/**
 * Copyright (c) 2019-2022 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.coreTools

import com.intellij.database.util.common.isNotNullOrEmpty
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.ZipUtil
import com.intellij.util.system.CpuArch
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.rd.util.concurrentMapOf
import com.jetbrains.rdclient.util.idea.pumpMessages
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.time.Duration

object FunctionsCoreToolsManager {

    private val logger = Logger.getInstance(FunctionsCoreToolsManager::class.java)

    private val releasesCache = concurrentMapOf<String, FunctionsCoreToolsRelease>()

    fun demandCoreToolsPathForVersion(project: Project,
                                      azureFunctionsVersion: String,
                                      releaseFeedUrl: String,
                                      allowDownload: Boolean): String? {

        val downloadRoot = resolveDownloadRoot()

        // When download is allowed, see if we can resolve the path to a (newer) version
        if (allowDownload && Registry.`is`("azure.function_app.core_tools.feed.enabled")) {
            ensureReleasesCacheFromFeed(releaseFeedUrl)

            val coreToolsPath = resolveDownloadInfoForRelease(azureFunctionsVersion, downloadRoot)
                    ?.let { downloadInfo -> ensureReleaseDownloaded(project, downloadInfo) }
            if (coreToolsPath != null) {
                return coreToolsPath
            }
        }

        // Fallback to using an existing download
        val coreToolsPath = tryResolveExistingCoreToolsPath(azureFunctionsVersion, downloadRoot)
        if (coreToolsPath != null) {
            return coreToolsPath
        }

        return null
    }

    private fun ensureReleasesCacheFromFeed(releaseFeedUrl: String) {

        if (releasesCache.isNotEmpty()) return

        val azureCoreToolsFeedReleaseFilter =
                when {
                    SystemInfo.isWindows && CpuArch.isIntel64() -> AzureCoreToolsFeedReleaseFilter("Windows", listOf("x64"), listOf("minified", "full"))
                    SystemInfo.isWindows && CpuArch.isArm64() -> AzureCoreToolsFeedReleaseFilter("Windows", listOf("arm64", "x64"), listOf("minified", "full"))
                    SystemInfo.isWindows -> AzureCoreToolsFeedReleaseFilter("Windows", listOf("x86"), listOf("minified", "full"))
                    SystemInfo.isMac && CpuArch.isArm64() -> AzureCoreToolsFeedReleaseFilter("MacOS", listOf("arm64", "x64"), listOf("full"))
                    SystemInfo.isMac -> AzureCoreToolsFeedReleaseFilter("MacOS", listOf("x64"), listOf("full"))
                    SystemInfo.isLinux -> AzureCoreToolsFeedReleaseFilter("Linux", listOf("x64"), listOf("full"))
                    else -> AzureCoreToolsFeedReleaseFilter("Unknown", listOf("x64"), listOf("full"))
                }

        logger.debug("Azure Core Tools release filter: $azureCoreToolsFeedReleaseFilter")

        try {
            val functionsCoreToolsReleaseFeed = FunctionsCoreToolsReleaseFeedService.createInstance()
                    .getReleaseFeed(releaseFeedUrl)
                    .execute()
                    .body() ?: return

            val releaseTags = functionsCoreToolsReleaseFeed.tags
                    .toSortedMap()
                    .filterValues {
                        it.releaseQuality.isNotNullOrEmpty && it.release.isNotNullOrEmpty
                    }

            for ((releaseTagName, releaseTag) in releaseTags) {
                val release = functionsCoreToolsReleaseFeed.releases[releaseTag.release!!]
                        ?: continue

                val releaseCoreTools = release.coreTools
                        .filter {
                            // Match OS and ensure a download link is present
                            it.os.equals(azureCoreToolsFeedReleaseFilter.os, ignoreCase = true) &&
                                    it.downloadLink.isNotNullOrEmpty
                        }
                        .sortedWith(compareBy<ReleaseCoreTool> {
                            // Then match architecture. Items higher in the list have precedence.
                            // Use 9999 when no match is found.
                            azureCoreToolsFeedReleaseFilter.architectures.indexOfFirst { architecture ->
                                it.architecture.equals(architecture, ignoreCase = true)
                            }.let { rank -> if (rank >= 0) rank else 9999 }
                        }.thenBy {
                            // Then match size. Items higher in the list have precedence.
                            // Use 9999 when no match is found.
                            azureCoreToolsFeedReleaseFilter.sizes.indexOfFirst { size ->
                                it.size.equals(size, ignoreCase = true)
                            }.let { rank -> if (rank >= 0) rank else 9999 }
                        })
                        .firstOrNull() ?: continue

                logger.debug("Release for Azure Core Tools version ${releaseTagName.lowercase()}: ${releaseTag.release}; ${releaseCoreTools.downloadLink}")

                releasesCache.putIfAbsent(
                        releaseTagName.lowercase(),
                        FunctionsCoreToolsRelease(
                                releaseTagName.lowercase(),
                                releaseTag.release,
                                releaseCoreTools.downloadLink!!))
            }
        } catch (e: UnknownHostException) {
            logger.warn("Could not download from Azure Functions Core Tools release feed URL at $releaseFeedUrl: $e")
        } catch (e: IOException) {
            logger.warn("Could not download from Azure Functions Core Tools release feed URL at $releaseFeedUrl: $e")
        }
    }

    private fun resolveDownloadRoot(): File {
        val properties = PropertiesComponent.getInstance()

        // Ensure download root exists
        val downloadRoot = File(properties.getValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH,
                AzureRiderSettings.VALUE_FUNCTIONS_CORETOOLS_DOWNLOAD_PATH))

        try {
            downloadRoot.mkdir()
        } catch (e: Exception) {
            logger.error("Error while creating download root: ${downloadRoot.path}", e)
        }

        return downloadRoot
    }

    private fun resolveDownloadInfoForRelease(azureFunctionsVersion: String, downloadRoot: File): FunctionsCoreToolsDownloadInfo? {

        val releaseInfo = releasesCache[azureFunctionsVersion.lowercase()]
        if (releaseInfo == null) {
            logger.warn("Could not determine Azure Functions Core Tools release. Azure Functions version: '$azureFunctionsVersion'")
            return null
        }

        val downloadFolderForTag = downloadRoot.resolve(releaseInfo.functionsVersion)
        val downloadFolderForTagRelease = downloadFolderForTag.resolve(releaseInfo.coreToolsVersion)

        logger.debug("Found Azure Functions Core Tools release from feed. " +
                     "Azure Functions version: '${releaseInfo.functionsVersion}'; " +
                     "Core Tools Version: ${releaseInfo.coreToolsVersion}; " +
                     "Expected download path: ${downloadFolderForTagRelease.path}")

        return FunctionsCoreToolsDownloadInfo(
                downloadFolderForTag,
                downloadFolderForTagRelease,
                releaseInfo
        )
    }

    private fun ensureReleaseDownloaded(project: Project, downloadInfo: FunctionsCoreToolsDownloadInfo): String? {

        // Does the download path exist?
        if (downloadInfo.downloadFolderForTagAndRelease.exists()) {
            return downloadInfo.downloadFolderForTagAndRelease.path
        }

        // If not, download the release...
        var installed = false
        ProgressManager.getInstance().run(
                object : Task.Backgroundable(project, message("progress.function_app.core_tools.downloading"), true) {
                    override fun run(indicator: ProgressIndicator) {
                        downloadRelease(downloadInfo, indicator) {
                            installed = true
                        }
                    }
                })

        // REVIEW: What is reasonable amount of time to wait for tools download/expansion?
        pumpMessages(Duration.ofMinutes(15)) { installed }

        if (downloadInfo.downloadFolderForTagAndRelease.exists()) {
            return downloadInfo.downloadFolderForTagAndRelease.path
        }

        return null
    }

    private fun downloadRelease(
            functionsCoreToolsDownload: FunctionsCoreToolsDownloadInfo,
            indicator: ProgressIndicator,
            onComplete: () -> Unit) {

        if (!indicator.isRunning) indicator.start()

        indicator.isIndeterminate = true

        // Download latest artifact to temporary folder
        val tempFile = FileUtil.createTempFile(
                File(FileUtil.getTempDirectory()),
                "AzureFunctions-${functionsCoreToolsDownload.release.functionsVersion}-${functionsCoreToolsDownload.release.coreToolsVersion}",
                "download", true, true)

        // Download
        indicator.text = message("progress.function_app.core_tools.preparing_to_download")
        indicator.isIndeterminate = false
        HttpRequests.request(functionsCoreToolsDownload.release.coreToolsArtifactUrl)
                .productNameAsUserAgent()
                .connect {
                    indicator.text = message("progress.function_app.core_tools.downloading")
                    it.saveToFile(tempFile, indicator)
                }

        indicator.checkCanceled()

        // Extract
        ProgressManager.getInstance().executeNonCancelableSection {
            indicator.text = message("progress.function_app.core_tools.preparing_to_extract")
            indicator.isIndeterminate = true
            try {
                if (functionsCoreToolsDownload.downloadFolderForTag.exists()) {
                    functionsCoreToolsDownload.downloadFolderForTag.deleteRecursively()
                }
            } catch (e: Exception) {
                logger.error("Error while removing directory ${functionsCoreToolsDownload.downloadFolderForTag.path}", e)
            }

            indicator.text = message("progress.function_app.core_tools.extracting")
            indicator.isIndeterminate = true
            try {
                ZipUtil.extract(tempFile.toPath(), functionsCoreToolsDownload.downloadFolderForTagAndRelease.toPath(), null)
            } catch (e: Exception) {
                logger.error("Error while extracting ${tempFile.path} to ${functionsCoreToolsDownload.downloadFolderForTagAndRelease.path}", e)
            }

            indicator.text = message("progress.function_app.core_tools.cleaning_up_temporary_files")
            indicator.isIndeterminate = true
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (e: Exception) {
                logger.error("Error while removing temporary file ${tempFile.path}", e)
            }

            indicator.text = message("progress.common.finished")
        }

        if (indicator.isRunning)
            indicator.stop()

        onComplete()
    }

    private fun tryResolveExistingCoreToolsPath(azureFunctionsVersion: String, downloadRoot: File): String? {

        // Determine target folders for (potentially) cached path
        val downloadFolderForTag = downloadRoot.resolve(azureFunctionsVersion.lowercase())
        if (!downloadFolderForTag.exists()) return null

        val downloadFolderForTagRelease = downloadFolderForTag.listFiles(File::isDirectory)
                ?.sortedWith { first, second -> VersionComparatorUtil.compare(first?.name, second?.name) }
                ?.lastOrNull { it.exists() && it.listFiles { file ->
                    file.nameWithoutExtension.equals("func", ignoreCase = true) }?.any() == true }

        if (downloadFolderForTagRelease != null) {

            logger.debug("Found existing Azure Functions Core Tools path. " +
                    "Azure Functions version: '${azureFunctionsVersion.lowercase()}'; " +
                    "Download path: ${downloadFolderForTagRelease.path}")

            return downloadFolderForTagRelease.path
        }

        logger.warn("Could not determine existing Azure Functions Core Tools path. " +
                    "Azure Functions version: '${azureFunctionsVersion.lowercase()}'")

        return null
    }

    private class AzureCoreToolsFeedReleaseFilter(val os: String,
                                                  val architectures: List<String>,
                                                  val sizes: List<String>)

    private class FunctionsCoreToolsRelease(val functionsVersion: String,
                                            val coreToolsVersion: String,
                                            val coreToolsArtifactUrl: String)

    private class FunctionsCoreToolsDownloadInfo(val downloadFolderForTag: File,
                                                 val downloadFolderForTagAndRelease: File,
                                                 val release: FunctionsCoreToolsRelease)
}
