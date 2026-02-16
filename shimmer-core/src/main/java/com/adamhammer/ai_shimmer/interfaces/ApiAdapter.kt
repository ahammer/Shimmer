package com.adamhammer.ai_shimmer.interfaces

import com.adamhammer.ai_shimmer.model.PromptContext
import kotlin.reflect.KClass

/**
 * Adapter that sends a [PromptContext] to an AI provider and returns a deserialized result.
 *
 * Implementations handle provider-specific concerns: HTTP transport, authentication,
 * prompt formatting, and response deserialization.
 */
interface ApiAdapter {
    fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>
    ): R
}
