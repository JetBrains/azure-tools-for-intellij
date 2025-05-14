package com.microsoft.azure.toolkit.intellij.devops.api

import com.intellij.collaboration.api.ServerPath
import com.intellij.util.withScheme
import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
class AzureDevOpsServerPath : ServerPath {

    var uri: String = ""
        private set

    constructor()

    constructor(uri: String) {
        require(uri.isNotEmpty())
        require(!uri.endsWith('/'))
        val validation = URI.create(uri)
        require(validation.scheme != null)
        require(validation.scheme.startsWith("http"))
        this.uri = uri
    }

    override fun toString(): String = uri

    override fun toURI(): URI = URI.create("$uri/")

    companion object {
        val DEFAULT_PATH : AzureDevOpsServerPath = AzureDevOpsServerPath("https://dev.azure.com")
    }

    fun toHttpsURI() : URI {
        val uri = toURI()
        return uri.withScheme(if (uri.scheme == "http") "https" else uri.scheme)
    }
}
