package com.adamhammer.ai_shimmer.test

import com.adamhammer.ai_shimmer.model.PromptContext

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
