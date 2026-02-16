package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.model.TokenBucketRateLimiter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class TokenBucketRateLimiterTest {

    @Test
    fun `allows requests up to the limit`() {
        val limiter = TokenBucketRateLimiter(10)
        // Should not block for 10 quick calls
        repeat(10) { limiter.acquire() }
    }

    @Test
    fun `blocks when limit is exceeded`() {
        val limiter = TokenBucketRateLimiter(2)
        limiter.acquire()
        limiter.acquire()

        // Third acquire should block (or at least wait)
        val acquired = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val t = thread {
            latch.countDown()
            limiter.acquire() // should block
            acquired.incrementAndGet()
        }

        latch.await()
        Thread.sleep(100) // give the thread time to attempt acquire
        assertEquals(0, acquired.get(), "Third acquire should be blocked")

        t.interrupt() // unblock the thread
        t.join(1000)
    }

    @Test
    fun `concurrent acquires respect the limit`() {
        val limiter = TokenBucketRateLimiter(5)
        val acquired = AtomicInteger(0)
        val threads = (1..5).map {
            thread {
                limiter.acquire()
                acquired.incrementAndGet()
            }
        }
        threads.forEach { it.join(2000) }
        assertEquals(5, acquired.get())
    }
}
