package ca.adamhammer.shimmer.interfaces

import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.model.ShimmerRequest

/**
 * Builds a [PromptContext] from a raw [ShimmerRequest].
 *
 * The default implementation ([ca.adamhammer.shimmer.context.DefaultContextBuilder])
 * assembles a system preamble and JSON method invocation from annotations.
 * Replace it to fully control how prompts are constructed.
 */
interface ContextBuilder {
    fun build(request: ShimmerRequest): PromptContext
}
