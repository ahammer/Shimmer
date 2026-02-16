package com.adamhammer.ai_shimmer.model

import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Raw request data extracted from a proxy method invocation.
 * Passed to a [com.adamhammer.ai_shimmer.interfaces.ContextBuilder] to produce a [PromptContext].
 */
data class ShimmerRequest(
    val method: Method,
    val args: Array<out Any>?,
    val memory: Map<String, String>,
    val resultClass: KClass<*>
)
