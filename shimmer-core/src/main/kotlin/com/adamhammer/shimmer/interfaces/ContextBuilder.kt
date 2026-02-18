package com.adamhammer.shimmer.interfaces

import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.model.ShimmerRequest

/**
 * Builds a [PromptContext] from a raw [ShimmerRequest].
 *
 * The default implementation ([com.adamhammer.shimmer.context.DefaultContextBuilder])
 * assembles a system preamble and JSON method invocation from annotations.
 * Replace it to fully control how prompts are constructed.
 */
interface ContextBuilder {
    fun build(request: ShimmerRequest): PromptContext
}
