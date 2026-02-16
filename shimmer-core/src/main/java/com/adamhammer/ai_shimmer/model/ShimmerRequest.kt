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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShimmerRequest) return false
        return method == other.method &&
            args.contentEquals(other.args) &&
            memory == other.memory &&
            resultClass == other.resultClass
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + (args?.contentHashCode() ?: 0)
        result = 31 * result + memory.hashCode()
        result = 31 * result + resultClass.hashCode()
        return result
    }
}
