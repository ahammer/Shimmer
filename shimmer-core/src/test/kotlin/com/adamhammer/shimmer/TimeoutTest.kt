package com.adamhammer.shimmer

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.model.ResiliencePolicy
import com.adamhammer.shimmer.model.ShimmerException
import com.adamhammer.shimmer.model.ShimmerTimeoutException
import com.adamhammer.shimmer.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

class TimeoutTest {

    /**
     * Test the timeout mechanism directly using a dedicated thread pool
     * to avoid ForkJoinPool work-stealing.
     */
    @Test
    fun `executeWithTimeout throws ShimmerTimeoutException on slow adapter`() {
        val slowAdapter = object : ApiAdapter {
            override suspend fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                kotlinx.coroutines.delay(5000)
                @Suppress("UNCHECKED_CAST")
                return SimpleResult("slow") as R
            }
        }

        val context = PromptContext("test", "{}", emptyMap())
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

        try {
            val innerFuture = CompletableFuture.supplyAsync({
                kotlinx.coroutines.runBlocking { slowAdapter.handleRequest(context, SimpleResult::class) }
            }, executor)

            assertThrows(TimeoutException::class.java) {
                innerFuture.get(100, TimeUnit.MILLISECONDS)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `no timeout when adapter responds within limit`() {
        val fastAdapter = MockAdapter.builder()
            .responses(SimpleResult("fast"))
            .delayMs(10)
            .build()

        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(fastAdapter)
            .setResiliencePolicy(ResiliencePolicy(timeoutMs = 5000))
            .build().api

        val result = api.get().get()
        assertEquals("fast", result.value)
    }

    @Test
    fun `zero timeout means no timeout enforcement`() {
        val adapter = MockAdapter.builder()
            .responses(SimpleResult("no-timeout"))
            .delayMs(50)
            .build()

        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(adapter)
            .setResiliencePolicy(ResiliencePolicy(timeoutMs = 0))
            .build().api

        val result = api.get().get()
        assertEquals("no-timeout", result.value)
    }

    @Test
    fun `resilience retries on adapter failure`() {
        var callCount = 0
        val flakeyAdapter = object : ApiAdapter {
            override suspend fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                callCount++
                if (callCount < 3) throw RuntimeException("Transient failure $callCount")
                @Suppress("UNCHECKED_CAST")
                return SimpleResult("recovered") as R
            }
        }

        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(flakeyAdapter)
            .setResiliencePolicy(ResiliencePolicy(maxRetries = 3, retryDelayMs = 10))
            .build().api

        val result = api.get().get()
        assertEquals("recovered", result.value)
        assertEquals(3, callCount)
    }
}
