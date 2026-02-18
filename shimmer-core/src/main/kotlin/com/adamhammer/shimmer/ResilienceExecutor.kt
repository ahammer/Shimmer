package com.adamhammer.shimmer

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.RequestListener
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * Executes adapter calls with retry, timeout, fallback, and rate-limiting logic.
 *
 * Accepts strategy lambdas for sleeping/delaying so the same core loop handles
 * both the blocking ([Thread.sleep]) and suspend ([delay]) paths.
 */
class ResilienceExecutor(
    private val resilience: ResiliencePolicy,
    private val listeners: List<RequestListener>
) {
    private val logger = Logger.getLogger(ResilienceExecutor::class.java.name)

    /**
     * Run the adapter call with resilience. The [sleepStrategy] is called between retries.
     * Pass `{ ms -> Thread.sleep(ms) }` for the blocking path or `{ ms -> delay(ms) }` for the suspend path.
     *
     * @param rethrowCancellation when true, [CancellationException] is immediately re-thrown
     */
    fun <R : Any> execute(
        adapter: ApiAdapter,
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>,
        sleepStrategy: (Long) -> Unit,
        rethrowCancellation: Boolean = false
    ): R {
        notifyStart(context)
        val startMs = System.currentTimeMillis()
        var lastException: Exception? = null
        val maxAttempts = resilience.maxRetries + 1

        for (attempt in 1..maxAttempts) {
            try {
                val result = executeWithTimeout(adapter, context, resultClass, toolProviders)

                val validator = resilience.resultValidator
                if (validator != null && !validator(result)) {
                    throw ResultValidationException("Result validation failed on attempt $attempt")
                }

                notifyComplete(context, result, startMs)
                return result
            } catch (e: CancellationException) {
                if (rethrowCancellation) throw e
                lastException = e
            } catch (e: Exception) {
                lastException = e
                logger.warning { "Attempt $attempt/$maxAttempts failed: ${e.message}" }
                if (attempt < maxAttempts) {
                    val delayMs = (resilience.retryDelayMs * resilience.backoffMultiplier.pow(attempt - 1)).toLong()
                    sleepStrategy(delayMs)
                }
            }
        }

        val fallback = resilience.fallbackAdapter
        if (fallback != null) {
            logger.info { "Primary adapter exhausted, trying fallback" }
            try {
                val result = executeWithTimeout(fallback, context, resultClass, toolProviders)
                notifyComplete(context, result, startMs)
                return result
            } catch (e: Exception) {
                val ex = ShimmerException("All attempts failed including fallback", e)
                notifyError(context, ex, startMs)
                throw ex
            }
        }

        val ex = ShimmerException("All $maxAttempts attempt(s) failed", lastException)
        notifyError(context, ex, startMs)
        throw ex
    }

    private fun <R : Any> executeWithTimeout(
        adapter: ApiAdapter,
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        if (resilience.timeoutMs <= 0) {
            return callAdapter(adapter, context, resultClass, toolProviders)
        }
        val future = CompletableFuture.supplyAsync {
            callAdapter(adapter, context, resultClass, toolProviders)
        }
        try {
            return future.get(resilience.timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw ShimmerTimeoutException("Request timed out after ${resilience.timeoutMs}ms", e)
        }
    }

    private fun <R : Any> callAdapter(
        adapter: ApiAdapter,
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R = if (toolProviders.isNotEmpty()) {
        adapter.handleRequest(context, resultClass, toolProviders)
    } else {
        adapter.handleRequest(context, resultClass)
    }

    fun notifyStart(context: PromptContext) {
        listeners.forEach { it.onRequestStart(context) }
    }

    fun notifyComplete(context: PromptContext, result: Any, startMs: Long) {
        val duration = System.currentTimeMillis() - startMs
        listeners.forEach { it.onRequestComplete(context, result, duration) }
    }

    fun notifyError(context: PromptContext, error: Exception, startMs: Long) {
        val duration = System.currentTimeMillis() - startMs
        listeners.forEach { it.onRequestError(context, error, duration) }
    }
}
