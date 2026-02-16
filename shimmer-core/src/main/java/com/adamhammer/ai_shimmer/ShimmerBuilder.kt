package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.context.DefaultContextBuilder
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.interfaces.Interceptor
import com.adamhammer.ai_shimmer.model.ResiliencePolicy
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class ShimmerInstance<T : Any>(
    val api: T,
    val memory: MutableMap<String, String>,
    val klass: KClass<T>
)

class ShimmerBuilder<T : Any>(private val apiInterface: KClass<T>) {
    private var adapter: ApiAdapter? = null
    private var contextBuilder: ContextBuilder = DefaultContextBuilder()
    private val interceptors = mutableListOf<Interceptor>()
    private var resilience = ResiliencePolicy()

    fun <U : ApiAdapter> setAdapterClass(adapterClass: KClass<U>): ShimmerBuilder<T> {
        this.adapter = adapterClass.constructors
            .firstOrNull { it.parameters.all { p -> p.isOptional } }
            ?.callBy(emptyMap())
            ?: throw IllegalArgumentException(
                "No constructor with all-optional parameters found for ${adapterClass.simpleName}"
            )
        return this
    }

    fun setAdapterDirect(adapter: ApiAdapter): ShimmerBuilder<T> {
        this.adapter = adapter
        return this
    }

    fun setContextBuilder(builder: ContextBuilder): ShimmerBuilder<T> {
        this.contextBuilder = builder
        return this
    }

    fun addInterceptor(interceptor: Interceptor): ShimmerBuilder<T> {
        this.interceptors.add(interceptor)
        return this
    }

    fun setResiliencePolicy(policy: ResiliencePolicy): ShimmerBuilder<T> {
        this.resilience = policy
        return this
    }

    fun build(): ShimmerInstance<T> {
        val adapter = adapter ?: throw IllegalStateException("Adapter must be provided")
        val shimmer = Shimmer<T>(adapter, contextBuilder, interceptors.toList(), resilience)

        val proxyInstance = Proxy.newProxyInstance(
            apiInterface.java.classLoader,
            arrayOf(apiInterface.java),
            shimmer
        )

        val instance = ShimmerInstance(apiInterface.java.cast(proxyInstance), mutableMapOf(), apiInterface)
        shimmer.instance = instance
        return instance
    }
}
