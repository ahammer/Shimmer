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
    /** Single-shot request — no tool calling. */
    fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>
    ): R

    /**
     * Request with tool-calling support.
     *
     * Adapters that support multi-turn tool calling override this to implement
     * the iterative loop: send request with tool definitions → receive tool calls
     * → dispatch to [ToolProvider.callTool] → feed results back → repeat until
     * the LLM produces a final response.
     *
     * The default implementation ignores tool providers and delegates to the
     * single-shot [handleRequest].
     */
    fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R = handleRequest(context, resultClass)
}
