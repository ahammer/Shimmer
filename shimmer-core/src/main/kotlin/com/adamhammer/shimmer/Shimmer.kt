package com.adamhammer.shimmer

import com.adamhammer.shimmer.annotations.Memorize
import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.ContextBuilder
import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.interfaces.RequestListener
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.*
import com.adamhammer.shimmer.utils.toJsonString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.logging.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

class Shimmer(
    private val adapter: ApiAdapter,
    private val contextBuilder: ContextBuilder,
    private val interceptors: List<Interceptor>,
    resilience: ResiliencePolicy,
    private val toolProviders: List<ToolProvider> = emptyList(),
    private val memory: MutableMap<String, String>,
    private val klass: KClass<*>,
    listeners: List<RequestListener> = emptyList()
) : InvocationHandler {

    private val resilienceExecutor = ResilienceExecutor(resilience, listeners)

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

        return invokeFuture(method, args)
    }

    private fun invokeFuture(method: Method, args: Array<out Any>?): CompletableFuture<*> {
        val memorizeKey = method.getAnnotation(Memorize::class.java)?.label

        return CompletableFuture.supplyAsync {
            val genericReturnType = method.genericReturnType
            if (genericReturnType is ParameterizedType) {
                val actualType = genericReturnType.actualTypeArguments[0]
                val clazz = actualType as? Class<*>
                    ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                val kClass = clazz.kotlin
                val context = buildContext(method, args?.toList(), kClass)

                rateLimiter?.acquire()
                concurrencySemaphore?.acquire()
                try {
                    val result = resilienceExecutor.execute(
                        adapter, context, kClass, toolProviders,
                        sleepStrategy = { ms -> Thread.sleep(ms) }
                    )
                    if (memorizeKey != null) memory[memorizeKey] = result.toJsonString()
                    result
                } finally {
                    concurrencySemaphore?.release()
                }
            } else {
                throw IllegalStateException(
                    "Return type of method ${method.name} is not parameterized. " +
                    "Expected Future<SomeType> but got ${method.genericReturnType}"
                )
            }
        }
    }

    private fun invokeStreaming(method: Method, args: Array<out Any>?): Flow<String> {
        val context = buildContext(method, args?.toList(), String::class)
        return adapter.handleRequestStreaming(context, toolProviders)
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

        val block: suspend () -> Any? = {
            val context = buildContext(method, realArgs?.toList(), resultClass)

            rateLimiter?.acquireSuspend()
            withContext(Dispatchers.IO) {
                concurrencySemaphore?.acquire()
                try {
                    val result = resilienceExecutor.execute(
                        adapter, context, resultClass, toolProviders,
                        sleepStrategy = { ms -> Thread.sleep(ms) },
                        rethrowCancellation = true
                    )
                    if (memorizeKey != null) memory[memorizeKey] = result.toJsonString()
                    result
                } finally {
                    concurrencySemaphore?.release()
                }
            }
        }

        return block.startCoroutineUninterceptedOrReturn(continuation)
    }

    private fun buildContext(method: Method, args: List<Any>?, resultClass: KClass<*>): PromptContext {
        val descriptor = MethodDescriptor.from(method, args, resultClass)
        val request = ShimmerRequest(descriptor, memory.toMap(), resultClass)
        var context = contextBuilder.build(request)
        if (toolProviders.isNotEmpty()) {
            val allTools = toolProviders.flatMap { it.listTools() }
            context = context.copy(availableTools = allTools)
        }
        for (interceptor in interceptors) {
            context = interceptor.intercept(context)
        }
        return context
    }
}
