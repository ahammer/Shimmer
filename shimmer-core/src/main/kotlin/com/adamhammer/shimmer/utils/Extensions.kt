@file:Suppress("TooManyFunctions")

package com.adamhammer.shimmer.utils

import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.declaredFunctions
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

import com.adamhammer.shimmer.annotations.AiOperation
import com.adamhammer.shimmer.annotations.AiParameter
import com.adamhammer.shimmer.annotations.AiResponse
import com.adamhammer.shimmer.annotations.AiSchema
import com.adamhammer.shimmer.annotations.Terminal
import com.adamhammer.shimmer.model.MethodDescriptor
import com.adamhammer.shimmer.model.ToolDefinition

private val json = Json { prettyPrint = true }

fun KClass<*>.toJsonStructure(): JsonElement = when {
    this == String::class -> JsonPrimitive("Text")
    java.isEnum -> buildJsonObject {
        put("enum", JsonArray(java.enumConstants.map { JsonPrimitive(it.toString()) }))
    }
    else -> buildJsonObject {
        val classAnnotation = findAnnotation<AiSchema>()
        declaredMemberProperties.forEach { prop ->
            val schemaDesc = resolveStructureDescription(prop, classAnnotation)
            val propType = prop.returnType.classifier as? KClass<*>
            put(prop.name, buildStructureProperty(propType, prop, schemaDesc))
        }
    }
}

private fun KClass<*>.resolveStructureDescription(
    prop: kotlin.reflect.KProperty1<*, *>,
    classAnnotation: AiSchema?
): String {
    val javaField = try {
        java.getDeclaredField(prop.name)
    } catch (_: NoSuchFieldException) {
        null
    }
    val fieldAnnotation = javaField?.getAnnotation(AiSchema::class.java)
    return when {
        fieldAnnotation?.description?.isNotBlank() == true -> fieldAnnotation.description
        fieldAnnotation?.title?.isNotBlank() == true -> fieldAnnotation.title
        prop.findAnnotation<AiSchema>()?.description?.isNotBlank() == true ->
            prop.findAnnotation<AiSchema>()!!.description
        prop.findAnnotation<AiSchema>()?.title?.isNotBlank() == true ->
            prop.findAnnotation<AiSchema>()!!.title
        classAnnotation?.description?.isNotBlank() == true ->
            "${classAnnotation.description} - ${prop.name}"
        classAnnotation?.title?.isNotBlank() == true ->
            "${classAnnotation.title} - ${prop.name}"
        else -> prop.name
    }
}

private fun buildStructureProperty(
    propType: KClass<*>?,
    prop: kotlin.reflect.KProperty1<*, *>,
    schemaDesc: String
): JsonElement = when {
    propType?.java?.isEnum == true -> {
        val values = propType.java.enumConstants.joinToString("/") { it.toString() }
        JsonPrimitive("Enum ${prop.name} ($values)")
    }
    propType == Map::class -> buildJsonObject { put("key", JsonPrimitive("value")) }
    propType in setOf(List::class, Set::class) -> JsonArray(listOf(JsonPrimitive(schemaDesc)))
    propType in setOf(String::class, Int::class, Long::class, Double::class, Float::class, Boolean::class) ->
        JsonPrimitive(schemaDesc)
    propType != null -> propType.toJsonStructure()
    else -> JsonPrimitive(schemaDesc)
}

fun KClass<*>.toJsonStructureString(): String = json.encodeToString(toJsonStructure())

fun Method.toJsonInvocation(
    args: Array<out Any>? = null,
    memory: Map<String, String> = emptyMap()
): JsonObject = buildJsonObject {
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

    if (memory.isNotEmpty()) {
        put("memory", buildJsonObject {
            memory.forEach { (key, value) ->
                put(key, JsonPrimitive(value))
            }
        })
    }

    val responseAnnotation = getAnnotation(AiResponse::class.java)
    val responseSchema = if (responseAnnotation != null && responseAnnotation.responseClass != Unit::class) {
        responseAnnotation.responseClass
    } else {
        // Extract the generic type argument from Future<T> (or similar parameterized return types)
        val generic = genericReturnType
        if (generic is ParameterizedType) {
            (generic.actualTypeArguments.firstOrNull() as? Class<*>)?.kotlin ?: returnType.kotlin
        } else {
            returnType.kotlin
        }
    }
    put("resultSchema", responseSchema.toJsonStructure())
}

fun Method.toJsonInvocationString(args: Array<out Any>? = null, memory: Map<String, String> = emptyMap()): String {
    val jsonObject = toJsonInvocation(args, memory)
    return json.encodeToString(jsonObject)
}

// ── MethodDescriptor extensions ─────────────────────────────────────────────

fun MethodDescriptor.toJsonInvocation(memory: Map<String, String> = emptyMap()): JsonObject = buildJsonObject {
    put("method", buildString {
        append(name)
        when {
            operationSummary.isNotBlank() -> append(": $operationSummary")
            operationDescription.isNotBlank() -> append(": $operationDescription")
        }
    })
    put("parameters", JsonArray(parameters.map { param ->
        buildJsonObject {
            put("description", JsonPrimitive(param.description))
            put("value", param.value.toJsonElement())
        }
    }))
    if (memory.isNotEmpty()) {
        put("memory", buildJsonObject {
            memory.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
        })
    }
    put("resultSchema", resultClass.toJsonStructure())
}

fun MethodDescriptor.toJsonInvocationString(memory: Map<String, String> = emptyMap()): String =
    json.encodeToString(toJsonInvocation(memory))

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    else -> try {
        val serializer = serializer(this::class.createType())
        json.encodeToJsonElement(serializer, this)
    } catch (e: Exception) {
        JsonPrimitive("(error encoding to JSON: ${e.message})")
    }
}

fun Any?.toJsonString(): String = json.encodeToString(toJsonElement())

fun KClass<*>.toJsonClassMetadata(excludedMethods: Set<String> = emptySet()): JsonObject = buildJsonObject {
    put("Agent Name", simpleName ?: "Unknown")
    val filteredMethods = declaredFunctions.filter { it.name !in excludedMethods }
    put("methods", JsonArray(filteredMethods.map { method ->
        buildJsonObject {
            put("name", method.name)
            method.findAnnotation<AiOperation>()?.let {
                when {
                    it.summary.isNotBlank() -> put("summary", it.summary)
                    it.description.isNotBlank() -> put("description", it.description)
                    else -> {}
                }
            }
            if (method.findAnnotation<Terminal>() != null) {
                put("terminal", true)
            }
            val responseClass = method.findAnnotation<AiResponse>()?.responseClass
                ?.takeIf { it != Unit::class }
            if (responseClass != null) {
                put("returnType", responseClass.simpleName ?: "Unknown")
            }
            val params = method.parameters.mapNotNull { param ->
                param.findAnnotation<AiParameter>()?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    buildJsonObject {
                        put("name", param.name ?: "arg")
                        put("description", desc)
                    }
                }
            }
            if (params.isNotEmpty()) {
                put("parameters", JsonArray(params))
            }
        }
    }))
}

fun KClass<*>.toJsonClassMetadataString(excludedMethods: Set<String> = emptySet()): String =
    json.encodeToString(toJsonClassMetadata(excludedMethods))

/**
 * Converts an annotated interface method into a provider-agnostic [ToolDefinition].
 *
 * Builds a JSON Schema `inputSchema` from [AiParameter] annotations and parameter types,
 * and an `outputSchema` from the return type / [AiResponse] annotation.
 */
fun Method.toToolDefinition(): ToolDefinition {
    val operation = getAnnotation(AiOperation::class.java)
    val description = when {
        operation?.description?.isNotBlank() == true -> operation.description
        operation?.summary?.isNotBlank() == true -> operation.summary
        else -> name
    }

    val inputSchema = buildToolInputSchema(this)
    val responseSchema = resolveResponseClass(this)
    val outputSchemaStr = responseSchema.toJsonStructureString()

    return ToolDefinition(
        name = name,
        description = description,
        inputSchema = json.encodeToString(inputSchema),
        outputSchema = outputSchemaStr
    )
}

private fun kotlinTypeToJsonType(klass: KClass<*>): String = when (klass) {
    String::class -> "string"
    Int::class, Long::class -> "integer"
    Double::class, Float::class -> "number"
    Boolean::class -> "boolean"
    else -> "string"
}

private fun buildToolInputSchema(method: Method): JsonObject = buildJsonObject {
    put("type", "object")
    val props = buildJsonObject {
        method.parameters.forEach { param ->
            val paramDesc = param.getAnnotation(AiParameter::class.java)?.description ?: param.name
            val paramType = param.type.kotlin
            put(param.name, buildJsonObject {
                put("type", kotlinTypeToJsonType(paramType))
                put("description", paramDesc)
            })
        }
    }
    put("properties", props)
    put("required", JsonArray(method.parameters.map { JsonPrimitive(it.name) }))
}

private fun resolveResponseClass(method: Method): KClass<*> {
    val responseAnnotation = method.getAnnotation(AiResponse::class.java)
    if (responseAnnotation != null && responseAnnotation.responseClass != Unit::class) {
        return responseAnnotation.responseClass
    }
    val generic = method.genericReturnType
    if (generic is ParameterizedType) {
        return (generic.actualTypeArguments.firstOrNull() as? Class<*>)?.kotlin ?: method.returnType.kotlin
    }
    return method.returnType.kotlin
}

/**
 * Converts all annotated methods on an interface into [ToolDefinition]s.
 */
fun KClass<*>.toToolDefinitions(): List<ToolDefinition> {
    return java.declaredMethods
        .filter { it.isAnnotationPresent(AiOperation::class.java) }
        .map { it.toToolDefinition() }
}

/**
 * Generates a proper JSON Schema object from a Kotlin class.
 *
 * Unlike [toJsonStructure] which produces example JSON payloads for prompt engineering,
 * this produces a valid JSON Schema (compatible with OpenAI's structured output
 * `response_format: json_schema`) with `type`, `properties`, `required`, and
 * `additionalProperties` fields.
 *
 * Supports: primitives, enums, `List<T>`, `Map<String, T>`, and nested data classes.
 * Pulls `description` from [AiSchema] annotations when available.
 */
fun KClass<*>.toJsonSchema(): JsonObject = when {
    this == String::class -> buildJsonObject { put("type", "string") }
    this == Int::class || this == Long::class -> buildJsonObject { put("type", "integer") }
    this == Double::class || this == Float::class -> buildJsonObject { put("type", "number") }
    this == Boolean::class -> buildJsonObject { put("type", "boolean") }
    java.isEnum -> buildJsonObject {
        put("type", "string")
        put("enum", JsonArray(java.enumConstants.map { JsonPrimitive(it.toString()) }))
    }
    else -> buildJsonObject {
        put("type", "object")
        val classAnnotation = findAnnotation<AiSchema>()
        if (classAnnotation?.description?.isNotBlank() == true) {
            put("description", classAnnotation.description)
        }
        val props = buildJsonObject {
            declaredMemberProperties.forEach { prop ->
                val propType = prop.returnType.classifier as? KClass<*>
                val schemaDesc = resolvePropertyDescription(prop, classAnnotation)
                val propSchema = buildPropertySchema(propType, prop, schemaDesc)
                put(prop.name, propSchema)
            }
        }
        put("properties", props)
        put("required", JsonArray(declaredMemberProperties.map { JsonPrimitive(it.name) }))
        put("additionalProperties", JsonPrimitive(false))
    }
}

private fun KClass<*>.resolvePropertyDescription(
    prop: kotlin.reflect.KProperty1<*, *>,
    classAnnotation: AiSchema?
): String? {
    val javaField = try {
        java.getDeclaredField(prop.name)
    } catch (_: NoSuchFieldException) {
        null
    }
    val fieldAnnotation = javaField?.getAnnotation(AiSchema::class.java)
    return when {
        fieldAnnotation?.description?.isNotBlank() == true -> fieldAnnotation.description
        fieldAnnotation?.title?.isNotBlank() == true -> fieldAnnotation.title
        prop.findAnnotation<AiSchema>()?.description?.isNotBlank() == true ->
            prop.findAnnotation<AiSchema>()!!.description
        prop.findAnnotation<AiSchema>()?.title?.isNotBlank() == true ->
            prop.findAnnotation<AiSchema>()!!.title
        classAnnotation?.description?.isNotBlank() == true ->
            "${classAnnotation.description} - ${prop.name}"
        else -> null
    }
}

private val primitiveTypes = setOf(String::class, Int::class, Long::class, Double::class, Float::class, Boolean::class)

private fun buildPropertySchema(
    propType: KClass<*>?,
    prop: kotlin.reflect.KProperty1<*, *>,
    description: String?
): JsonObject = buildJsonObject {
    when {
        propType == null -> put("type", "string")
        propType in primitiveTypes -> put("type", kotlinTypeToJsonType(propType))
        propType.java.isEnum -> {
            put("type", "string")
            put("enum", JsonArray(propType.java.enumConstants.map { JsonPrimitive(it.toString()) }))
        }
        propType == List::class || propType == Set::class -> {
            put("type", "array")
            val elementType = prop.returnType.arguments.firstOrNull()
                ?.type?.classifier as? KClass<*>
            if (elementType != null) put("items", elementType.toJsonSchema())
        }
        propType == Map::class -> {
            put("type", "object")
            put("additionalProperties", JsonPrimitive(true))
        }
        else -> {
            propType.toJsonSchema().forEach { (k, v) -> put(k, v) }
            return@buildJsonObject
        }
    }
    if (description != null) put("description", description)
}

/**
 * Returns the JSON Schema as a JSON string.
 */
fun KClass<*>.toJsonSchemaString(): String = json.encodeToString(toJsonSchema())
