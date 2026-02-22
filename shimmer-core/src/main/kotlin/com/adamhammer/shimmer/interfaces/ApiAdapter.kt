package com.adamhammer.shimmer.interfaces

import com.adamhammer.shimmer.model.PromptContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass

/**
 * Adapter that sends a [PromptContext] to an AI provider and returns a deserialized result.
 *
 * Implementations handle provider-specific concerns: HTTP transport, authentication,
 * prompt formatting, and response deserialization.
 */
interface ApiAdapter {
    /** Single-shot request — no tool calling. */
    suspend fun <R : Any> handleRequest(
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
    suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R = handleRequest(context, resultClass)

    /**
     * Streaming request — returns tokens as they arrive.
     *
     * Override to provide token-by-token streaming from the AI provider.
     * The default implementation falls back to the non-streaming [handleRequest],
     * emitting the full response as a single element.
     */
    fun handleRequestStreaming(
        context: PromptContext
    ): Flow<String> = kotlinx.coroutines.flow.flow { emit(handleRequest(context, String::class)) }

    /**
     * Streaming request with tool-calling support.
     *
     * Adapters that support multi-turn tool calling during streaming override
     * this to accumulate tool calls from the stream, dispatch them, feed
     * results back, and continue streaming until a final text response.
     *
     * The default implementation ignores tool providers and delegates to the
     * no-tool [handleRequestStreaming].
     */
    fun handleRequestStreaming(
        context: PromptContext,
        toolProviders: List<ToolProvider>
    ): Flow<String> = handleRequestStreaming(context)
}
