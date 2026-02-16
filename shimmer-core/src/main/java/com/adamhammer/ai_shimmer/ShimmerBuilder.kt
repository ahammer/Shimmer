package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.context.DefaultContextBuilder
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.interfaces.Interceptor
import com.adamhammer.ai_shimmer.interfaces.ToolProvider
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ResiliencePolicy
import com.adamhammer.ai_shimmer.model.ShimmerConfigurationException
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ShimmerInstance<T : Any>(
    val api: T,
    internal val _memory: MutableMap<String, String>,
    val klass: KClass<T>
) {
    /** Read-only view of the current memory map. */
    val memory: Map<String, String> get() = _memory.toMap()
}

@DslMarker
annotation class ShimmerDsl

/**
 * Builder for creating a Shimmer proxy. Supports both DSL and fluent Java-style APIs.
 *
 * DSL usage:
 * ```kotlin
 * val instance = shimmer<MyAPI> {
 *     adapter(OpenAiAdapter())
 *     resilience {
 *         maxRetries = 2
 *         timeoutMs = 30_000
 *     }
 *     interceptor { ctx -> ctx.copy(systemInstructions = ctx.systemInstructions + "\nBe concise.") }
 * }
 * ```
 */
@ShimmerDsl
class ShimmerBuilder<T : Any>(private val apiInterface: KClass<T>) {
    private var adapter: ApiAdapter? = null
    private var contextBuilder: ContextBuilder = DefaultContextBuilder()
    private val interceptors = mutableListOf<Interceptor>()
    private val toolProviders = mutableListOf<ToolProvider>()
    private var resiliencePolicy: ResiliencePolicy = ResiliencePolicy()

    // ── DSL-style configuration ─────────────────────────────────────────────

    /** Set the adapter instance. */
    fun adapter(adapter: ApiAdapter): ShimmerBuilder<T> {
        this.adapter = adapter
        return this
    }

    /** Set a custom context builder. */
    fun contextBuilder(builder: ContextBuilder): ShimmerBuilder<T> {
        this.contextBuilder = builder
        return this
    }

    /** Add an interceptor via lambda. */
    fun interceptor(block: (PromptContext) -> PromptContext): ShimmerBuilder<T> {
        this.interceptors.add(Interceptor { block(it) })
        return this
    }

    /** Configure resilience policy via DSL block. */
    fun resilience(block: ResiliencePolicyBuilder.() -> Unit): ShimmerBuilder<T> {
        this.resiliencePolicy = ResiliencePolicyBuilder().apply(block).build()
        return this
    }

    /** Set a pre-built resilience policy. */
    fun resilience(policy: ResiliencePolicy): ShimmerBuilder<T> {
        this.resiliencePolicy = policy
        return this
    }

    /** Register a tool provider. Tools from all providers are made available to adapters. */
    fun toolProvider(provider: ToolProvider): ShimmerBuilder<T> {
        this.toolProviders.add(provider)
        return this
    }

    /** Register multiple tool providers. */
    fun toolProviders(providers: List<ToolProvider>): ShimmerBuilder<T> {
        this.toolProviders.addAll(providers)
        return this
    }

    // ── Legacy Java-style API (kept for backward compatibility) ─────────────

    fun <U : ApiAdapter> setAdapterClass(adapterClass: KClass<U>): ShimmerBuilder<T> {
        val ctor = adapterClass.constructors.firstOrNull { it.parameters.all { p -> p.isOptional } }
            ?: throw IllegalArgumentException(
                "${adapterClass.simpleName} has no no-arg or all-optional constructor"
            )
        this.adapter = ctor.callBy(emptyMap())
        return this
    }

    fun setAdapterDirect(adapter: ApiAdapter): ShimmerBuilder<T> = adapter(adapter)

    fun setContextBuilder(builder: ContextBuilder): ShimmerBuilder<T> = contextBuilder(builder)

    fun addInterceptor(interceptor: Interceptor): ShimmerBuilder<T> {
        this.interceptors.add(interceptor)
        return this
    }

    fun setResiliencePolicy(policy: ResiliencePolicy): ShimmerBuilder<T> = resilience(policy)

    // ── Build ───────────────────────────────────────────────────────────────

    fun build(): ShimmerInstance<T> {
        val resolvedAdapter = adapter
            ?: throw ShimmerConfigurationException("Adapter must be provided. Use adapter(...) or setAdapterDirect(...)")

        val shimmer = Shimmer<T>(resolvedAdapter, contextBuilder, interceptors.toList(), resiliencePolicy, toolProviders.toList())

        val proxyInstance = Proxy.newProxyInstance(
            apiInterface.java.classLoader,
            arrayOf(apiInterface.java),
            shimmer
        )

        val instance = ShimmerInstance(apiInterface.java.cast(proxyInstance), ConcurrentHashMap(), apiInterface)
        shimmer.instance = instance
        return instance
    }
}

/**
 * DSL builder for [ResiliencePolicy].
 */
@ShimmerDsl
class ResiliencePolicyBuilder {
    var maxRetries: Int = 0
    var retryDelayMs: Long = 1000
    var backoffMultiplier: Double = 2.0
    var timeoutMs: Long = 0
    var resultValidator: ((Any) -> Boolean)? = null
    var fallbackAdapter: ApiAdapter? = null

    fun build() = ResiliencePolicy(
        maxRetries = maxRetries,
        retryDelayMs = retryDelayMs,
        backoffMultiplier = backoffMultiplier,
        timeoutMs = timeoutMs,
        resultValidator = resultValidator,
        fallbackAdapter = fallbackAdapter
    )
}

/**
 * Top-level DSL entry point for creating a Shimmer proxy.
 *
 * ```kotlin
 * val instance = shimmer<MyAPI> {
 *     adapter(OpenAiAdapter())
 *     resilience { maxRetries = 2 }
 * }
 * val api = instance.api
 * ```
 */
inline fun <reified T : Any> shimmer(block: ShimmerBuilder<T>.() -> Unit): ShimmerInstance<T> {
    return ShimmerBuilder(T::class).apply(block).build()
}
