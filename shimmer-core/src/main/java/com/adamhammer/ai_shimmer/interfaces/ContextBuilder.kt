package com.adamhammer.ai_shimmer.interfaces

import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ShimmerRequest

/**
 * Builds a [PromptContext] from a raw [ShimmerRequest].
 *
 * The default implementation ([com.adamhammer.ai_shimmer.context.DefaultContextBuilder])
 * assembles a system preamble and JSON method invocation from annotations.
 * Replace it to fully control how prompts are constructed.
 */
interface ContextBuilder {
    fun build(request: ShimmerRequest): PromptContext
}
