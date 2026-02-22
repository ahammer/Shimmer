package com.adamhammer.shimmer.adapters

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.AdapterResponse
import com.adamhammer.shimmer.model.PromptContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

/**
 * An [ApiAdapter] decorator that caches responses from the inner adapter
 * based on the deterministic parts of the [PromptContext].
 *
 * Cache hits avoid an API round-trip entirely, returning the stored result
 * and (if available) the original [com.adamhammer.shimmer.model.UsageInfo].
 *
 * **Bypass rules:**
 * - Streaming requests always delegate to the inner adapter (not cacheable).
 * - Tool-calling requests always delegate to the inner adapter (tool results are non-deterministic).
 *
 * @param inner the underlying adapter to delegate cache misses to
 * @param ttlMs time-to-live for cache entries in milliseconds (default: 5 minutes)
 * @param maxEntries maximum number of cached entries; oldest-accessed entries are evicted when exceeded
 */
class CachingAdapter(
    private val inner: ApiAdapter,
    private val ttlMs: Long = 300_000,
    private val maxEntries: Int = 100
) : ApiAdapter {

    private data class CacheEntry(
        val result: Any,
        val usage: com.adamhammer.shimmer.model.UsageInfo?,
        val expiresAt: Long,
        val insertOrder: Long
    )

    private val cache = ConcurrentHashMap<Int, CacheEntry>()
    private val insertCounter = AtomicLong(0)

    private fun cacheKey(context: PromptContext): Int {
        var hash = context.systemInstructions.hashCode()
        hash = 31 * hash + context.methodInvocation.hashCode()
        hash = 31 * hash + context.memory.hashCode()
        hash = 31 * hash + context.conversationHistory.hashCode()
        hash = 31 * hash + context.methodName.hashCode()
        return hash
    }

    private fun evictExpired() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { it.value.expiresAt <= now }
    }

    private fun evictOldestIfNeeded() {
        if (cache.size >= maxEntries) {
            val oldest = cache.entries.minByOrNull { it.value.insertOrder }
            if (oldest != null) cache.remove(oldest.key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Any> getFromCache(key: Int): AdapterResponse<R>? {
        val entry = cache[key] ?: return null
        if (entry.expiresAt <= System.currentTimeMillis()) {
            cache.remove(key)
            return null
        }
        return AdapterResponse(entry.result as R, entry.usage)
    }

    private fun <R : Any> putInCache(key: Int, response: AdapterResponse<R>) {
        evictExpired()
        evictOldestIfNeeded()
        cache[key] = CacheEntry(
            result = response.result,
            usage = response.usage,
            expiresAt = System.currentTimeMillis() + ttlMs,
            insertOrder = insertCounter.getAndIncrement()
        )
    }

    // ── Single-shot requests (cacheable) ────────────────────────────────

    override suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>
    ): R {
        return handleRequestWithUsage(context, resultClass).result
    }

    override suspend fun <R : Any> handleRequestWithUsage(
        context: PromptContext,
        resultClass: KClass<R>
    ): AdapterResponse<R> {
        val key = cacheKey(context)
        getFromCache<R>(key)?.let { return it }
        val response = inner.handleRequestWithUsage(context, resultClass)
        putInCache(key, response)
        return response
    }

    // ── Tool-calling requests (bypass cache — non-deterministic) ────────

    override suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        return inner.handleRequest(context, resultClass, toolProviders)
    }

    override suspend fun <R : Any> handleRequestWithUsage(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): AdapterResponse<R> {
        return inner.handleRequestWithUsage(context, resultClass, toolProviders)
    }

    // ── Streaming requests (bypass cache) ───────────────────────────────

    override fun handleRequestStreaming(context: PromptContext): Flow<String> {
        return inner.handleRequestStreaming(context)
    }

    override fun handleRequestStreaming(
        context: PromptContext,
        toolProviders: List<ToolProvider>
    ): Flow<String> {
        return inner.handleRequestStreaming(context, toolProviders)
    }
}
