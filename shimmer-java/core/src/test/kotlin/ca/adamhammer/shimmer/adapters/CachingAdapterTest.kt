package ca.adamhammer.shimmer.adapters

import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.test.MockAdapter
import ca.adamhammer.shimmer.test.SimpleResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CachingAdapterTest {

    private fun context(method: String = "test", invocation: String = "{}"): PromptContext =
        PromptContext(
            systemInstructions = "system",
            methodInvocation = invocation,
            memory = emptyMap(),
            methodName = method
        )

    @Test
    fun `cache hit returns cached result without calling inner adapter again`() = runBlocking {
        val inner = MockAdapter.scripted(SimpleResult("cached"))
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val ctx = context()
        val first = caching.handleRequest(ctx, SimpleResult::class)
        val second = caching.handleRequest(ctx, SimpleResult::class)

        assertEquals("cached", first.value)
        assertEquals("cached", second.value)
        inner.verifyCallCount(1)
    }

    @Test
    fun `different contexts produce cache misses`() = runBlocking {
        val inner = MockAdapter.scripted(SimpleResult("a"), SimpleResult("b"))
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val r1 = caching.handleRequest(context(invocation = "{\"x\":1}"), SimpleResult::class)
        val r2 = caching.handleRequest(context(invocation = "{\"x\":2}"), SimpleResult::class)

        assertEquals("a", r1.value)
        assertEquals("b", r2.value)
        inner.verifyCallCount(2)
    }

    @Test
    fun `expired entries produce cache miss`() = runBlocking {
        val inner = MockAdapter.scripted(SimpleResult("first"), SimpleResult("second"))
        val caching = CachingAdapter(inner, ttlMs = 1) // 1ms TTL

        val ctx = context()
        caching.handleRequest(ctx, SimpleResult::class)

        // Wait for TTL to expire
        kotlinx.coroutines.delay(10)

        val second = caching.handleRequest(ctx, SimpleResult::class)
        assertEquals("second", second.value)
        inner.verifyCallCount(2)
    }

    @Test
    fun `handleRequestWithUsage caches and returns usage info`() = runBlocking {
        val inner = MockAdapter.scripted(SimpleResult("with-usage"))
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val ctx = context()
        val first = caching.handleRequestWithUsage(ctx, SimpleResult::class)
        val second = caching.handleRequestWithUsage(ctx, SimpleResult::class)

        assertEquals("with-usage", first.result.value)
        assertEquals("with-usage", second.result.value)
        inner.verifyCallCount(1)
    }

    @Test
    fun `streaming always delegates to inner adapter`() = runBlocking {
        val inner = MockAdapter.scripted("stream-result", "stream-result-2")
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val ctx = context()
        val first = caching.handleRequestStreaming(ctx).toList()
        val second = caching.handleRequestStreaming(ctx).toList()

        assertEquals(listOf("stream-result"), first)
        assertEquals(listOf("stream-result-2"), second)
        assertEquals(2, inner.callCount)
    }

    @Test
    fun `tool-calling requests bypass cache`() = runBlocking {
        val inner = MockAdapter.scripted(SimpleResult("tool-a"), SimpleResult("tool-b"))
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val ctx = context()
        val r1 = caching.handleRequest(ctx, SimpleResult::class, emptyList())
        val r2 = caching.handleRequest(ctx, SimpleResult::class, emptyList())

        assertEquals("tool-a", r1.value)
        assertEquals("tool-b", r2.value)
        inner.verifyCallCount(2)
    }

    @Test
    fun `maxEntries evicts old entries when exceeded`() = runBlocking {
        val inner = MockAdapter.scripted(
            SimpleResult("a"), SimpleResult("b"), SimpleResult("c"), SimpleResult("a2")
        )
        val caching = CachingAdapter(inner, ttlMs = 60_000, maxEntries = 2)

        // Fill cache with 2 entries
        caching.handleRequest(context(invocation = "1"), SimpleResult::class)
        caching.handleRequest(context(invocation = "2"), SimpleResult::class)
        // Evicts entry 1
        caching.handleRequest(context(invocation = "3"), SimpleResult::class)
        // Entry 1 should be evicted, causes a cache miss
        val r = caching.handleRequest(context(invocation = "1"), SimpleResult::class)

        assertEquals("a2", r.value)
        inner.verifyCallCount(4)
    }

    @Test
    fun `cache works with String result class`() = runBlocking {
        val inner = MockAdapter.scripted("hello-world")
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val ctx = context()
        val first = caching.handleRequest(ctx, String::class)
        val second = caching.handleRequest(ctx, String::class)

        assertEquals("hello-world", first)
        assertEquals("hello-world", second)
        inner.verifyCallCount(1)
    }

    @Test
    fun `memory differences produce different cache keys`() = runBlocking {
        val inner = MockAdapter.scripted(SimpleResult("no-mem"), SimpleResult("with-mem"))
        val caching = CachingAdapter(inner, ttlMs = 60_000)

        val ctx1 = context().copy(memory = emptyMap())
        val ctx2 = context().copy(memory = mapOf("key" to "val"))

        val r1 = caching.handleRequest(ctx1, SimpleResult::class)
        val r2 = caching.handleRequest(ctx2, SimpleResult::class)

        assertEquals("no-mem", r1.value)
        assertEquals("with-mem", r2.value)
        inner.verifyCallCount(2)
    }
}
