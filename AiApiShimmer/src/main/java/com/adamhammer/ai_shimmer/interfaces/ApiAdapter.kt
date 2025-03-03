package com.adamhammer.ai_shimmer.interfaces

import java.lang.reflect.Method
import kotlin.reflect.KClass

// This is an Adapter that can reference a specific AI implementation
interface ApiAdapter {
    // Handles a request
    fun <R : Any> handleRequest(
        method: Method,
        args: Array<out Any>?,
        resultClass: KClass<R>,
        memory: Map<String, String>
    ): R
}