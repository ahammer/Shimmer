package com.adamhammer.shimmer.model

import com.adamhammer.shimmer.interfaces.ApiAdapter

/**
 * Configures retry, timeout, fallback, rate limiting, and result validation behavior.
 */
data class ResiliencePolicy(
    val maxRetries: Int = 0,
    val retryDelayMs: Long = 1000,
    val backoffMultiplier: Double = 2.0,
    val timeoutMs: Long = 0,
    val resultValidator: ((Any) -> Boolean)? = null,
    val fallbackAdapter: ApiAdapter? = null,
    /** Maximum concurrent in-flight requests (0 = unlimited). */
    val maxConcurrentRequests: Int = 0,
    /** Maximum requests per minute (0 = unlimited). Enforced via token-bucket. */
    val maxRequestsPerMinute: Int = 0
)
