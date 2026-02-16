package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class ShimmerInstance<T : Any>(
    val api: T,
    val memory: MutableMap<String, String>,
    val klass: KClass<T>
)


// Builder for an AI Shimmer for an interface specification.
// Binds the Interface -> AI Adapter.
class ShimmerBuilder<T : Any>(private val apiInterface: KClass<T>) {
    private var adapter: ApiAdapter? = null

    fun <U : ApiAdapter> setAdapterClass(adapterClass: KClass<U>): ShimmerBuilder<T> {
        this.adapter = adapterClass.constructors
            .first { it.parameters.isEmpty() }
            .call()
        return this
    }

    fun setAdapterDirect(adapter: ApiAdapter): ShimmerBuilder<T> {
        this.adapter = adapter
        return this
    }


    fun build(): ShimmerInstance<T> {
        if (adapter == null) {
            throw IllegalStateException("Adapter must be provided")
        }
        val shimmer = Shimmer<T>(adapter!!)

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
