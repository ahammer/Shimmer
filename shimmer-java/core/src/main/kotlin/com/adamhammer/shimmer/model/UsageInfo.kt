package com.adamhammer.shimmer.model

/**
 * Token usage and cost metadata from a single adapter call.
 *
 * Populated by vendor adapters that have access to the provider's usage response fields.
 * When pricing is not configured, cost fields default to zero.
 */
data class UsageInfo(
    val model: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val inputCostPerToken: Double = 0.0,
    val outputCostPerToken: Double = 0.0
) {
    val totalTokens: Long get() = inputTokens + outputTokens
    val estimatedCost: Double get() = inputTokens * inputCostPerToken + outputTokens * outputCostPerToken
}
