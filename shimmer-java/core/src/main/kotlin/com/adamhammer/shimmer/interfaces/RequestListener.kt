package com.adamhammer.shimmer.interfaces

import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.model.UsageInfo

/**
 * Observes Shimmer request lifecycle events for logging, metrics, and cost tracking.
 *
 * Register via [com.adamhammer.shimmer.ShimmerBuilder.listener].
 * Multiple listeners can be registered and are called in registration order.
 */
interface RequestListener {
    /** Called before the adapter receives a request. */
    fun onRequestStart(context: PromptContext) {}

    /** Called after a successful adapter response. */
    fun onRequestComplete(context: PromptContext, result: Any, durationMs: Long, usage: UsageInfo? = null) {}

    /** Called when a request fails (after all retries are exhausted). */
    fun onRequestError(context: PromptContext, error: Exception, durationMs: Long) {}
}
