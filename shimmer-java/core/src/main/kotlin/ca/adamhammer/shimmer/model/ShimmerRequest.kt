package ca.adamhammer.shimmer.model

import kotlin.reflect.KClass

/**
 * Raw request data extracted from a proxy method invocation.
 * Passed to a [ca.adamhammer.shimmer.interfaces.ContextBuilder] to produce a [PromptContext].
 */
data class ShimmerRequest(
    val descriptor: MethodDescriptor,
    val memory: Map<String, String>,
    val resultClass: KClass<*>
)
