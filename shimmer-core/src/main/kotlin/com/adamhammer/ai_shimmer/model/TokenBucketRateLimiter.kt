package com.adamhammer.ai_shimmer.model

/**
 * Simple token-bucket rate limiter that allows at most [maxPerMinute] acquisitions
 * per rolling [windowMs] window. Blocks the calling thread until a token is available.
 */
class TokenBucketRateLimiter(
    private val maxPerMinute: Int,
    private val windowMs: Long = 60_000
) {

    private val timestamps = ArrayDeque<Long>()

    @Synchronized
    fun acquire() {
        while (true) {
            val now = System.currentTimeMillis()
            // Evict timestamps older than the window
            while (timestamps.isNotEmpty() && now - timestamps.first() > windowMs) {
                timestamps.removeFirst()
            }
            if (timestamps.size < maxPerMinute) {
                timestamps.addLast(now)
                return
            }
            // Wait until the oldest token expires, then notify other waiters
            val waitMs = windowMs - (now - timestamps.first()) + 1
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as java.lang.Object).wait(waitMs)
            // After waking, notify other blocked threads so they can re-check
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as java.lang.Object).notifyAll()
        }
    }
}
