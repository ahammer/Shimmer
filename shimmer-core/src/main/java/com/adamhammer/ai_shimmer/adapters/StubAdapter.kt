package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.model.PromptContext
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
                else -> resultClass.java.getDeclaredConstructor().newInstance()
            }
        }
    }
}
