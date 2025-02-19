@file:OptIn(InternalSerializationApi::class)

package com.adamhammer.ai_shimmer.utils


import com.adamhammer.ai_shimmer.interfaces.Memorize
import com.adamhammer.ai_shimmer.interfaces.SerializableRequest
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import java.lang.reflect.Method

object MethodUtils {

    // Create a shared Json instance.
    private val json = Json { prettyPrint = true }

    /**
     * Public entry point to build a JSON schema as a pretty-printed string.
     */
    fun buildSchema(kClass: KClass<*>): String {
        return when {
            kClass == String::class -> "\"Respond with text\""
            kClass.java.isEnum -> json.encodeToString(JsonObject.serializer(), buildEnumSchema(kClass))
            else -> json.encodeToString(JsonObject.serializer(), buildClassSchema(kClass))
        }
    }

    /**
     * Returns a JSON object representing the schema of an enum.
     */
    private fun buildEnumSchema(kClass: KClass<*>): JsonObject {
        val enumValues = kClass.java.enumConstants.map { it.toString() }
        return buildJsonObject {
            put("enum", json.encodeToJsonElement(enumValues))
        }
    }

    /**
     * Recursively builds a JSON object for a non-enum class by inspecting its declared properties.
     * If a property is an enum, its possible values are included.
     */
    private fun buildClassSchema(kClass: KClass<*>): JsonObject {
        return buildJsonObject {
            kClass.declaredMemberProperties.forEach { prop ->
                val description = prop.findAnnotation<Schema>()?.description ?: "optional"
                val propKClass = prop.returnType.classifier as? KClass<*>
                val propSchema = if (propKClass != null && propKClass.java.isEnum) {
                    buildJsonObject {
                        put("description", description)
                        put("enum", json.encodeToJsonElement(propKClass.java.enumConstants.map { it.toString() }))
                    }
                } else {
                    buildJsonObject {
                        put("description", description)
                        put("schema", buildSchema(propKClass ?: String::class))
                    }
                }
                put(prop.name, propSchema)
            }
        }
    }

    /**
     * Generates a SerializableRequest by extracting method and parameter metadata.
     */
    @OptIn(InternalSerializationApi::class)
    fun generateSerializableRequest(method: Method, args: Array<out Any>?, memoryMap: MutableMap<String, String>): SerializableRequest {
        val methodField = buildMethodField(method)
        val parametersList = method.parameters.mapIndexed { index, param ->
            buildParameterMap(param, index, args)
        }
        val resultSchema = buildResultSchema(method)

        return SerializableRequest(
            method = methodField,
            parameters = parametersList,
            memory = memoryMap,
            resultSchema = resultSchema
        )
    }

    /**
     * Builds a string representation of the method name along with its Operation summary and description.
     */
    private fun buildMethodField(method: Method): String {
        val op = method.getAnnotation(Operation::class.java)
        val builder = StringBuilder(method.name)
        op?.let {
            if (it.summary.isNotEmpty()) builder.append(" - Summary: ${it.summary}")
            if (it.description.isNotEmpty()) builder.append(" - Description: ${it.description}")
        }
        return builder.toString()
    }

    /**
     * Builds a map representing a method parameter's metadata and its encoded value.
     */
    private fun buildParameterMap(param: java.lang.reflect.Parameter, index: Int, args: Array<out Any>?): Map<String, String> {
        val paramMap = mutableMapOf<String, String>()
        val paramAnn = param.getAnnotation(Parameter::class.java)
        paramMap["description"] = paramAnn?.description ?: ""
        paramMap["value"] = encodeArgumentValue(if (args != null && index < args.size) args[index] else null)
        return paramMap
    }

    /**
     * Encodes the provided argument to its JSON string representation using kotlinx.serialization.
     */
    private fun encodeArgumentValue(arg: Any?): String {
        return if (arg == null) {
            "null"
        } else {
            try {
                val serializer: KSerializer<Any> = arg::class.serializer() as KSerializer<Any>
                json.encodeToString(serializer, arg)
            } catch (e: Exception) {
                "(error encoding to JSON: ${e.message})"
            }
        }
    }

    /**
     * Extracts memory information if the method is annotated with @Memorize.
     */
    private fun buildMemoryMap(method: Method): Map<String, String> {
        val memorizeAnn = method.getAnnotation(Memorize::class.java)
        return if (memorizeAnn != null) {
            mapOf("memorize" to memorizeAnn.label)
        } else {
            emptyMap()
        }
    }

    /**
     * Builds the result schema based on the method's ApiResponse annotation or its return type.
     */
    private fun buildResultSchema(method: Method): String {
        val apiResponse = method.getAnnotation(ApiResponse::class.java)
        return if (apiResponse != null && apiResponse.content.isNotEmpty()) {
            val schemaAnn = apiResponse.content[0].schema
            buildSchema(schemaAnn.implementation)
        } else {
            buildSchema(method.returnType.kotlin)
        }
    }
}
