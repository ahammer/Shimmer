package com.adamhammer.shimmer

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.RequestListener
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.logging.Logger
import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * Executes adapter calls with retry, timeout, fallback, and rate-limiting logic.
 */
class ResilienceExecutor(
    private val resilience: ResiliencePolicy,
    private val listeners: List<RequestListener>
) {
    private val logger = Logger.getLogger(ResilienceExecutor::class.java.name)

    /**
     * Run the adapter call with resilience.
     *
     * @param rethrowCancellation when true, [CancellationException] is immediately re-thrown
     */
    suspend fun <R : Any> execute(
        adapter: ApiAdapter,
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        notifyStart(context)
        val startMs = System.currentTimeMillis()
        var lastException: Exception? = null
        val maxAttempts = resilience.maxRetries + 1
        var currentContext = context

        for (attempt in 1..maxAttempts) {
            try {
                val result = executeWithTimeout(adapter, currentContext, resultClass, toolProviders)

                val validator = resilience.resultValidator
                if (validator != null) {
                    val validationResult = validator(result)
                    if (validationResult is ValidationResult.Invalid) {
                        throw ResultValidationException(validationResult.reason, currentContext)
                    } else if (validationResult == false) { // For backwards compatibility if validator returns Boolean
                        throw ResultValidationException("Result validation failed on attempt $attempt", currentContext)
                    }
                }

                notifyComplete(currentContext, result, startMs)
                return result
            } catch (e: CancellationException) {
                throw e // Always rethrow CancellationException to respect structured concurrency
            } catch (e: ResultValidationException) {
                lastException = e
                logger.warning { "Attempt $attempt/$maxAttempts failed: ${e.message}" }
                
                currentContext = currentContext.copy(
                    conversationHistory = currentContext.conversationHistory + Message(
                        role = MessageRole.USER,
                        content = "Validation failed: ${e.message}. Please correct your response."
                    )
                )
                
                if (attempt < maxAttempts) {
                    val delayMs = (resilience.retryDelayMs * resilience.backoffMultiplier.pow(attempt - 1)).toLong()
                    kotlinx.coroutines.delay(delayMs)
                }
            } catch (e: Exception) {
                lastException = e
                logger.warning { "Attempt $attempt/$maxAttempts failed: ${e.message}" }
                
                if (attempt < maxAttempts) {
                    val delayMs = (resilience.retryDelayMs * resilience.backoffMultiplier.pow(attempt - 1)).toLong()
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }

        val fallback = resilience.fallbackAdapter
        if (fallback != null) {
            logger.info { "Primary adapter exhausted, trying fallback" }
            try {
                val result = executeWithTimeout(fallback, currentContext, resultClass, toolProviders)
                notifyComplete(currentContext, result, startMs)
                return result
            } catch (e: Exception) {
                val ex = ShimmerException("All attempts failed including fallback", e, currentContext)
                notifyError(currentContext, ex, startMs)
                throw ex
            }
        }

        val ex = ShimmerException("All $maxAttempts attempt(s) failed", lastException, currentContext)
        notifyError(currentContext, ex, startMs)
        throw ex
    }

    private suspend fun <R : Any> executeWithTimeout(
        adapter: ApiAdapter,
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        if (resilience.timeoutMs <= 0) {
            return callAdapter(adapter, context, resultClass, toolProviders)
        }
        return kotlinx.coroutines.withTimeoutOrNull(resilience.timeoutMs) {
            callAdapter(adapter, context, resultClass, toolProviders)
        } ?: throw ShimmerTimeoutException("Request timed out after ${resilience.timeoutMs}ms", null, context)
    }

    private suspend fun <R : Any> callAdapter(
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
