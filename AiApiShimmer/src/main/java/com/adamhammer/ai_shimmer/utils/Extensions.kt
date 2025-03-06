@file:OptIn(InternalSerializationApi::class)
package com.adamhammer.ai_shimmer.utils

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import java.lang.reflect.Method
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlin.reflect.full.declaredFunctions


private val json = Json { prettyPrint = true }

// Extension method for structural schema representation (1:1 mapping)
fun KClass<*>.toJsonStructure(): JsonElement = when {
    this == String::class -> JsonPrimitive("Text")
    java.isEnum -> buildJsonObject {
        put("enum", JsonArray(java.enumConstants.map { JsonPrimitive(it.toString()) }))
    }
    else -> buildJsonObject {
        declaredMemberProperties.forEach { prop ->
            val schemaDesc = prop.findAnnotation<Schema>()?.description ?: "optional"
            val propType = prop.returnType.classifier as? KClass<*>

            val propSchema = when {
                propType?.java?.isEnum == true -> JsonArray(propType.java.enumConstants.map { JsonPrimitive(it.toString()) })
                propType == Map::class -> buildJsonObject { put("key", JsonPrimitive("value")) }
                propType in setOf(List::class, Set::class) -> JsonArray(listOf(JsonPrimitive(schemaDesc)))
                propType in setOf(String::class, Int::class, Long::class, Double::class, Float::class, Boolean::class) -> JsonPrimitive(schemaDesc)
                propType != null -> propType.toJsonStructure()
                else -> JsonPrimitive(schemaDesc)
            }

            put(prop.name, propSchema)
        }
    }
}

fun KClass<*>.toJsonStructureString(): String = json.encodeToString(toJsonStructure())



// Extension method for method invocation metadata
fun Method.toJsonInvocation(args: Array<out Any>? = null): JsonObject = buildJsonObject {
    val operation = getAnnotation(Operation::class.java)

    put("method", buildString {
        append(name)
        operation?.description?.takeIf { it.isNotBlank() }?.let { append(": $it") }
    })

    put("parameters", JsonArray(parameters.mapIndexed { index, param ->
        buildJsonObject {
            put("description", JsonPrimitive(param.getAnnotation(Parameter::class.java)?.description.orEmpty()))
            val argValue = args?.getOrNull(index)
            put("value", argValue.toJsonElement())
        }
    }))

    val responseSchema = getAnnotation(ApiResponse::class.java)
        ?.content?.firstOrNull()
        ?.schema
        ?.implementation
        ?: returnType.kotlin
    put("resultSchema", responseSchema.toJsonStructure())
}





fun Method.toJsonInvocationString(args: Array<out Any>? = null): String = json.encodeToString(toJsonInvocation(args))

// Extension to serialize an object to JsonElement directly
@OptIn(InternalSerializationApi::class)
fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    else -> try {
        @Suppress("UNCHECKED_CAST")
        json.encodeToJsonElement((this::class.serializer() as KSerializer<Any>), this)
    } catch (e: Exception) {
        JsonPrimitive("(error encoding to JSON: ${e.message})")
    }
}

fun Any?.toJsonString(): String = json.encodeToString(toJsonElement())

// Extension for class-level metadata (method descriptions, annotations)
fun KClass<*>.toJsonClassMetadata(): JsonObject = buildJsonObject {
    put("Agent Name", simpleName ?: "Unknown")
    put("methods", JsonArray(declaredFunctions.map { method ->
        buildJsonObject {
            put("name", method.name)
            method.findAnnotation<Operation>()?.description?.takeIf { it.isNotBlank() }?.let {
                put("description", it)
            }
            put("parameters", JsonArray(method.parameters.mapNotNull { param ->
                param.findAnnotation<Parameter>()?.description?.takeIf { it.isNotBlank() }?.let {
                    buildJsonObject { put("description", it) }
                }
            }))
        }
    }))
}

fun KClass<*>.toJsonClassMetadataString(): String = json.encodeToString(toJsonClassMetadata())