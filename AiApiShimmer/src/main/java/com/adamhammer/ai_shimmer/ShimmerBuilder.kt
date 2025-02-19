package com.adamhammer.ai_shimmer


import Shimmer
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

// Builder for an AI Shimmer for an interface specification.
// Binds the Interface -> AI Adapter.
class ShimmerBuilder<T : Any>(private val apiInterface: KClass<T>) {
    private var adapter: ApiAdapter<T>? = null

    fun setAdapter(adapter: ApiAdapter<T>): ShimmerBuilder<T> {
        this.adapter = adapter
        return this
    }

    fun build(): T {
        if (adapter == null) {
            throw IllegalStateException("Adapter must be provided")
        }
        val proxyInstance = Proxy.newProxyInstance(
            apiInterface.java.classLoader,
            arrayOf(apiInterface.java),
            Shimmer(adapter!!)
        )
        return apiInterface.java.cast(proxyInstance)
    }
}