package com.adamhammer.ai_shimmer.context

import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ShimmerRequest
import com.adamhammer.ai_shimmer.utils.toJsonInvocationString

/**
 * Default [ContextBuilder] that assembles prompts from annotation metadata.
 *
 * Produces a system preamble instructing the AI to follow a structured request/response
 * protocol, plus a JSON method invocation string derived from the method's annotations.
 */
class DefaultContextBuilder : ContextBuilder {

    override fun build(request: ShimmerRequest): PromptContext {
        val methodInvocation = request.method.toJsonInvocationString(request.args, emptyMap())

        return PromptContext(
            systemInstructions = SYSTEM_PREAMBLE,
            methodInvocation = methodInvocation,
            memory = request.memory
        )
    }

    companion object {
        val SYSTEM_PREAMBLE = """
            |You are a specialized AI Assistant that handles request/response method calls and returns results in JSON or plain text.
            |
            |Rules:
            |1. A JSON block describes the method, its parameters, and any stored memory.
            |2. The 'resultSchema' field indicates the JSON format you must return.
            |   - Do NOT add any fields not mentioned in the schema.
            |   - If the schema is "Text", return plain text without JSON wrapping.
            |3. If memory is provided, use it to inform your answer.
            |4. Do not include apology statements, disclaimers, or meta-commentary.
            |5. Return only the specified structure or text, without superfluous information.
            |6. When a Memorize key is present, the result will be stored for later — minimize tokens, maximize content.
        """.trimMargin()
    }
}
