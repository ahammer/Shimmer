package com.adamhammer.shimmer

import com.adamhammer.shimmer.adapters.OpenAiAdapter
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.test.SimpleResult
import com.adamhammer.shimmer.test.TestColor
import com.adamhammer.shimmer.test.EnumResult
import com.openai.client.OpenAIClient
import com.openai.models.*
import io.mockk.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class OpenAiAdapterTest {

    private lateinit var mockClient: OpenAIClient
    private lateinit var adapter: OpenAiAdapter

    @BeforeEach
    fun setup() {
        mockClient = mockk()
        adapter = OpenAiAdapter(client = mockClient)
    }

    private fun stubCompletion(responseText: String) {
        val chatService = mockk<com.openai.services.blocking.ChatService>()
        val completionService = mockk<com.openai.services.blocking.chat.CompletionService>()
        val choice = mockk<ChatCompletion.Choice>()
        val message = mockk<ChatCompletionMessage>()
        val completion = mockk<ChatCompletion>()

        every { mockClient.chat() } returns chatService
        every { chatService.completions() } returns completionService
        every { completionService.create(any<ChatCompletionCreateParams>()) } returns completion
        every { completion.choices() } returns listOf(choice)
        every { choice.message() } returns message
        every { message.content() } returns Optional.of(responseText)
        every { message.toolCalls() } returns Optional.empty()
    }

    // ── extractJson tests ──────────────────────────────────────────────────

    @Test
    fun `extractJson extracts JSON object from mixed text`() {
        val result = adapter.extractJson("Here is the result: {\"value\": \"hello\"} and some more text")
        assertEquals("{\"value\": \"hello\"}", result)
    }

    @Test
    fun `extractJson extracts JSON array`() {
        val result = adapter.extractJson("Result: [1, 2, 3] done")
        assertEquals("[1, 2, 3]", result)
    }

    @Test
    fun `extractJson returns original text when no JSON found`() {
        val text = "just plain text"
        val result = adapter.extractJson(text)
        assertEquals(text, result)
    }

    @Test
    fun `extractJson handles nested JSON objects`() {
        val text = "prefix {\"outer\": {\"inner\": true}} suffix"
        val result = adapter.extractJson(text)
        assertEquals("{\"outer\": {\"inner\": true}}", result)
    }

    @Test
    fun `extractJson strips markdown code fences`() {
        val text = "Here is the result:\n```json\n{\"value\": \"fenced\"}\n```\nDone."
        val result = adapter.extractJson(text)
        assertEquals("{\"value\": \"fenced\"}", result)
    }

    @Test
    fun `extractJson strips plain code fences without language tag`() {
        val text = "```\n{\"value\": \"plain-fence\"}\n```"
        val result = adapter.extractJson(text)
        assertEquals("{\"value\": \"plain-fence\"}", result)
    }

    @Test
    fun `extractJson extracts array from fenced block`() {
        val text = "```json\n[1, 2, 3]\n```"
        val result = adapter.extractJson(text)
        assertEquals("[1, 2, 3]", result)
    }

    // ── handleRequest for data class results ───────────────────────────────

    @Test
    fun `handleRequest deserializes JSON response to data class`() {
        stubCompletion("{\"value\": \"test-value\"}")

        val context = PromptContext(
            systemInstructions = "You are a test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, SimpleResult::class)
        assertEquals("test-value", result.value)
    }

    @Test
    fun `handleRequest extracts JSON from markdown code block`() {
        stubCompletion("```json\n{\"value\": \"from-block\"}\n```")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, SimpleResult::class)
        assertEquals("from-block", result.value)
    }

    // ── handleRequest for String results ───────────────────────────────────

    @Test
    fun `handleRequest returns plain string for String result class`() {
        stubCompletion("Hello World")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, String::class)
        assertEquals("Hello World", result)
    }

    @Test
    fun `handleRequest cleans Text marker for String result`() {
        stubCompletion("\"Text\"")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, String::class)
        assertEquals("", result)
    }

    @Test
    fun `handleRequest handles RESULT header for String result`() {
        stubCompletion("# RESULT\nActual content here")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, String::class)
        assertEquals("Actual content here", result)
    }

    // ── memory formatting ──────────────────────────────────────────────────

    @Test
    fun `handleRequest includes memory in prompt when present`() {
        val capturedParams = slot<ChatCompletionCreateParams>()

        val chatService = mockk<com.openai.services.blocking.ChatService>()
        val completionService = mockk<com.openai.services.blocking.chat.CompletionService>()
        val choice = mockk<ChatCompletion.Choice>()
        val message = mockk<ChatCompletionMessage>()
        val completion = mockk<ChatCompletion>()

        every { mockClient.chat() } returns chatService
        every { chatService.completions() } returns completionService
        every { completionService.create(capture(capturedParams)) } returns completion
        every { completion.choices() } returns listOf(choice)
        every { choice.message() } returns message
        every { message.content() } returns Optional.of("{\"value\": \"ok\"}")
        every { message.toolCalls() } returns Optional.empty()

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = mapOf("key1" to "val1", "key2" to "val2")
        )

        adapter.handleRequest(context, SimpleResult::class)

        val params = capturedParams.captured
        val paramsString = params.toString()
        assertTrue(paramsString.contains("MEMORY"), "Prompt should contain MEMORY section, got: $paramsString")
    }

    @Test
    fun `handleRequest omits memory section when memory is empty`() {
        val capturedParams = slot<ChatCompletionCreateParams>()

        val chatService = mockk<com.openai.services.blocking.ChatService>()
        val completionService = mockk<com.openai.services.blocking.chat.CompletionService>()
        val choice = mockk<ChatCompletion.Choice>()
        val message = mockk<ChatCompletionMessage>()
        val completion = mockk<ChatCompletion>()

        every { mockClient.chat() } returns chatService
        every { chatService.completions() } returns completionService
        every { completionService.create(capture(capturedParams)) } returns completion
        every { completion.choices() } returns listOf(choice)
        every { choice.message() } returns message
        every { message.content() } returns Optional.of("{\"value\": \"ok\"}")
        every { message.toolCalls() } returns Optional.empty()

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        adapter.handleRequest(context, SimpleResult::class)

        val params = capturedParams.captured
        val paramsString = params.toString()
        assertFalse(paramsString.contains("MEMORY"), "Prompt should not contain MEMORY when memory is empty")
    }

    // ── error cases ────────────────────────────────────────────────────────

    @Test
    fun `handleRequest throws on deserialization failure`() {
        stubCompletion("this is not valid json at all")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        assertThrows(RuntimeException::class.java) {
            adapter.handleRequest(context, SimpleResult::class)
        }
    }

    @Test
    fun `handleRequest throws when no response choices`() {
        val chatService = mockk<com.openai.services.blocking.ChatService>()
        val completionService = mockk<com.openai.services.blocking.chat.CompletionService>()
        val completion = mockk<ChatCompletion>()

        every { mockClient.chat() } returns chatService
        every { chatService.completions() } returns completionService
        every { completionService.create(any<ChatCompletionCreateParams>()) } returns completion
        every { completion.choices() } returns emptyList()

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        assertThrows(RuntimeException::class.java) {
            adapter.handleRequest(context, SimpleResult::class)
        }
    }

    // ── enum deserialization (offline) ──────────────────────────────────────

    @Test
    fun `handleRequest deserializes enum result from JSON object`() {
        stubCompletion("{\"color\": \"BLUE\"}")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, EnumResult::class)
        assertEquals(TestColor.BLUE, result.color)
    }

    @Test
    fun `handleRequest deserializes enum result wrapped in markdown`() {
        stubCompletion("```json\n{\"color\": \"GREEN\"}\n```")

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = emptyMap()
        )

        val result = adapter.handleRequest(context, EnumResult::class)
        assertEquals(TestColor.GREEN, result.color)
    }

    // ── memory encoding ────────────────────────────────────────────────────

    @Test
    fun `handleRequest embeds JSON memory values as structured JSON not escaped strings`() {
        val capturedParams = slot<ChatCompletionCreateParams>()

        val chatService = mockk<com.openai.services.blocking.ChatService>()
        val completionService = mockk<com.openai.services.blocking.chat.CompletionService>()
        val choice = mockk<ChatCompletion.Choice>()
        val message = mockk<ChatCompletionMessage>()
        val completion = mockk<ChatCompletion>()

        every { mockClient.chat() } returns chatService
        every { chatService.completions() } returns completionService
        every { completionService.create(capture(capturedParams)) } returns completion
        every { completion.choices() } returns listOf(choice)
        every { choice.message() } returns message
        every { message.content() } returns Optional.of("{\"value\": \"ok\"}")
        every { message.toolCalls() } returns Optional.empty()

        val context = PromptContext(
            systemInstructions = "test",
            methodInvocation = "{}",
            memory = mapOf("obj" to "{\"nested\":\"data\"}", "plain" to "simple")
        )

        adapter.handleRequest(context, SimpleResult::class)

        val params = capturedParams.captured
        val paramsString = params.toString()
        // The nested JSON should appear as structured JSON, not as an escaped string
        assertTrue(paramsString.contains("nested"), "Memory should contain nested JSON key: $paramsString")
        assertTrue(paramsString.contains("data"), "Memory should contain nested JSON value: $paramsString")
    }
}
