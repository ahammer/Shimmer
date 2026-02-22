package com.adamhammer.shimmer

import com.adamhammer.shimmer.adapters.CachingAdapter
import com.adamhammer.shimmer.context.DefaultContextBuilder
import com.adamhammer.shimmer.context.InMemoryStore
import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.ContextBuilder
import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.interfaces.MemoryStore
import com.adamhammer.shimmer.interfaces.RequestListener
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.interfaces.TypeAdapter
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.model.ResiliencePolicy
import com.adamhammer.shimmer.model.ShimmerConfigurationException
import com.adamhammer.shimmer.model.TypeAdapterRegistry
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * A configured Shimmer proxy instance.
 *
 * @param T the interface type this instance implements
 * @param api the dynamic proxy implementing [T], backed by the configured adapter
 * @param memoryStore the memory store for persisting results across calls
 * @param klass the [KClass] of the interface type
 */
class ShimmerInstance<T : Any>(
    val api: T,
    val memoryStore: MemoryStore,
    val klass: KClass<T>,
    val usage: UsageTracker = UsageTracker()
) {
    /**
     * Returns the memory store backing this instance.
     * Intended for intra-library use only (e.g., agent dispatchers that need
     * to share memory across multiple [ShimmerInstance]s).
     */
    fun writableMemory(): MemoryStore = memoryStore
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
    private val listeners = mutableListOf<RequestListener>()
    private var resiliencePolicy: ResiliencePolicy = ResiliencePolicy()
    private val typeAdapterRegistry = TypeAdapterRegistry()
    private var cacheConfig: CacheConfig? = null

    // ── DSL-style configuration ─────────────────────────────────────────────

    /** Set the adapter instance. */
    fun adapter(adapter: ApiAdapter): ShimmerBuilder<T> {
        this.adapter = adapter
        return this
    }

    /** Set a routing adapter that chooses an adapter based on the prompt context. */
    fun adapter(router: (PromptContext) -> ApiAdapter): ShimmerBuilder<T> {
        this.adapter = com.adamhammer.shimmer.adapters.RoutingAdapter(router)
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

    /** Register a request lifecycle listener for metrics, logging, or cost tracking. */
    fun listener(listener: RequestListener): ShimmerBuilder<T> {
        this.listeners.add(listener)
        return this
    }

    /** Enable response caching with configurable TTL and max entries. */
    fun cache(block: CacheConfig.() -> Unit = {}): ShimmerBuilder<T> {
        this.cacheConfig = CacheConfig().apply(block)
        return this
    }

    /**
     * Register a [TypeAdapter] that bridges an external POJO to a `@Serializable` mirror type.
     *
     * When Shimmer encounters the POJO as a parameter or return type, it will
     * transparently swap it for the mirror type during schema generation, serialization,
     * and deserialization, then convert back to the POJO for the caller.
     */
    fun typeAdapter(adapter: TypeAdapter<*, *>): ShimmerBuilder<T> {
        typeAdapterRegistry.register(adapter)
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
        var resolvedAdapter = adapter
            ?: throw ShimmerConfigurationException("Adapter must be provided. Use adapter(...) or setAdapterDirect(...)")

        cacheConfig?.let { config ->
            resolvedAdapter = CachingAdapter(resolvedAdapter, config.ttlMs, config.maxEntries)
        }

        val memoryStore = InMemoryStore()
        val usageTracker = UsageTracker()
        val allListeners = listeners.toList() + usageTracker

        val shimmer = Shimmer(
            resolvedAdapter, contextBuilder, interceptors.toList(),
            resiliencePolicy, toolProviders.toList(), memoryStore, apiInterface,
            allListeners, typeAdapterRegistry
        )

        val proxyInstance = Proxy.newProxyInstance(
            apiInterface.java.classLoader,
            arrayOf(apiInterface.java),
            shimmer
        )

        return ShimmerInstance(apiInterface.java.cast(proxyInstance), memoryStore, apiInterface, usageTracker)
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
    var resultValidator: ((Any) -> Any)? = null
    var fallbackAdapter: ApiAdapter? = null
    var maxConcurrentRequests: Int = 0
    var maxRequestsPerMinute: Int = 0

    fun build() = ResiliencePolicy(
        maxRetries = maxRetries,
        retryDelayMs = retryDelayMs,
        backoffMultiplier = backoffMultiplier,
        timeoutMs = timeoutMs,
        resultValidator = resultValidator,
        fallbackAdapter = fallbackAdapter,
        maxConcurrentRequests = maxConcurrentRequests,
        maxRequestsPerMinute = maxRequestsPerMinute
    )
}

/**
 * Configuration for response caching.
 *
 * @property ttlMs time-to-live for cache entries in milliseconds (default: 5 minutes)
 * @property maxEntries maximum number of cached entries (default: 100)
 */
@ShimmerDsl
class CacheConfig {
    var ttlMs: Long = 300_000
    var maxEntries: Int = 100
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
