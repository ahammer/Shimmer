package com.adamhammer.ai_shimmer.model

/**
 * The assembled context that will be sent to an AI adapter.
 * Built by a [com.adamhammer.ai_shimmer.interfaces.ContextBuilder] and
 * optionally modified by [com.adamhammer.ai_shimmer.interfaces.Interceptor]s.
 */
data class PromptContext(
    val systemInstructions: String,
    val methodInvocation: String,
    val memory: Map<String, String>,
    val properties: Map<String, Any> = emptyMap()
)
