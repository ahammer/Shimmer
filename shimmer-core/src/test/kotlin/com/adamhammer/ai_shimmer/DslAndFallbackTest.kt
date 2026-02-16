package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ShimmerConfigurationException
import com.adamhammer.ai_shimmer.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DslAndFallbackTest {

    // ── DSL entry point tests ───────────────────────────────────────────────

    @Test
    fun `shimmer DSL with resilience and interceptor`() {
        val mock = MockAdapter.scripted(SimpleResult("dsl-full"))
        val instance = shimmer<SimpleTestAPI> {
            adapter(mock)
            resilience {
                maxRetries = 1
                retryDelayMs = 10
            }
            interceptor { ctx ->
                ctx.copy(systemInstructions = ctx.systemInstructions + "\n[DSL-INTERCEPTOR]")
            }
        }

        val result = instance.api.get().get()
        assertEquals("dsl-full", result.value)
        mock.lastContext!!.assertSystemInstructionsContain("[DSL-INTERCEPTOR]")
    }

    @Test
    fun `shimmer DSL exposes memory through instance`() {
        val mock = MockAdapter.scripted("stored")
        val instance = shimmer<MemoryTestAPI> {
            adapter(mock)
        }

        instance.api.store("test").get()
        assertTrue(instance.memory.containsKey("stored-value"))
    }

    // ── @AiResponse fallback type extraction tests ──────────────────────────

    @Test
    fun `AiResponse fallback extracts type from Future generic`() {
        val mock = MockAdapter.scripted(SimpleResult("fallback-works"))
        val api = ShimmerBuilder(FallbackResponseAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.get().get()
        assertEquals("fallback-works", result.value)
    }

    @Test
    fun `AiResponse fallback extracts String type from Future generic`() {
        val mock = MockAdapter.scripted("hello-fallback")
        val api = ShimmerBuilder(FallbackResponseAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.getString().get()
        assertEquals("hello-fallback", result)
    }

    @Test
    fun `AiResponse fallback produces correct resultSchema in prompt`() {
        val mock = MockAdapter.scripted(SimpleResult("schema-check"))
        val api = ShimmerBuilder(FallbackResponseAPI::class)
            .setAdapterDirect(mock)
            .build().api

        api.get().get()

        // The methodInvocation should contain a resultSchema derived from SimpleResult
        val invocation = mock.lastContext!!.methodInvocation
        assertTrue(invocation.contains("resultSchema"), "Expected resultSchema in invocation: $invocation")
        assertTrue(invocation.contains("value"), "Expected 'value' field from SimpleResult in schema: $invocation")
    }

    // ── Suspend fallback test ───────────────────────────────────────────────

    @Test
    fun `suspend function triggers fallback adapter when primary fails`() = runBlocking {
        val failingMock = MockAdapter.builder()
            .responses(SimpleResult("unused"))
            .failOnCall(0, RuntimeException("primary-failure"))
            .build()

        val fallbackMock = MockAdapter.scripted(SimpleResult("fallback-result"))

        val api = shimmer<SuspendTestAPI> {
            adapter(failingMock)
            resilience {
                maxRetries = 0
                fallbackAdapter = fallbackMock
            }
        }.api

        val result = api.get()
        assertEquals("fallback-result", result.value)
        fallbackMock.verifyCallCount(1)
    }

    // ── Negative DSL test ───────────────────────────────────────────────────

    @Test
    fun `shimmer DSL without adapter throws ShimmerConfigurationException`() {
        assertThrows(ShimmerConfigurationException::class.java) {
            shimmer<SimpleTestAPI> { }
        }
    }
}
