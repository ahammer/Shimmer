package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.annotations.Memorize
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.interfaces.Interceptor
import com.adamhammer.ai_shimmer.model.*
import com.adamhammer.ai_shimmer.utils.toJsonString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.math.pow
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

class Shimmer<T : Any>(
    private val adapter: ApiAdapter,
    private val contextBuilder: ContextBuilder,
    private val interceptors: List<Interceptor>,
    private val resilience: ResiliencePolicy
) : InvocationHandler {

    private val logger = Logger.getLogger(Shimmer::class.java.name)
    internal lateinit var instance: ShimmerInstance<T>

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        // Handle standard Object methods so debuggers, logging, etc. don't throw
        if (method.declaringClass == Object::class.java) {
            return when (method.name) {
                "toString" -> "ShimmerProxy[${instance.klass.simpleName}]"
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

        if (!Future::class.java.isAssignableFrom(method.returnType)) {
            throw UnsupportedOperationException(
                "Method ${method.name} must return Future<T> or be a suspend function, " +
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

                val request = ShimmerRequest(method, args, instance._memory.toMap(), kClass)
                var context = contextBuilder.build(request)

                for (interceptor in interceptors) {
                    context = interceptor.intercept(context)
                }

                val result = executeWithResilience(context, kClass)

                if (memorizeKey != null) {
                    instance._memory[memorizeKey] = result.toJsonString()
                }

                if (result is Future<*>) result.get() else result
            } else {
                throw IllegalStateException(
                    "Return type of method ${method.name} is not parameterized. " +
                    "Expected Future<SomeType> but got ${method.genericReturnType}"
                )
            }
        }
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = ShimmerRequest(method, realArgs, instance._memory.toMap(), resultClass)
                var context = contextBuilder.build(request)

                for (interceptor in interceptors) {
                    context = interceptor.intercept(context)
                }

                val result = executeWithResilience(context, resultClass)

                if (memorizeKey != null) {
                    instance._memory[memorizeKey] = result.toJsonString()
                }

                continuation.resumeWith(Result.success(result))
            } catch (e: Exception) {
                continuation.resumeWith(Result.failure(e))
            }
        }

        return kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
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
