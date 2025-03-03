@file:OptIn(InternalSerializationApi::class)

package com.adamhammer.ai_shimmer.utils

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.Memorize
import com.adamhammer.ai_shimmer.interfaces.SerializableRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.internal.impl.utils.CollectionsKt
import kotlin.reflect.jvm.javaField

object MethodUtils {

    // Create a shared Json instance.
    private val json = Json { prettyPrint = true }

    /**
     * Public entry point to build a JSON schema as a pretty-printed string.
     */
    fun buildResultSchema(kClass: KClass<*>): String {
        println(">> Entering buildResultSchema with kClass = ${kClass.qualifiedName}")

        // First checkpoint: check if it's a String
        if (kClass == String::class) {
            println(">> Detected kClass is String. Returning \"Text\".")
            return "Text"
        }

        // Second checkpoint: check if it's an enum
        if (kClass.java.isEnum) {
            println(">> Detected kClass is an enum type.")
            val enumSchema = buildEnumSchema(kClass)
            println(">> Built enum schema: $enumSchema")

            val encodedEnum = json.encodeToString(JsonObject.serializer(), enumSchema)
            println(">> Encoded enum schema as JSON: $encodedEnum")

            return encodedEnum
        }

        // Final checkpoint: treat it as a regular class
        println(">> Detected kClass is neither String nor an enum type.")
        val classSchema = buildClassSchema(kClass)
        println(">> Built class schema: $classSchema")

        val encodedClass = json.encodeToString(JsonObject.serializer(), classSchema)
        println(">> Encoded class schema as JSON: $encodedClass")

        return encodedClass
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


    // If the given kClass is a dynamic proxy, return the first interface's KClass; otherwise return kClass.
    private fun actualKClass(kClass: KClass<*>): KClass<*> {
        return if (Proxy.isProxyClass(kClass.java)) {
            kClass.java.interfaces.firstOrNull()?.kotlin ?: kClass
        } else {
            kClass
        }
    }

    // Returns a safe class name: either the Kotlin qualifiedName (if available) or falls back to the Java class name.
    private fun safeClassName(kClass: KClass<*>): String =
        actualKClass(kClass).qualifiedName ?: actualKClass(kClass).java.name

    private fun buildClassSchema(kClass: KClass<*>): JsonObject {
        val realKClass = actualKClass(kClass)
        println(">====================================================")
        println("Entering buildClassSchema(kClass = ${safeClassName(kClass)})")
        println("kClass info:")
        println(
            "  isSealed=${realKClass.isSealed}, isData=${realKClass.isData}, isCompanion=${realKClass.isCompanion}, " +
                    "isInner=${realKClass.isInner}, isOpen=${realKClass.isOpen}, isFinal=${realKClass.isFinal}, isAbstract=${realKClass.isAbstract}"
        )

        // Attempt to read declared members
        val declaredProps = try {
            val props = realKClass.declaredMemberProperties
            println("Successfully fetched declaredMemberProperties: ${props.size} total.")
            println("Property names = [${props.joinToString { it.name }}]")
            props
        } catch (ex: Throwable) {
            println("ERROR: Exception thrown while getting declaredMemberProperties for ${safeClassName(realKClass)}: ${ex.message}")
            ex.printStackTrace()
            // Return an empty list so that building can continue, though it won't have any properties
            emptyList<KProperty1<out Any, Any?>>()
        }

        // Build the JSON object
        val resultJson = try {
            buildJsonObject {
                declaredProps.forEach { prop ->
                    println("----------------------------------------------------")
                    println("Now processing property '${prop.name}' from class ${safeClassName(realKClass)}")

                    // Attempt to read @Schema annotation
                    val schemaAnnotation = try {
                        prop.findAnnotation<Schema>()
                            ?: prop.javaField?.getAnnotation(Schema::class.java)
                    } catch (ex: Throwable) {
                        println("ERROR: Exception while reading annotations from property '${prop.name}': ${ex.message}")
                        ex.printStackTrace()
                        null
                    }

                    val description = schemaAnnotation?.description ?: "optional"
                    println("   Annotation present? ${schemaAnnotation != null}, using description=\"$description\"")

                    // Attempt to read the property’s KClass
                    val propKClass: KClass<*>? = try {
                        prop.returnType.classifier as? KClass<*>
                    } catch (ex: Throwable) {
                        println("ERROR: Exception thrown getting classifier for '${prop.name}': ${ex.message}")
                        ex.printStackTrace()
                        null
                    }

                    // Debug the type
                    println("   Determined property type for '${prop.name}': ${propKClass?.qualifiedName ?: "Could not resolve"}")

                    // Decide how to build JSON for this property
                    val propSchema: JsonElement = try {
                        when {
                            // (1) Enum?
                            propKClass != null && propKClass.java.isEnum -> {
                                println("   -> It's an enum type.")
                                val enumValues = propKClass.java.enumConstants.map { it.toString() }
                                json.encodeToJsonElement("Enum ${prop.name} (${enumValues.joinToString("/")})")
                            }

                            // (2) Map?
                            propKClass == Map::class -> {
                                println("   -> It's a Map; will use a sample {\"key\": \"value\"}.")
                                buildJsonObject {
                                    put("key", "value")
                                }
                            }

                            // (3) List or Set?
                            (propKClass == List::class || propKClass == Set::class) -> {
                                println("   -> It's a List/Set; will use a sample array containing the description.")
                                json.encodeToJsonElement(listOf(description))
                            }

                            // (4) Primitive?
                            propKClass in setOf(
                                String::class, Int::class, Long::class,
                                Double::class, Float::class, Boolean::class
                            ) -> {
                                println("   -> It's a primitive/string type; will use 'description' as the value.")
                                json.encodeToJsonElement(description)
                            }

                            // (5) Nested object?
                            propKClass != null -> {
                                println("   -> It's a nested object; recursing into buildClassSchema(${propKClass.simpleName}).")
                                buildClassSchema(propKClass)
                            }

                            // (6) Fallback
                            else -> {
                                println("   -> Unrecognized type; using fallback with 'description'.")
                                json.encodeToJsonElement(description)
                            }
                        }
                    } catch (ex: Throwable) {
                        println("ERROR: Exception thrown while deciding property type in 'when' for '${prop.name}': ${ex.message}")
                        ex.printStackTrace()
                        // Fallback if something blew up
                        json.encodeToJsonElement("ERROR: ${ex.message ?: "unknown"}")
                    }

                    println("   -> Final schema for '${prop.name}': $propSchema")

                    // Add to the JSON
                    try {
                        put(prop.name, propSchema)
                    } catch (ex: Throwable) {
                        println("ERROR: Exception thrown while putting property '${prop.name}' in JSON: ${ex.message}")
                        ex.printStackTrace()
                    }
                }
            }
        } catch (ex: Throwable) {
            println("ERROR: Exception thrown while building the JSON object for ${safeClassName(realKClass)}: ${ex.message}")
            ex.printStackTrace()
            // Build an error JSON so we at least have something
            buildJsonObject {
                put("error", "Could not build schema for ${safeClassName(realKClass)}, exception: ${ex.message}")
            }
        }

        println("<< Exiting buildClassSchema for class: ${safeClassName(realKClass)}")
        println("<====================================================\n")

        return resultJson
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
            memory = memoryMap
        )
    }

    /**
     * Builds a string representation of the method name along with its Operation summary and description.
     */
    private fun buildMethodField(method: Method): String {
        val op = method.getAnnotation(Operation::class.java)
        val builder = StringBuilder(method.name)
        op?.let {
            if (it.description.isNotEmpty()) builder.append(": ${it.description}")
        }
        return builder.toString()
    }

    private fun buildParameterMap(param: java.lang.reflect.Parameter, index: Int, args: Array<out Any>?): Map<String, JsonElement> {
        val paramMap = mutableMapOf<String, JsonElement>()
        val paramAnn = param.getAnnotation(Parameter::class.java)
        // Store description as a JsonPrimitive.
        paramMap["description"] = JsonPrimitive(paramAnn?.description ?: "")

        // Instead of calling toString(), encode the argument to a JsonElement.
        val valueJson = if (args != null && index < args.size) encodeArgumentValue(args[index]) else JsonNull
        paramMap["value"] = valueJson as JsonElement
        return paramMap
    }


    /**
     * Encodes the provided argument to its JSON string representation using kotlinx.serialization.
     */
    @OptIn(InternalSerializationApi::class)
    private fun encodeArgumentValue(arg: Any?): JsonElement {
        return if (arg == null) {
            JsonNull
        } else {
            try {
                // Get a serializer for the argument's type
                val serializer: KSerializer<Any> = arg::class.serializer() as KSerializer<Any>
                // Encode the argument to a JsonElement rather than a string.
                json.encodeToJsonElement(serializer, arg)
            } catch (e: Exception) {
                // Return an error message as a JsonPrimitive if encoding fails.
                JsonPrimitive("(error encoding to JSON: ${e.message})")
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
            buildResultSchema(schemaAnn.implementation)
        } else {
            buildResultSchema(method.returnType.kotlin)
        }
    }

    /**
     * Parses the provided object for decision schema.
     *
     * This method introspects the object's class, extracts metadata about its methods and properties,
     * and returns a JSON string representing the object's capabilities, including method signatures,
     * associated annotations (such as Operation, Parameter, and ApiResponse), and a schema of the object's type.
     */
    fun parseObjectForDecisionSchema(kClass: Class<Any>): String {

        // Exclude methods inherited from Any.
        val methods = kClass.methods.filter { it.declaringClass == kClass }

        val methodsJson = methods.map { method ->
            buildJsonObject {
                // Always include the method name.
                put("name", method.name)

                // Add Operation annotation details if present and non-blank.
                method.getAnnotation(Operation::class.java)?.let { op ->
                    if (op.description.isNotBlank()) put("description", op.description)
                }

                // Process parameters: only include the description if provided (and not blank).
                val paramsJson = method.parameters.mapNotNull { param ->
                    param.getAnnotation(Parameter::class.java)?.let { paramAnn ->
                        val desc = paramAnn.description.trim()
                        if (desc.isNotEmpty()) buildJsonObject {
                            put("description", desc)
                        } else null
                    }
                }
                if (paramsJson.isNotEmpty()) put("parameters", JsonArray(paramsJson))

            }
        }
        val snapshot = buildJsonObject {
            put("Agent Name", kClass.simpleName ?: "Unknown")
            put("methods", JsonArray(methodsJson))
        }
        return json.encodeToString(JsonObject.serializer(), snapshot)
    }
}
