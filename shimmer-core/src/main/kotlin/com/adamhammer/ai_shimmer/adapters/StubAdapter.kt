package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ShimmerConfigurationException
import kotlin.reflect.KClass

/**
 * Test adapter that returns a default-constructed instance of the result class.
 * Handles String, enums, boxed primitives, and data classes with all-default constructors.
 * Useful for verifying proxy wiring without making real AI calls.
 */
class StubAdapter : ApiAdapter {
    @Suppress("UNCHECKED_CAST")
    override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        return when (resultClass) {
            String::class -> "" as R
            Int::class -> 0 as R
            Long::class -> 0L as R
            Double::class -> 0.0 as R
            Float::class -> 0f as R
            Boolean::class -> false as R
            else -> when {
                resultClass.java.isEnum -> resultClass.java.enumConstants.first() as R
                else -> {
                    // Try Kotlin reflection first (handles data classes with all-default parameters)
                    val ktCtor = resultClass.constructors
                        .firstOrNull { ctor -> ctor.parameters.all { it.isOptional } }
                    if (ktCtor != null) {
                        ktCtor.callBy(emptyMap())
                    } else {
                        try {
                            resultClass.java.getDeclaredConstructor().newInstance()
                        } catch (e: NoSuchMethodException) {
                            throw ShimmerConfigurationException(
                                "StubAdapter cannot create an instance of ${resultClass.simpleName}: " +
                                "no no-arg constructor found. Ensure the class has a no-arg constructor " +
                                "or all-default parameters."
                            )
                        }
                    }
                }
            }
        }
    }
}
