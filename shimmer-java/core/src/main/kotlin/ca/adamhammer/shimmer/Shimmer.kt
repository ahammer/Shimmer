package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.annotations.Memorize
import ca.adamhammer.shimmer.interfaces.ApiAdapter
import ca.adamhammer.shimmer.interfaces.ContextBuilder
import ca.adamhammer.shimmer.interfaces.Interceptor
import ca.adamhammer.shimmer.interfaces.MemoryStore
import ca.adamhammer.shimmer.interfaces.RequestListener
import ca.adamhammer.shimmer.interfaces.ToolProvider
import ca.adamhammer.shimmer.model.*
import ca.adamhammer.shimmer.utils.toJsonString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.future
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

/**
 * The core dynamic proxy invocation handler for Shimmer interfaces.
 *
 * Intercepts method calls on the proxy interface, builds a [PromptContext] using the
 * configured [ContextBuilder] and [Interceptor]s, and delegates the request to the
 * configured [ApiAdapter] via the [ResilienceExecutor].
 *
 * Supports `suspend` functions, `Future<T>`, and `Flow<String>` return types.
 */
class Shimmer(
    private val adapter: ApiAdapter,
    private val contextBuilder: ContextBuilder,
    private val interceptors: List<Interceptor>,
    resilience: ResiliencePolicy,
    private val toolProviders: List<ToolProvider> = emptyList(),
    private val memoryStore: MemoryStore,
    private val klass: KClass<*>,
    listeners: List<RequestListener> = emptyList(),
    private val typeAdapterRegistry: TypeAdapterRegistry = TypeAdapterRegistry()
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

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun invokeFuture(method: Method, args: Array<out Any>?): CompletableFuture<*> {
        val memorizeKey = method.getAnnotation(Memorize::class.java)?.label

        return GlobalScope.future(Dispatchers.IO) {
            val genericReturnType = method.genericReturnType
            if (genericReturnType is ParameterizedType) {
                val actualType = genericReturnType.actualTypeArguments[0]
                val clazz = actualType as? Class<*>
                    ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                val kClass = clazz.kotlin
                val mirrorClass = typeAdapterRegistry.mirrorClassFor(kClass)
                val bridgedArgs = args?.map { typeAdapterRegistry.convertArg(it) }?.toTypedArray()
                val context = buildContext(method, bridgedArgs?.toList(), mirrorClass)

                rateLimiter?.acquireSuspend()
                concurrencySemaphore?.acquire()
                try {
                    val result = resilienceExecutor.execute(
                        adapter, context, mirrorClass, toolProviders
                    )
                    if (memorizeKey != null) memoryStore.put(memorizeKey, result.toJsonString())
                    typeAdapterRegistry.convertResult(result, kClass)
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
        val realArgs = args?.toList()
        return kotlinx.coroutines.flow.flow {
            val context = buildContext(method, realArgs, String::class)
            adapter.handleRequestStreaming(context, toolProviders).collect { emit(it) }
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
        val mirrorClass = typeAdapterRegistry.mirrorClassFor(resultClass)
        val bridgedArgs = realArgs?.map { typeAdapterRegistry.convertArg(it) }?.toTypedArray()

        val block: suspend () -> Any? = {
            val context = buildContext(method, bridgedArgs?.toList(), mirrorClass)

            rateLimiter?.acquireSuspend()
            concurrencySemaphore?.acquire()
            try {
                val result = resilienceExecutor.execute(
                    adapter, context, mirrorClass, toolProviders
                )
                if (memorizeKey != null) memoryStore.put(memorizeKey, result.toJsonString())
                typeAdapterRegistry.convertResult(result, resultClass)
            } finally {
                concurrencySemaphore?.release()
            }
        }

        return block.startCoroutineUninterceptedOrReturn(continuation)
    }

    private suspend fun buildContext(method: Method, args: List<Any>?, resultClass: KClass<*>): PromptContext {
        val descriptor = MethodDescriptor.from(method, args, resultClass)
        val request = ShimmerRequest(descriptor, memoryStore.getAll(), resultClass)
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
