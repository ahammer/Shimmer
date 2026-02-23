package ca.adamhammer.shimmer.model

import kotlinx.coroutines.delay

/**
 * Simple token-bucket rate limiter that allows at most [maxPerMinute] acquisitions
 * per rolling [windowMs] window.
 *
 * Provides both a blocking [acquire] (for the `Future<T>` path) and a
 * cooperative [acquireSuspend] (for the suspend/coroutine path) so the
 * caller's dispatcher is never blocked unnecessarily.
 */
class TokenBucketRateLimiter(
    private val maxPerMinute: Int,
    private val windowMs: Long = 60_000
) {

    private val timestamps = ArrayDeque<Long>()

    /** Blocking acquire — waits via [Object.wait] until a token is available. */
    @Synchronized
    fun acquire() {
        while (true) {
            val waitMs = tryAcquireOrGetWaitTime()
            if (waitMs == 0L) return
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as java.lang.Object).wait(waitMs)
        }
    }

    /** Suspend-friendly acquire — uses cooperative [delay] instead of blocking. */
    suspend fun acquireSuspend() {
        while (true) {
            val waitMs = tryAcquireOrGetWaitTime()
            if (waitMs == 0L) return
            delay(waitMs)
        }
    }

    /**
     * Attempts to acquire a token. Returns 0 on success, or the number of
     * milliseconds the caller should wait before retrying.
     */
    @Synchronized
    private fun tryAcquireOrGetWaitTime(): Long {
        val now = System.currentTimeMillis()
        while (timestamps.isNotEmpty() && now - timestamps.first() > windowMs) {
            timestamps.removeFirst()
        }
        if (timestamps.size < maxPerMinute) {
            timestamps.addLast(now)
            return 0
        }
        return windowMs - (now - timestamps.first()) + 1
    }
}
