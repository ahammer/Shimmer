package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.annotations.Memorize
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.interfaces.Interceptor
import com.adamhammer.ai_shimmer.interfaces.RequestListener
import com.adamhammer.ai_shimmer.interfaces.ToolProvider
import com.adamhammer.ai_shimmer.model.*
import com.adamhammer.ai_shimmer.utils.toJsonString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

class Shimmer<T : Any>(
    private val adapter: ApiAdapter,
    private val contextBuilder: ContextBuilder,
    private val interceptors: List<Interceptor>,
    private val resilience: ResiliencePolicy,
    private val toolProviders: List<ToolProvider> = emptyList(),
    private val memory: MutableMap<String, String>,
    private val klass: KClass<T>,
    private val listeners: List<RequestListener> = emptyList()
) : InvocationHandler {

    private val logger = Logger.getLogger(Shimmer::class.java.name)

    private val concurrencySemaphore: Semaphore? =
        if (resilience.maxConcurrentRequests > 0) Semaphore(resilience.maxConcurrentRequests) else null

    private val rateLimiter: TokenBucketRateLimiter? =
        if (resilience.maxRequestsPerMinute > 0) TokenBucketRateLimiter(resilience.maxRequestsPerMinute) else null

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        // Handle standard Object methods so debuggers, logging, etc. don't throw
        if (method.declaringClass == Object::class.java) {
            return when (method.name) {
                "toString" -> "ShimmerProxy[${klass.simpleName}]"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> method.invoke(this, *(args ?: emptyArray()))
            }
        }

        // Detect suspend functions: last parameter is Continuation<T>
        val isSuspend = args != null && args.isNotEmpty() && args.last() is Continuation<*>

        if (isSuspend) {
            return invokeSuspend(method, args!!)
        }

        // Detect Flow<String> return type for streaming
        if (Flow::class.java.isAssignableFrom(method.returnType)) {
            return invokeStreaming(method, args)
        }

        if (!Future::class.java.isAssignableFrom(method.returnType)) {
            throw UnsupportedOperationException(
                "Method ${method.name} must return Future<T>, Flow<String>, or be a suspend function, " +
                "but returns ${method.returnType.simpleName}. "
            )
        }

        val memorizeKey = method.getAnnotation(Memorize::class.java)?.label

        return CompletableFuture.supplyAsync {
            val genericReturnType = method.genericReturnType
            if (genericReturnType is ParameterizedType) {
                val actualType = genericReturnType.actualTypeArguments[0]
                val clazz = actualType as? Class<*>
                    ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                val kClass = clazz.kotlin

                val request = ShimmerRequest(method, args?.toList(), memory.toMap(), kClass)
                var context = contextBuilder.build(request)

                // Inject available tools from registered tool providers
                if (toolProviders.isNotEmpty()) {
                    val allTools = toolProviders.flatMap { it.listTools() }
                    context = context.copy(availableTools = allTools)
                }

                for (interceptor in interceptors) {
                    context = interceptor.intercept(context)
                }

                val startMs = System.currentTimeMillis()
                val result = executeWithResilience(context, kClass, startMs)

                if (memorizeKey != null) {
                    memory[memorizeKey] = result.toJsonString()
                }

                notifyComplete(context, result, startMs)
                if (result is Future<*>) result.get() else result
            } else {
                throw IllegalStateException(
                    "Return type of method ${method.name} is not parameterized. " +
                    "Expected Future<SomeType> but got ${method.genericReturnType}"
                )
            }
        }
    }

    private fun invokeStreaming(method: Method, args: Array<out Any>?): Flow<String> {
        val request = ShimmerRequest(method, args?.toList(), memory.toMap(), String::class)
        var context = contextBuilder.build(request)

        if (toolProviders.isNotEmpty()) {
            val allTools = toolProviders.flatMap { it.listTools() }
            context = context.copy(availableTools = allTools)
        }

        for (interceptor in interceptors) {
            context = interceptor.intercept(context)
        }

        return adapter.handleRequestStreaming(context)
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokeSuspend(method: Method, args: Array<out Any>): Any? {
        val continuation = args.last() as Continuation<Any?>
        val realArgs = if (args.size > 1) args.sliceArray(0 until args.size - 1) else null
        val memorizeKey = method.getAnnotation(Memorize::class.java)?.label

        // Resolve the result type from the Kotlin function's return type
        val kFunction = method.kotlinFunction
        val resultClass: KClass<*> = if (kFunction != null) {
            kFunction.returnType.classifier as? KClass<*> ?: String::class
        } else {
            String::class
        }

        // Use startCoroutineUninterceptedOrReturn to preserve the caller's
        // coroutine context (Job, dispatcher), enabling structured concurrency
        // and proper cancellation propagation.
        val block: suspend () -> Any? = {
            val request = ShimmerRequest(method, realArgs?.toList(), memory.toMap(), resultClass)
            var context = contextBuilder.build(request)

            if (toolProviders.isNotEmpty()) {
                val allTools = toolProviders.flatMap { it.listTools() }
                context = context.copy(availableTools = allTools)
            }

            for (interceptor in interceptors) {
                context = interceptor.intercept(context)
            }

            val startMs = System.currentTimeMillis()
            val result = executeSuspendWithResilience(context, resultClass, startMs)

            if (memorizeKey != null) {
                memory[memorizeKey] = result.toJsonString()
            }

            notifyComplete(context, result, startMs)
            result
        }

        return block.startCoroutineUninterceptedOrReturn(continuation)
    }

    // ── Blocking resilience (for Future<T> path) ────────────────────────────

    private fun <R : Any> executeWithResilience(context: PromptContext, resultClass: KClass<R>, startMs: Long): R {
        notifyStart(context)
        acquireRateLimit()
        concurrencySemaphore?.acquire()
        try {
            var lastException: Exception? = null
            val maxAttempts = resilience.maxRetries + 1

            for (attempt in 1..maxAttempts) {
                try {
                    val result = executeWithTimeout(adapter, context, resultClass)

                    val validator = resilience.resultValidator
                    if (validator != null && !validator(result)) {
                        throw ResultValidationException("Result validation failed on attempt $attempt")
                    }

                    return result
                } catch (e: Exception) {
                    lastException = e
                    logger.warning { "Attempt $attempt/$maxAttempts failed: ${e.message}" }
                    if (attempt < maxAttempts) {
                        val sleepMs = (resilience.retryDelayMs * resilience.backoffMultiplier.pow(attempt - 1)).toLong()
                        Thread.sleep(sleepMs)
                    }
                }
            }

            val fallback = resilience.fallbackAdapter
            if (fallback != null) {
                logger.info { "Primary adapter exhausted, trying fallback" }
                try {
                    return executeWithTimeout(fallback, context, resultClass)
                } catch (e: Exception) {
                    val ex = ShimmerException("All attempts failed including fallback", e)
                    notifyError(context, ex, startMs)
                    throw ex
                }
            }

            val ex = ShimmerException("All $maxAttempts attempt(s) failed", lastException)
            notifyError(context, ex, startMs)
            throw ex
        } finally {
            concurrencySemaphore?.release()
        }
    }

    private fun <R : Any> executeWithTimeout(adapter: ApiAdapter, context: PromptContext, resultClass: KClass<R>): R {
        if (resilience.timeoutMs <= 0) {
            return if (toolProviders.isNotEmpty()) {
                adapter.handleRequest(context, resultClass, toolProviders)
            } else {
                adapter.handleRequest(context, resultClass)
            }
        }
        val future = CompletableFuture.supplyAsync {
            if (toolProviders.isNotEmpty()) {
                adapter.handleRequest(context, resultClass, toolProviders)
            } else {
                adapter.handleRequest(context, resultClass)
            }
        }
        try {
            return future.get(resilience.timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw ShimmerTimeoutException("Request timed out after ${resilience.timeoutMs}ms", e)
        }
    }

    // ── Suspend-aware resilience (uses cooperative delay, respects cancellation) ──

    private suspend fun <R : Any> executeSuspendWithResilience(
        context: PromptContext,
        resultClass: KClass<R>,
        startMs: Long
    ): R {
        notifyStart(context)
        acquireRateLimit()
        concurrencySemaphore?.acquire()
        try {
            var lastException: Exception? = null
            val maxAttempts = resilience.maxRetries + 1

            for (attempt in 1..maxAttempts) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        executeWithTimeout(adapter, context, resultClass)
                    }

                    val validator = resilience.resultValidator
                    if (validator != null && !validator(result)) {
                        throw ResultValidationException("Result validation failed on attempt $attempt")
                    }

                    return result
                } catch (e: CancellationException) {
                    throw e // Respect coroutine cancellation — never retry
                } catch (e: Exception) {
                    lastException = e
                    logger.warning { "Attempt $attempt/$maxAttempts failed: ${e.message}" }
                    if (attempt < maxAttempts) {
                        val delayMs = (resilience.retryDelayMs * resilience.backoffMultiplier.pow(attempt - 1)).toLong()
                        delay(delayMs)
                    }
                }
            }

            val fallback = resilience.fallbackAdapter
            if (fallback != null) {
                logger.info { "Primary adapter exhausted, trying fallback" }
                try {
                    return withContext(Dispatchers.IO) {
                        executeWithTimeout(fallback, context, resultClass)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val ex = ShimmerException("All attempts failed including fallback", e)
                    notifyError(context, ex, startMs)
                    throw ex
                }
            }

            val ex = ShimmerException("All $maxAttempts attempt(s) failed", lastException)
            notifyError(context, ex, startMs)
            throw ex
        } finally {
            concurrencySemaphore?.release()
        }
    }

    // ── Listener notifications ─────────────────────────────────────────────

    private fun notifyStart(context: PromptContext) {
        listeners.forEach { it.onRequestStart(context) }
    }

    private fun notifyComplete(context: PromptContext, result: Any, startMs: Long) {
        val duration = System.currentTimeMillis() - startMs
        listeners.forEach { it.onRequestComplete(context, result, duration) }
    }

    private fun notifyError(context: PromptContext, error: Exception, startMs: Long) {
        val duration = System.currentTimeMillis() - startMs
        listeners.forEach { it.onRequestError(context, error, duration) }
    }

    // ── Rate limiting ───────────────────────────────────────────────────────

    private fun acquireRateLimit() {
        rateLimiter?.acquire()
    }
}
