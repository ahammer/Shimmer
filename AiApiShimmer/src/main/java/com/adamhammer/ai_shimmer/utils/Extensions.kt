@file:OptIn(InternalSerializationApi::class)
package com.adamhammer.ai_shimmer.utils

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.declaredFunctions
import java.lang.reflect.Method

// New custom annotations replacing Swagger
import com.adamhammer.ai_shimmer.annotations.AiOperation
import com.adamhammer.ai_shimmer.annotations.AiParameter
import com.adamhammer.ai_shimmer.annotations.AiResponse
import com.adamhammer.ai_shimmer.annotations.AiSchema

private val json = Json { prettyPrint = true }

// Extension method for structural schema representation (1:1 mapping)
fun KClass<*>.toJsonStructure(): JsonElement = when {
    this == String::class -> JsonPrimitive("Text")
    java.isEnum -> buildJsonObject {
        put("enum", JsonArray(java.enumConstants.map { JsonPrimitive(it.toString()) }))
    }
    else -> buildJsonObject {
        declaredMemberProperties.forEach { prop ->
            // If available, use AiSchema annotation to provide a description
            val schemaDesc = prop.findAnnotation<AiSchema>()?.description ?: "optional"
            val propType = prop.returnType.classifier as? KClass<*>

            val propSchema = when {
                propType?.java?.isEnum == true ->
                    JsonArray(propType.java.enumConstants.map { JsonPrimitive(it.toString()) })
                propType == Map::class ->
                    buildJsonObject { put("key", JsonPrimitive("value")) }
                propType in setOf(List::class, Set::class) ->
                    JsonArray(listOf(JsonPrimitive(schemaDesc)))
                propType in setOf(String::class, Int::class, Long::class, Double::class, Float::class, Boolean::class) ->
                    JsonPrimitive(schemaDesc)
                propType != null ->
                    propType.toJsonStructure()
                else ->
                    JsonPrimitive(schemaDesc)
            }
            put(prop.name, propSchema)
        }
    }
}

fun KClass<*>.toJsonStructureString(): String = json.encodeToString(toJsonStructure())

// Extension method for method invocation metadata
fun Method.toJsonInvocation(args: Array<out Any>? = null, memory: Map<String, String> = emptyMap()): JsonObject = buildJsonObject {
    val operation = getAnnotation(AiOperation::class.java)
    put("method", buildString {
        append(name)
        operation?.let {
            when {
                it.summary.isNotBlank() -> append(": ${it.summary}")
                it.description.isNotBlank() -> append(": ${it.description}")
            }
        }
    })

    put("parameters", JsonArray(parameters.mapIndexed { index, param ->
        buildJsonObject {
            put("description", JsonPrimitive(param.getAnnotation(AiParameter::class.java)?.description.orEmpty()))
            val argValue = args?.getOrNull(index)
            put("value", argValue.toJsonElement())
        }
    }))

    // Include memory in the method invocation JSON
    if (memory.isNotEmpty()) {
        put("memory", buildJsonObject {
            memory.forEach { (key, value) ->
                put(key, JsonPrimitive(value))
            }
        })
    }

    val responseSchema = getAnnotation(AiResponse::class.java)?.responseClass ?: returnType.kotlin
    put("resultSchema", responseSchema.toJsonStructure())
}

// Custom JSON encoder to ensure proper JSON formatting with commas between fields
fun Method.toJsonInvocationString(args: Array<out Any>? = null, memory: Map<String, String> = emptyMap()): String {
    val jsonObject = toJsonInvocation(args, memory)
    return json.encodeToString(jsonObject)
}


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
            method.findAnnotation<AiOperation>()?.let {
                when {
                    it.summary.isNotBlank() -> put("summary", it.summary)
                    it.description.isNotBlank() -> put("description", it.description)
                    else -> {}
                }
            }
            put("parameters", JsonArray(method.parameters.mapNotNull { param ->
                param.findAnnotation<AiParameter>()?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    buildJsonObject { put("description", desc) }
                }
            }))
        }
    }))
}

fun KClass<*>.toJsonClassMetadataString(): String =
    json.encodeToString(toJsonClassMetadata())
