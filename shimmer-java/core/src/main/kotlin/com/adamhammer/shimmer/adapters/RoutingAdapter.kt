package com.adamhammer.shimmer.adapters

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.AdapterResponse
import com.adamhammer.shimmer.model.PromptContext
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * An [ApiAdapter] that routes requests to different underlying adapters based on the [PromptContext].
 *
 * This is useful for mixed-vendor solutions, where you might want to use one provider
 * for text completions (e.g., Anthropic) and another for image generation (e.g., OpenAI),
 * or route based on the method name or custom properties injected by interceptors.
 *
 * @param router A function that takes a [PromptContext] and returns the [ApiAdapter] to use.
 */
class RoutingAdapter(
    private val router: (PromptContext) -> ApiAdapter
) : ApiAdapter {

    override suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>
    ): R {
        return router(context).handleRequest(context, resultClass)
    }

    override suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        return router(context).handleRequest(context, resultClass, toolProviders)
    }

    override suspend fun <R : Any> handleRequestWithUsage(
        context: PromptContext,
        resultClass: KClass<R>
    ): AdapterResponse<R> {
        return router(context).handleRequestWithUsage(context, resultClass)
    }

    override suspend fun <R : Any> handleRequestWithUsage(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): AdapterResponse<R> {
        return router(context).handleRequestWithUsage(context, resultClass, toolProviders)
    }

    override fun handleRequestStreaming(
        context: PromptContext
    ): Flow<String> {
        return router(context).handleRequestStreaming(context)
    }

    override fun handleRequestStreaming(
        context: PromptContext,
        toolProviders: List<ToolProvider>
    ): Flow<String> {
        return router(context).handleRequestStreaming(context, toolProviders)
    }
}
