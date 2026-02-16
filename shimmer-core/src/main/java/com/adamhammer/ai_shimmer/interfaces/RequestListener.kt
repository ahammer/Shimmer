package com.adamhammer.ai_shimmer.interfaces

import com.adamhammer.ai_shimmer.model.PromptContext

/**
 * Observes Shimmer request lifecycle events for logging, metrics, and cost tracking.
 *
 * Register via [com.adamhammer.ai_shimmer.ShimmerBuilder.listener].
 * Multiple listeners can be registered and are called in registration order.
 */
interface RequestListener {
    /** Called before the adapter receives a request. */
    fun onRequestStart(context: PromptContext) {}

    /** Called after a successful adapter response. */
    fun onRequestComplete(context: PromptContext, result: Any, durationMs: Long) {}

    /** Called when a request fails (after all retries are exhausted). */
    fun onRequestError(context: PromptContext, error: Exception, durationMs: Long) {}
}
