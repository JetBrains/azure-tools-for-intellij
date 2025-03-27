/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.net.ssl.CertificateManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import kotlin.io.path.absolutePathString

@Service(Service.Level.APP)
class FunctionCoreToolsReleaseFeedService : Disposable {
    companion object {
        fun getInstance(): FunctionCoreToolsReleaseFeedService = service()
        private val LOG = logger<FunctionCoreToolsReleaseFeedService>()
    }

    private val client = HttpClient(CIO) {
        engine {
            https {
                trustManager = CertificateManager.getInstance().trustManager
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 300000
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    private val feedMutex = Mutex()

    suspend fun getReleaseFeed(feedUrl: String): ReleaseFeed? {
        feedMutex.withLock {
            try {
                val temporaryFeedFile = FileUtil.createTempFile(
                    File(FileUtil.getTempDirectory()),
                    "AzureFunctionsToolingFeed",
                    ".json",
                    true,
                    true
                )
                val temporaryFeedPath = temporaryFeedFile.toPath()

                LOG.trace("Created a temporary feed file: ${temporaryFeedPath.absolutePathString()}")

                withContext(Dispatchers.IO) {
                    client.prepareGet(feedUrl).execute { httpResponse ->
                        val channel: ByteReadChannel = httpResponse.body()
                        channel.copyAndClose(temporaryFeedFile.writeChannel())
                    }
                }

                LOG.trace("Downloaded Functions tooling feed to the ${temporaryFeedPath.absolutePathString()}")

                val feed = withContext(Dispatchers.IO) {
                    json.decodeFromStream<ReleaseFeed>(temporaryFeedFile.inputStream())
                }

                return feed
            } catch (e: Exception) {
                LOG.warn("Unable to download the Functions tooling release feed", e)
                return null
            }
        }
    }

    override fun dispose() = client.close()
}

@Serializable
data class ReleaseFeed(
    @SerialName("tags")
    val tags: Map<String, Tag>,
    @SerialName("releases")
    val releases: Map<String, Release>
)

@Serializable
data class Tag(
    @SerialName("release")
    val release: String?,
    @SerialName("releaseQuality")
    val releaseQuality: String?,
    @SerialName("hidden")
    val hidden: Boolean
)

@Serializable
data class Release(
    @SerialName("templates")
    val templates: String?,
    @SerialName("coreTools")
    val coreTools: List<ReleaseCoreTool>
)

@Serializable
data class ReleaseCoreTool(
    @SerialName("OS")
    val os: String?,
    @SerialName("Architecture")
    val architecture: String?,
    @SerialName("downloadLink")
    val downloadLink: String?,
    @SerialName("sha2")
    val sha2: String?,
    @SerialName("size")
    val size: String?,
    @SerialName("default")
    val default: Boolean
)