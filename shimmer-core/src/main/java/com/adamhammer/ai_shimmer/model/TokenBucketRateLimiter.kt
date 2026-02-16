package com.adamhammer.ai_shimmer.model

/**
 * Simple token-bucket rate limiter that allows at most [maxPerMinute] acquisitions
 * per rolling 60-second window. Blocks the calling thread until a token is available.
 */
class TokenBucketRateLimiter(private val maxPerMinute: Int) {

    private val timestamps = ArrayDeque<Long>()

    @Synchronized
    fun acquire() {
        while (true) {
            val now = System.currentTimeMillis()
            // Evict timestamps older than 60 seconds
            while (timestamps.isNotEmpty() && now - timestamps.first() > 60_000) {
                timestamps.removeFirst()
            }
            if (timestamps.size < maxPerMinute) {
                timestamps.addLast(now)
                return
            }
            // Wait until the oldest token expires
            val waitMs = 60_000 - (now - timestamps.first()) + 1
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as java.lang.Object).wait(waitMs)
        }
    }
}
