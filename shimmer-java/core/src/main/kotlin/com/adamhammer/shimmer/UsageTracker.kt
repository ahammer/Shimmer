package com.adamhammer.shimmer

import com.adamhammer.shimmer.interfaces.RequestListener
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.model.UsageInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Accumulated usage statistics for a single model.
 */
data class ModelUsageStats(
    val model: String,
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val totalCost: Double = 0.0,
    val requestCount: Long = 0
) {
    val totalTokens: Long get() = totalInputTokens + totalOutputTokens
}

/**
 * Tracks token usage and estimated cost across all requests for a Shimmer instance.
 *
 * Automatically registered as a [RequestListener] on every [ShimmerInstance].
 * Thread-safe — safe to read from any thread while requests are in flight.
 */
class UsageTracker : RequestListener {
    private val statsMap = ConcurrentHashMap<String, ModelUsageStats>()
    private val totalRequestCount = AtomicLong(0)

    override fun onRequestComplete(context: PromptContext, result: Any, durationMs: Long, usage: UsageInfo?) {
        if (usage == null) return
        totalRequestCount.incrementAndGet()
        statsMap.compute(usage.model) { _, existing ->
            val base = existing ?: ModelUsageStats(model = usage.model)
            base.copy(
                totalInputTokens = base.totalInputTokens + usage.inputTokens,
                totalOutputTokens = base.totalOutputTokens + usage.outputTokens,
                totalCost = base.totalCost + usage.estimatedCost,
                requestCount = base.requestCount + 1
            )
        }
    }

    /** Returns a snapshot of per-model usage statistics. */
    fun stats(): Map<String, ModelUsageStats> = statsMap.toMap()

    /** Total estimated cost across all models. */
    fun totalCost(): Double = statsMap.values.sumOf { it.totalCost }

    /** Total number of requests tracked. */
    fun totalRequests(): Long = totalRequestCount.get()

    /** Total tokens across all models. */
    fun totalTokens(): Long = statsMap.values.sumOf { it.totalTokens }

    /** Reset all tracked statistics. */
    fun reset() {
        statsMap.clear()
        totalRequestCount.set(0)
    }
}
