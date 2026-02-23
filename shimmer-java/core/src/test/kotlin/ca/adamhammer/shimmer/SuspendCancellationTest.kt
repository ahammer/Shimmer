package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.test.MockAdapter
import ca.adamhammer.shimmer.test.SimpleResult
import ca.adamhammer.shimmer.test.SuspendTestAPI
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SuspendCancellationTest {

    @Test
    fun `cancelled coroutine does not retry`() = runBlocking {
        val mock = MockAdapter.builder()
            .responses(SimpleResult("ok"))
            .delayMs(500) // delay so cancellation has time to fire
            .build()

        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .resilience {
                maxRetries = 5
                retryDelayMs = 10
            }
            .build().api

        val job = launch {
            api.get()
        }

        delay(50) // let the first attempt start
        job.cancelAndJoin()

        assertTrue(job.isCancelled, "Job should be cancelled")
        // Should have at most 1 attempt (the one that was running when cancelled)
        assertTrue(mock.callCount <= 1, "Expected at most 1 call, got ${mock.callCount}")
    }

    @Test
    fun `suspend function respects parent job cancellation`() {
        val mock = MockAdapter.builder()
            .responses(SimpleResult("ok"))
            .delayMs(1000)
            .build()

        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        assertThrows(CancellationException::class.java) {
            runBlocking {
                withTimeout(50) {
                    api.get()
                }
            }
        }
    }
}
