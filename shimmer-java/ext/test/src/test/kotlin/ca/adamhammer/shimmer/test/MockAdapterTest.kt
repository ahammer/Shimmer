package ca.adamhammer.shimmer.test

import ca.adamhammer.shimmer.ShimmerBuilder
import ca.adamhammer.shimmer.model.PromptContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MockAdapterTest {

    @Test
    fun `scripted adapter returns responses in order`() {
        val mock = MockAdapter.scripted(
            SimpleResult("first"),
            SimpleResult("second"),
            SimpleResult("third")
        )

        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        assertEquals("first", api.get().get().value)
        assertEquals("second", api.get().get().value)
        assertEquals("third", api.get().get().value)
        mock.verifyCallCount(3)
    }

    @Test
    fun `scripted adapter repeats last response when exhausted`() {
        val mock = MockAdapter.scripted(SimpleResult("only"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        assertEquals("only", api.get().get().value)
        assertEquals("only", api.get().get().value)
    }

    @Test
    fun `dynamic adapter receives context`() {
        val mock = MockAdapter.dynamic { context, _ ->
            SimpleResult(value = if (context.memory.isEmpty()) "no-memory" else "has-memory")
        }

        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        assertEquals("no-memory", api.get().get().value)
    }

    @Test
    fun `captured contexts record all calls`() {
        val mock = MockAdapter.scripted(SimpleResult("a"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        api.get().get()
        api.getWithParam("hello").get()

        assertEquals(2, mock.callCount)
        assertNotNull(mock.lastContext)
        assertTrue(mock.contextAt(1).methodInvocation.contains("hello"))
    }

    @Test
    fun `builder failOnCall throws on specified index`() {
        val mock = MockAdapter.builder()
            .responses(SimpleResult("ok"))
            .failOnCall(1, RuntimeException("boom"))
            .build()

        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        assertEquals("ok", api.get().get().value)

        val ex = assertThrows(java.util.concurrent.ExecutionException::class.java) {
            api.get().get()
        }
        // Resilience wraps in ShimmerException, so check the full cause chain
        val rootCause = generateSequence(ex as Throwable) { it.cause }.last()
        assertTrue(rootCause.message?.contains("boom") == true,
            "Expected root cause to contain 'boom', got: ${rootCause.message}")
    }

    @Test
    fun `verifyCallCount throws on mismatch`() {
        val mock = MockAdapter.scripted(SimpleResult("x"))
        assertThrows(AssertionError::class.java) {
            mock.verifyCallCount(1)
        }
    }

    @Test
    fun `prompt assertions pass for valid context`() {
        val context = PromptContext(
            systemInstructions = "You are a test AI",
            methodInvocation = """{"method": "greet"}""",
            memory = mapOf("key" to "value"),
            properties = mapOf("temp" to 0.7)
        )

        context
            .assertSystemInstructionsContain("test AI")
            .assertMethodInvocationContains("greet")
            .assertMemoryContains("key", "value")
            .assertPropertyEquals("temp", 0.7)
    }

    @Test
    fun `prompt assertion fails with descriptive message`() {
        val context = PromptContext(
            systemInstructions = "hello",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val error = assertThrows(AssertionError::class.java) {
            context.assertSystemInstructionsContain("missing")
        }
        assertTrue(error.message!!.contains("missing"))
    }

    @Test
    fun `assertMemoryEmpty passes for empty memory`() {
        val context = PromptContext("", "", emptyMap())
        context.assertMemoryEmpty()
    }

    @Test
    fun `assertMemoryEmpty fails for non-empty memory`() {
        val context = PromptContext("", "", mapOf("k" to "v"))
        assertThrows(AssertionError::class.java) {
            context.assertMemoryEmpty()
        }
    }
}
