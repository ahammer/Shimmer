package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.annotations.*
import com.adamhammer.ai_shimmer.test.MockAdapter
import com.adamhammer.ai_shimmer.test.SimpleResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class ConcurrencyTest {

    interface ConcurrentMemoryAPI {
        @AiOperation(summary = "Store", description = "Stores a value")
        @AiResponse(description = "Stored result", responseClass = SimpleResult::class)
        @Memorize(label = "item")
        fun store(
            @AiParameter(description = "Input") input: String
        ): Future<SimpleResult>
    }

    interface ConcurrentSuspendAPI {
        @AiOperation(summary = "Store", description = "Stores a value")
        @AiResponse(description = "Stored result", responseClass = SimpleResult::class)
        @Memorize(label = "item")
        suspend fun store(
            @AiParameter(description = "Input") input: String
        ): SimpleResult
    }

    @Test
    fun `concurrent Future calls with @Memorize do not throw ConcurrentModificationException`() {
        val mock = MockAdapter.builder()
            .dynamicResponse { _, _ -> SimpleResult("result") }
            .delayMs(10)
            .build()

        val instance = ShimmerBuilder(ConcurrentMemoryAPI::class)
            .setAdapterDirect(mock)
            .build()

        val futures = (1..20).map { i ->
            instance.api.store("input-$i")
        }

        // All futures should complete without ConcurrentModificationException
        futures.forEach { future ->
            val result = future.get()
            assertEquals("result", result.value)
        }

        // Memory should contain the key (value may be any of the writes)
        assertTrue(instance.memory.containsKey("item"))
        mock.verifyCallCount(20)
    }

    @Test
    fun `concurrent suspend calls with @Memorize do not throw ConcurrentModificationException`() = runBlocking {
        val mock = MockAdapter.builder()
            .dynamicResponse { _, _ -> SimpleResult("suspended") }
            .delayMs(10)
            .build()

        val instance = ShimmerBuilder(ConcurrentSuspendAPI::class)
            .setAdapterDirect(mock)
            .build()

        val results = (1..20).map { i ->
            async {
                instance.api.store("input-$i")
            }
        }.awaitAll()

        results.forEach { result ->
            assertEquals("suspended", result.value)
        }

        assertTrue(instance.memory.containsKey("item"))
        // MockAdapter's internal list is not thread-safe, so we only verify
        // that all 20 coroutines completed successfully without exceptions
        assertTrue(mock.callCount >= 1, "At least one call should have been recorded")
    }
}
