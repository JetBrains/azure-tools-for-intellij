package com.microsoft.azure.toolkit.intellij.devops.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule

interface AzureDevOpsRestJsonDeserializer {
    fun <T> deserialize(json: String, klass: Class<T>): T
    fun <T> deserializeList(json: String, elementType: Class<T>): List<T>
    fun <T, W> deserializeWrapped(json: String, wrapperType: Class<W>, extractor: (W) -> T): T
    fun <T, W> deserializeWrapped(json: String, wrapperType: JavaType, extractor: (W) -> T): T
}

object AzureDevOpsRestJsonDeserializerImpl : AzureDevOpsRestJsonDeserializer {
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun getTypeFactory(): TypeFactory = objectMapper.typeFactory

    override fun <T> deserialize(json: String, klass: Class<T>): T {
        return objectMapper.readValue(json, klass)
    }

    override fun <T> deserializeList(json: String, elementType: Class<T>): List<T> {
        val listType: JavaType = objectMapper.typeFactory.constructCollectionType(List::class.java, elementType)
        return objectMapper.readValue(json, listType)
    }

    override fun <T, W> deserializeWrapped(json: String, wrapperType: Class<W>, extractor: (W) -> T): T {
        try {
            val wrapper = objectMapper.readValue<W>(json, wrapperType)
            return extractor(wrapper)
        } catch (e: Exception) {
            val jsonNode = objectMapper.readTree(json)
            val wrapper = objectMapper.convertValue(jsonNode, wrapperType)
            return extractor(wrapper)
        }
    }

    override fun <T, W> deserializeWrapped(json: String, wrapperType: JavaType, extractor: (W) -> T): T {
        try {
            val wrapper = objectMapper.readValue<W>(json, wrapperType)
            return extractor(wrapper)
        } catch (e: Exception) {
            val jsonNode = objectMapper.readTree(json)
            val wrapper = objectMapper.convertValue<W>(jsonNode, wrapperType)
            return extractor(wrapper)
        }
    }
}
