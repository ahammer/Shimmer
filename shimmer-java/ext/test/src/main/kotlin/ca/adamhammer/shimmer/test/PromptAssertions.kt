package ca.adamhammer.shimmer.test

import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.model.TypedKey

/**
 * Fluent assertion extensions for [PromptContext] in tests.
 *
 * Usage:
 * ```kotlin
 * mock.lastContext!!
 *     .assertSystemInstructionsContain("specialized AI")
 *     .assertMethodInvocationContains("greet")
 *     .assertMemoryContains("key", "value")
 * ```
 */

fun PromptContext.assertSystemInstructionsContain(text: String): PromptContext {
    if (!systemInstructions.contains(text)) {
        throw AssertionError(
            "Expected systemInstructions to contain \"$text\" but was:\n$systemInstructions"
        )
    }
    return this
}

fun PromptContext.assertMethodInvocationContains(text: String): PromptContext {
    if (!methodInvocation.contains(text)) {
        throw AssertionError(
            "Expected methodInvocation to contain \"$text\" but was:\n$methodInvocation"
        )
    }
    return this
}

fun PromptContext.assertMemoryContains(key: String, value: String): PromptContext {
    val actual = memory[key]
    if (actual != value) {
        throw AssertionError(
            "Expected memory[\"$key\"] to be \"$value\" but was \"$actual\". Full memory: $memory"
        )
    }
    return this
}

fun PromptContext.assertMemoryEmpty(): PromptContext {
    if (memory.isNotEmpty()) {
        throw AssertionError("Expected empty memory but was: $memory")
    }
    return this
}

fun PromptContext.assertPropertyEquals(key: String, expected: Any): PromptContext {
    val actual = properties[key]
    if (actual != expected) {
        throw AssertionError(
            "Expected properties[\"$key\"] to be \"$expected\" but was \"$actual\""
        )
    }
    return this
}

fun <T> PromptContext.assertTypedPropertyEquals(key: TypedKey<T>, expected: T): PromptContext {
    val actual = this[key]
    if (actual != expected) {
        throw AssertionError(
            "Expected properties[\"${key.name}\"] to be \"$expected\" but was \"$actual\""
        )
    }
    return this
}

fun PromptContext.assertHasTools(vararg names: String): PromptContext {
    val toolNames = availableTools.map { it.name }
    for (name in names) {
        if (name !in toolNames) {
            throw AssertionError(
                "Expected available tools to contain \"$name\" but found: $toolNames"
            )
        }
    }
    return this
}

fun PromptContext.assertToolCount(expected: Int): PromptContext {
    if (availableTools.size != expected) {
        throw AssertionError(
            "Expected $expected available tool(s) but found ${availableTools.size}: ${availableTools.map { it.name }}"
        )
    }
    return this
}
