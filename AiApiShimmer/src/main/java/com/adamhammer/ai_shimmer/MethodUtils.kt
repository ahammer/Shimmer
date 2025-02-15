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

object MethodUtils {

    /**
     * Inspects a java.lang.reflect.Method and builds a nicely indented description of
     * the method’s name, its @AI annotation (if present), and for each parameter,
     * prints its (optional) @AI metadata as well as recursively inspecting the parameter’s class.
     */
    fun buildMetaData(method: Method): String {
        val sb = StringBuilder()

        // Process the method’s own @AI annotation, if available.
        method.getAnnotation(AI::class.java)?.let { ai ->
            sb.append("${ai.label}: ${ai.description}\n")
        }

        // Process each parameter of the method.
        method.parameters.forEach { param ->
            param.type.getAnnotation(AI::class.java)?.let { ai ->
                sb.append("    ${param.type.simpleName}: ${ai.description}\n")
            }
            val kClass = param.type.kotlin
            sb.append(recursivelyProcessClass(kClass, indent = "    "))
        }
        return sb.toString()
    }

    /**
     * Recursively inspects the given class’s properties. For each property that has an @AI annotation,
     * it prints the property name along with its label and description. If the property’s type
     * is itself a non-primitive (and non-String) class, it recurses.
     */
    private fun recursivelyProcessClass(kClass: KClass<*>, indent: String): String {
        val sb = StringBuilder()
        kClass.declaredMemberProperties.forEach { prop ->
            val annotations = prop.annotations
            prop.findAnnotation<AI>()?.let { ai ->
                sb.append("$indent ${prop.name}: ${ai.description}\n")
            }

            // Determine if the property’s type is a Kotlin class.
            val propClass = prop.returnType.classifier as? KClass<*>
            // Skip primitives and common types (to avoid endless recursion)
            if (propClass != null && !propClass.java.isPrimitive && propClass != String::class) {
                // Recurse a level deeper.
                sb.append(recursivelyProcessClass(propClass, indent + "  "))
            }
        }
        return sb.toString()
    }

    /**
     * Generates a JSON “schema” for the given Kotlin class. This schema includes the class name
     * and a mapping of field names to their descriptions (as provided in the @AI annotation on each property).
     *
     * The JSON output might look like:
     *
     * {
     *    "class": "Question",
     *    "fields": {
     *         "text": "The question to be asked",
     *         "context": "Who is asking the Question"
     *    }
     * }
     */
    fun buildSchema(kClass: KClass<*>): String {
        if (kClass == String::class) {
            return "Respond with text"
        }
        val fieldsObject = buildJsonObject {
            kClass.declaredMemberProperties.forEach { prop ->
                // Use the description from @AI if available.
                val description = prop.findAnnotation<AI>()?.description ?: ""
                put(prop.name, description)
            }
        }

        // Return the JSON as a pretty-printed string.
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), fieldsObject)
    }

    @OptIn(InternalSerializationApi::class)
    fun buildParameterData(args: Array<out Any>?): String {
        val sb = StringBuilder()
        val json = Json { prettyPrint = true }

        args?.forEach { arg ->
            arg.javaClass.getAnnotation(AI::class.java)?.let { ai ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    // Get the serializer for the runtime type and cast it to KSerializer<Any>
                    val serializer = arg::class.serializer() as KSerializer<Any>
                    val jsonEncoded = json.encodeToString(serializer, arg)
                    sb.append("${ai.label} = $jsonEncoded\n")
                } catch (e: Exception) {
                    sb.append("${ai.label} = (error encoding to JSON: ${e.message})\n")
                }
            }
        }
        return sb.toString()
    }

}