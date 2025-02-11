package com.adamhammer.ai_shimmer

import ApiAdapter
import ApiInvocationHandler
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

class AiApiBuilder<T : Any>(private val apiInterface: KClass<T>) {
    private var adapter: ApiAdapter<T>? = null

    fun setAdapter(adapter: ApiAdapter<T>): AiApiBuilder<T> {
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
            ApiInvocationHandler(adapter!!)
        )
        return apiInterface.java.cast(proxyInstance)
    }
}
