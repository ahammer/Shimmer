package com.adamhammer.ai_shimmer.model

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter

/**
 * Configures retry, timeout, fallback, and result validation behavior.
 */
data class ResiliencePolicy(
    val maxRetries: Int = 0,
    val retryDelayMs: Long = 1000,
    val backoffMultiplier: Double = 2.0,
    val timeoutMs: Long = 0,
    val resultValidator: ((Any) -> Boolean)? = null,
    val fallbackAdapter: ApiAdapter? = null
)
