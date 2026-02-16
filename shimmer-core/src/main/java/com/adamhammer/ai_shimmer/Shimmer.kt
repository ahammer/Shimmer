package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.annotations.Memorize
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.interfaces.Interceptor
import com.adamhammer.ai_shimmer.model.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import kotlin.math.pow
import kotlin.reflect.KClass

class Shimmer<T : Any>(
    private val adapter: ApiAdapter,
    private val contextBuilder: ContextBuilder,
    private val interceptors: List<Interceptor>,
    private val resilience: ResiliencePolicy
) : InvocationHandler {

    private val logger = Logger.getLogger(Shimmer::class.java.name)
    internal lateinit var instance: ShimmerInstance<T>

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        if (!Future::class.java.isAssignableFrom(method.returnType)) {
            throw UnsupportedOperationException("Method ${method.name} is not supported by the proxy")
        }

        val memorizeKey = method.getAnnotation(Memorize::class.java)?.label

        return CompletableFuture.supplyAsync {
            val genericReturnType = method.genericReturnType
            if (genericReturnType is ParameterizedType) {
                val actualType = genericReturnType.actualTypeArguments[0]
                val clazz = actualType as? Class<*>
                    ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                val kClass = clazz.kotlin

                val request = ShimmerRequest(method, args, instance.memory, kClass)
                var context = contextBuilder.build(request)

                for (interceptor in interceptors) {
                    context = interceptor.intercept(context)
                }

                val result = executeWithResilience(context, kClass)

                if (memorizeKey != null) {
                    instance.memory[memorizeKey] = result.toString()
                }

                if (result is Future<*>) result.get() else result
            } else {
                throw IllegalStateException("Return type of method ${method.name} is not parameterized")
            }
        }
    }

    private fun <R : Any> executeWithResilience(context: PromptContext, resultClass: KClass<R>): R {
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
                    val delay = (resilience.retryDelayMs * resilience.backoffMultiplier.pow(attempt - 1)).toLong()
                    Thread.sleep(delay)
                }
            }
        }

        val fallback = resilience.fallbackAdapter
        if (fallback != null) {
            logger.info { "Primary adapter exhausted, trying fallback" }
            try {
                return executeWithTimeout(fallback, context, resultClass)
            } catch (e: Exception) {
                throw ShimmerException("All attempts failed including fallback", e)
            }
        }

        throw ShimmerException("All $maxAttempts attempt(s) failed", lastException)
    }

    private fun <R : Any> executeWithTimeout(adapter: ApiAdapter, context: PromptContext, resultClass: KClass<R>): R {
        if (resilience.timeoutMs <= 0) {
            return adapter.handleRequest(context, resultClass)
        }
        val future = CompletableFuture.supplyAsync { adapter.handleRequest(context, resultClass) }
        try {
            return future.get(resilience.timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw ShimmerTimeoutException("Request timed out after ${resilience.timeoutMs}ms", e)
        }
    }
}
