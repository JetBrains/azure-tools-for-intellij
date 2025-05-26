package com.microsoft.azure.toolkit.intellij.devops.api.data

import com.microsoft.azure.toolkit.intellij.devops.api.AzureDevOpsRestJsonDeserializerImpl
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

interface AzureDevOpsResponse<T> {
    fun getContent(): T
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureDevOpsSimpleResponse<T>(
    @JsonProperty("value") val value: T
) : AzureDevOpsResponse<T> {
    override fun getContent(): T = value
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureDevOpsListResponse<T>(
    @JsonProperty("value") val value: List<T>,
    @JsonProperty("count") val count: Int = 0
) : AzureDevOpsResponse<List<T>> {
    override fun getContent(): List<T> = value
}

inline fun <reified T> String.deserializeAzureDevOps(): T {
    return AzureDevOpsRestJsonDeserializerImpl.deserialize(this, T::class.java)
}

inline fun <reified T> String.deserializeAzureDevOpsList(): List<T> {
    return AzureDevOpsRestJsonDeserializerImpl.deserializeList(this, T::class.java)
}

inline fun <reified T, reified E> String.deserializeAzureDevOpsWrapped(): T {
    val typeFactory = AzureDevOpsRestJsonDeserializerImpl.getTypeFactory()
    val elementType = typeFactory.constructType(E::class.java)
    val wrapperType = typeFactory.constructParametricType(AzureDevOpsListResponse::class.java, elementType)

    return AzureDevOpsRestJsonDeserializerImpl.deserializeWrapped(
        this,
        wrapperType
    ) { response: AzureDevOpsListResponse<E> -> response.getContent() as T }
}
