package com.adamhammer.shimmer.model

import com.adamhammer.shimmer.interfaces.ApiAdapter

/** Represents the outcome of a result validation check. */
sealed class ValidationResult {
    /** The result is valid and should be returned to the caller. */
    object Valid : ValidationResult()
    /** The result is invalid and should trigger a retry (if configured). */
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * Configures retry, timeout, fallback, rate limiting, and result validation behavior.
 *
 * @param maxRetries Maximum number of retries for transient failures or validation rejections.
 * @param retryDelayMs Initial delay between retries in milliseconds.
 * @param backoffMultiplier Multiplier applied to the delay after each retry.
 * @param timeoutMs Maximum time allowed for a single request in milliseconds (0 = unlimited).
 * @param resultValidator A function that validates the deserialized result. Can return a `Boolean` or a [ValidationResult].
 * @param fallbackAdapter An alternative adapter to use if the primary adapter fails after all retries.
 * @param maxConcurrentRequests Maximum concurrent in-flight requests (0 = unlimited).
 * @param maxRequestsPerMinute Maximum requests per minute (0 = unlimited). Enforced via token-bucket.
 */
data class ResiliencePolicy(
    val maxRetries: Int = 0,
    val retryDelayMs: Long = 1000,
    val backoffMultiplier: Double = 2.0,
    val timeoutMs: Long = 0,
    val resultValidator: ((Any) -> Any)? = null, // Can return Boolean or ValidationResult
    val fallbackAdapter: ApiAdapter? = null,
    val maxConcurrentRequests: Int = 0,
    val maxRequestsPerMinute: Int = 0
)
