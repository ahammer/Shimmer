package com.adamhammer.ai_shimmer

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.reflect.KClass

object AiApiShimmer {

    // Convenience reified inline function.
    inline fun <reified T : Any> wrap(): T = wrap(T::class)

    // Generic wrap function that creates a dynamic proxy for the provided interface.
    fun <T : Any> wrap(apiInterface: KClass<T>): T {
        val handler = ApiInvocationHandler()
        val proxyInstance = Proxy.newProxyInstance(
            apiInterface.java.classLoader,
            arrayOf(apiInterface.java),
            handler
        )
        return apiInterface.java.cast(proxyInstance)
    }

    // Custom InvocationHandler that intercepts method calls.
    private class ApiInvocationHandler : InvocationHandler {
        // Create a Json instance with your desired configuration.
        private val json = Json { ignoreUnknownKeys = true }

        @OptIn(InternalSerializationApi::class)
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
            println("Invoked method: ${method.name}")

            // Check if the method's return type is assignable from Future.
            if (Future::class.java.isAssignableFrom(method.returnType)) {
                return CompletableFuture.supplyAsync {
                    // Simulated JSON response.
                    val jsonResponse = """{"text": "Simulated answer for ${method.name}"}"""

                    // Retrieve the generic type T from Future<T>
                    val genericReturnType = method.genericReturnType
                    if (genericReturnType is ParameterizedType) {
                        val actualType = genericReturnType.actualTypeArguments[0]
                        // We expect the type argument to be a class.
                        val clazz = (actualType as? Class<*>)
                            ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")

                        // Convert to Kotlin KClass.
                        val kClass = clazz.kotlin

                        // Obtain the serializer for the expected type.
                        // Note: This requires that the type is annotated with @Serializable.
                        val kSerializer: KSerializer<Any> = kClass.serializer() as KSerializer<Any>

                        // Deserialize the JSON into the desired type.
                        json.decodeFromString(kSerializer, jsonResponse)
                    } else {
                        throw IllegalStateException("Return type of method ${method.name} is not parameterized")
                    }
                }
            }

            // Optionally handle other return types or throw if unsupported.
            throw UnsupportedOperationException("Method ${method.name} is not supported by the proxy")
        }
    }
}
