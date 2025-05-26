package com.microsoft.azure.toolkit.intellij.devops.api

import com.intellij.collaboration.api.HttpApiHelper
import com.intellij.collaboration.api.httpclient.CompoundRequestConfigurer
import com.intellij.collaboration.api.httpclient.HttpClientUtil
import com.intellij.collaboration.api.httpclient.HttpRequestConfigurer
import com.intellij.collaboration.api.httpclient.RequestTimeoutConfigurer
import com.intellij.openapi.diagnostic.logger
import com.microsoft.azure.toolkit.intellij.devops.api.data.deserializeAzureDevOpsWrapped
import org.jetbrains.annotations.ApiStatus
import java.net.http.HttpRequest
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.collaboration.auth.AccountDetails

class AzureDevOpsApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

private object AzureDevOpsApiVersions {
    const val CORE_API_VERSION = "7.1-preview"
}

val AzureDevOpsServerPath.apiBaseUrl: String
    get() = uri

@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureDevOpsRepository(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("defaultBranch") val defaultBranch: String = "",
    @JsonProperty("remoteUrl") val remoteUrl: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureDevOpsUser(
    @JsonProperty("accountName") val accountName: String = "",
) : AccountDetails {
    override val name: String get() = accountName
    override val avatarUrl: String? get() = null
}

@ApiStatus.Experimental
sealed interface AzureDevOpsApi : HttpApiHelper {
    val server: AzureDevOpsServerPath

    fun getCurrentUser(): AzureDevOpsUser

    fun getRepositories(): List<AzureDevOpsRepository>
}

internal class AzureDevOpsApiImpl(
    override val server: AzureDevOpsServerPath,
    httpHelper: HttpApiHelper
) : AzureDevOpsApi, HttpApiHelper by httpHelper {

    constructor(
        server: AzureDevOpsServerPath,
        tokenSupplier: (() -> String)? = null
    ) : this(
        server,
        tokenSupplier?.let { httpHelper(it) } ?: httpHelper()
    )

    override fun getCurrentUser(): AzureDevOpsUser {
        val organization = server.uri.substringAfterLast("/")
        val uri = "${server.uri}/_apis/projects?api-version=${AzureDevOpsApiVersions.CORE_API_VERSION}"
        logger<AzureDevOpsApi>().info("Validating token for organization by querying projects from $uri")
        val request = request(uri).GET().build()

        try {
            val client = java.net.http.HttpClient.newBuilder().build()
            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw AzureDevOpsApiException("Failed to validate token: HTTP ${response.statusCode()}")
            }

            // !!! Temporary: as we don't have user info from this API call, create a user with organization name
            return AzureDevOpsUser(
                accountName = organization,
            )
        } catch (e: Exception) {
            if (e is AzureDevOpsApiException) throw e
            logger<AzureDevOpsApi>().warn("Failed to validate token: ${e.message}")
            throw AzureDevOpsApiException("Failed to validate token: ${e.message}", e)
        }
    }

    override fun getRepositories(): List<AzureDevOpsRepository> {
        val uri = "${server.uri}/_apis/git/repositories?api-version=${AzureDevOpsApiVersions.CORE_API_VERSION}"
        logger<AzureDevOpsApi>().info("Getting repositories from $uri")
        val request = request(uri).GET().build()

        try {
            val client = java.net.http.HttpClient.newBuilder().build()
            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw AzureDevOpsApiException("Failed to get repositories: HTTP ${response.statusCode()}")
            }

            val responseBody = response.body()
            if (responseBody.isEmpty()) {
                throw AzureDevOpsApiException("Empty response body received for $request")
            }

            return try {
                responseBody.deserializeAzureDevOpsWrapped<List<AzureDevOpsRepository>, AzureDevOpsRepository>()
            } catch (e: Exception) {
                logger<AzureDevOpsApi>().warn("Failed to parse response: ${e.message}")
                throw AzureDevOpsApiException("Failed to parse response: ${e.message}", e)
            }
        } catch (e: Exception) {
            if (e is AzureDevOpsApiException) throw e
            logger<AzureDevOpsApi>().warn("Failed to get repositories: ${e.message}")
            throw AzureDevOpsApiException("Failed to get repositories: ${e.message}", e)
        }
    }

}

private fun httpHelper(tokenSupplier: () -> String): HttpApiHelper {
    val requestConfigurer = CompoundRequestConfigurer(
        RequestTimeoutConfigurer(),
        AzureDevOpsHeadersConfigurer(tokenSupplier)
    )
    return HttpApiHelper(
        logger = logger<AzureDevOpsApi>(),
        requestConfigurer = requestConfigurer
    )
}

private fun httpHelper(): HttpApiHelper {
    val requestConfigurer = CompoundRequestConfigurer(
        RequestTimeoutConfigurer(),
        AzureDevOpsHeadersConfigurer()
    )
    return HttpApiHelper(
        logger = logger<AzureDevOpsApi>(),
        requestConfigurer = requestConfigurer
    )
}


private const val PLUGIN_USER_AGENT_NAME = "IntelliJ-AzureDevOps-Plugin"

private class AzureDevOpsHeadersConfigurer(private val tokenSupplier: (() -> String)? = null) : HttpRequestConfigurer {
    override fun configure(builder: HttpRequest.Builder): HttpRequest.Builder = builder.apply {
        header(HttpClientUtil.USER_AGENT_HEADER, HttpClientUtil.getUserAgentValue(PLUGIN_USER_AGENT_NAME))

        tokenSupplier?.let { supplier ->
            val token = supplier()
            header("Authorization", "Bearer $token")
        }
    }
}
