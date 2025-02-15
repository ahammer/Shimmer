package com.adamhammer.ai_shimmer

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer

// Swagger/OpenAPI annotations
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter

object MethodUtils {

    /**
     * Inspects a [Method] and builds a nicely indented description of:
     *  - The method’s name along with its Swagger @Operation metadata (if present)
     *  - For each parameter, its @Parameter description (if present)
     *  - And a recursive listing of each parameter’s properties using @Schema metadata.
     */
    fun buildMetaData(method: Method): String {
        val sb = StringBuilder()

        // Process the method’s @Operation annotation, if available.
        method.getAnnotation(Operation::class.java)?.let { op ->
            if (op.summary.isNotEmpty()) {
                sb.append("Operation: ${op.summary}\n")
            }
            if (op.description.isNotEmpty()) {
                sb.append("Description: ${op.description}\n")
            }
        }

        // Process each parameter of the method.
        method.parameters.forEach { param ->
            param.getAnnotation(Parameter::class.java)?.let { paramAnn ->
                if (paramAnn.description.isNotEmpty()) {
                    sb.append("    ${param.type.simpleName} parameter: ${paramAnn.description}\n")
                }
            }
            // Recursively process the parameter’s class.
            val kClass = param.type.kotlin
            sb.append(recursivelyProcessClass(kClass, indent = "    "))
        }
        return sb.toString()
    }

    /**
     * Recursively inspects the given class’s properties.
     * For each property with a @Schema annotation, prints its title (or property name)
     * and description. Properties without a @Schema annotation are marked as optional.
     * If a property’s type is a non-primitive (and non-String) class, it recurses.
     */
    private fun recursivelyProcessClass(kClass: KClass<*>, indent: String): String {
        val sb = StringBuilder()
        kClass.declaredMemberProperties.forEach { prop ->
            // Check for @Schema on the property.
            val schema = prop.findAnnotation<Schema>()
            if (schema != null) {
                val title = if (schema.title.isNotEmpty()) schema.title else prop.name
                val description = if (schema.description.isNotEmpty()) schema.description else "optional"
                sb.append("$indent$title: $description\n")
            } else {
                sb.append("$indent${prop.name}: (optional)\n")
            }

            // Recurse into property types that are Kotlin classes (skip primitives and String).
            val propClass = prop.returnType.classifier as? KClass<*>
            if (propClass != null && !propClass.java.isPrimitive && propClass != String::class) {
                sb.append(recursivelyProcessClass(propClass, indent + "  "))
            }
        }
        return sb.toString()
    }

    /**
     * Generates a JSON “schema” for the given Kotlin class. This schema includes a mapping of field names
     * to their descriptions (as provided in the @Schema annotation on each property). Fields without a
     * @Schema annotation are marked as optional.
     *
     * Example output:
     *
     * {
     *    "text": "The question to be asked",
     *    "context": "Who is asking the Question"
     * }
     */
    fun buildSchema(kClass: KClass<*>): String {
        if (kClass == String::class) {
            return "Respond with text"
        }

        val fieldsObject = buildJsonObject {
            kClass.declaredMemberProperties.forEach { prop ->
                val description = prop.findAnnotation<Schema>()?.description ?: "optional"
                put(prop.name, description)
            }
        }

        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), fieldsObject)
    }

    /**
     * Iterates over the provided arguments and attempts to JSON-encode each one.
     * If the argument’s class has a @Schema annotation, its title is used as a label.
     * Otherwise, the class simple name is used. Fields that cannot be encoded
     * return an error message.
     */
    @OptIn(InternalSerializationApi::class)
    fun buildParameterData(args: Array<out Any>?): String {
        val sb = StringBuilder()
        val json = Json { prettyPrint = true }

        args?.forEach { arg ->
            // Check for @Schema annotation on the argument's class.
            val schema = arg.javaClass.getAnnotation(Schema::class.java)
            val label = if (schema != null && schema.title.isNotEmpty())
                schema.title
            else
                arg::class.simpleName ?: "Unknown"

            try {
                @Suppress("UNCHECKED_CAST")
                val serializer = arg::class.serializer() as KSerializer<Any>
                val jsonEncoded = json.encodeToString(serializer, arg)
                sb.append("$label = $jsonEncoded\n")
            } catch (e: Exception) {
                sb.append("$label = (error encoding to JSON: ${e.message})\n")
            }
        }
        return sb.toString()
    }
}
