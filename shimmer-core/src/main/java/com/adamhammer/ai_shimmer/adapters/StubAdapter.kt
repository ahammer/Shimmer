package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.model.PromptContext
import kotlin.reflect.KClass

/**
 * Test adapter that returns a default-constructed instance of the result class.
 * Useful for verifying proxy wiring without making real AI calls.
 */
class StubAdapter : ApiAdapter {
    override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        return resultClass.java.getDeclaredConstructor().newInstance()
    }
}
