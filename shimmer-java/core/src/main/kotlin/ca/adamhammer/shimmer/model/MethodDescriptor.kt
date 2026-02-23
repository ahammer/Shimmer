package ca.adamhammer.shimmer.model

import ca.adamhammer.shimmer.annotations.AiOperation
import ca.adamhammer.shimmer.annotations.AiParameter
import ca.adamhammer.shimmer.annotations.AiResponse
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

/**
 * Provider-agnostic description of a proxy method invocation.
 *
 * Replaces the raw `java.lang.reflect.Method` in the core model so that
 * custom [ca.adamhammer.shimmer.interfaces.ContextBuilder] implementations
 * do not depend on Java reflection internals.
 */
data class MethodDescriptor(
    val name: String,
    val operationSummary: String = "",
    val operationDescription: String = "",
    val parameters: List<ParameterDescriptor> = emptyList(),
    val resultClass: KClass<*>,
    val responseDescription: String = ""
) {
    companion object {
        /** Build a [MethodDescriptor] from a Java [Method] and its invocation arguments. */
        fun from(method: Method, args: List<Any>?, resultClass: KClass<*>): MethodDescriptor {
            val operation = method.getAnnotation(AiOperation::class.java)
            val response = method.getAnnotation(AiResponse::class.java)
            return MethodDescriptor(
                name = method.name,
                operationSummary = operation?.summary ?: "",
                operationDescription = operation?.description ?: "",
                parameters = method.parameters.mapIndexed { i, param ->
                    ParameterDescriptor(
                        name = param.name,
                        description = param.getAnnotation(AiParameter::class.java)?.description ?: "",
                        value = args?.getOrNull(i)
                    )
                },
                resultClass = resultClass,
                responseDescription = response?.description ?: ""
            )
        }

        /**
         * Resolve the result [KClass] from a [Method]'s return type, using
         * [AiResponse.responseClass] if present, otherwise unwrapping the
         * first generic type argument (e.g., `Future<MyResult>` → `MyResult`).
         */
        fun resolveResultClass(method: Method): KClass<*> {
            val response = method.getAnnotation(AiResponse::class.java)
            if (response != null && response.responseClass != Unit::class) {
                return response.responseClass
            }
            val generic = method.genericReturnType
            if (generic is ParameterizedType) {
                return (generic.actualTypeArguments.firstOrNull() as? Class<*>)?.kotlin
                    ?: method.returnType.kotlin
            }
            return method.returnType.kotlin
        }
    }
}

/**
 * A single parameter in a [MethodDescriptor].
 */
data class ParameterDescriptor(
    val name: String,
    val description: String = "",
    val value: Any? = null
)
