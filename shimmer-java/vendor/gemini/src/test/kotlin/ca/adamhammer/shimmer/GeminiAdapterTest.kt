package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.adapters.GeminiAdapter
import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.model.ShimmerDeserializationException
import ca.adamhammer.shimmer.test.EnumResult
import ca.adamhammer.shimmer.test.SimpleResult
import ca.adamhammer.shimmer.test.TestColor
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeminiAdapterTest {

    private lateinit var adapter: GeminiAdapter

    private val defaultContext = PromptContext(
        systemInstructions = "test",
        methodInvocation = "{}",
        memory = emptyMap()
    )

    @BeforeEach
    fun setup() {
        // Use a relaxed mock client — only extractJson, deserializeResponse, and
        // buildUserPrompt are tested here (Client.models is a public final field
        // that MockK cannot intercept). Full handleRequest coverage is in live tests.
        adapter = GeminiAdapter(client = mockk(relaxed = true))
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

    // ── deserializeResponse for data class results ─────────────────────────

    @Test
    fun `deserializeResponse deserializes JSON to data class`() {
        val result = adapter.deserializeResponse(
            "{\"value\": \"test-value\"}", SimpleResult::class, defaultContext
        )
        assertEquals("test-value", result.value)
    }

    @Test
    fun `deserializeResponse extracts JSON from markdown code block`() {
        val result = adapter.deserializeResponse(
            "```json\n{\"value\": \"from-block\"}\n```", SimpleResult::class, defaultContext
        )
        assertEquals("from-block", result.value)
    }

    @Test
    fun `deserializeResponse deserializes enum result`() {
        val result = adapter.deserializeResponse(
            "{\"color\": \"BLUE\"}", EnumResult::class, defaultContext
        )
        assertEquals(TestColor.BLUE, result.color)
    }

    @Test
    fun `deserializeResponse deserializes enum result wrapped in markdown`() {
        val result = adapter.deserializeResponse(
            "```json\n{\"color\": \"GREEN\"}\n```", EnumResult::class, defaultContext
        )
        assertEquals(TestColor.GREEN, result.color)
    }

    // ── deserializeResponse for String results ─────────────────────────────

    @Test
    fun `deserializeResponse returns plain string for String result class`() {
        val result = adapter.deserializeResponse("Hello World", String::class, defaultContext)
        assertEquals("Hello World", result)
    }

    @Test
    fun `deserializeResponse cleans Text marker for String result`() {
        val result = adapter.deserializeResponse("\"Text\"", String::class, defaultContext)
        assertEquals("", result)
    }

    @Test
    fun `deserializeResponse handles RESULT header for String result`() {
        val result = adapter.deserializeResponse(
            "# RESULT\nActual content here", String::class, defaultContext
        )
        assertEquals("Actual content here", result)
    }

    // ── deserialization error cases ────────────────────────────────────────

    @Test
    fun `deserializeResponse throws on invalid JSON for data class`() {
        assertThrows(ShimmerDeserializationException::class.java) {
            adapter.deserializeResponse(
                "this is not valid json at all", SimpleResult::class, defaultContext
            )
        }
    }

    // ── buildUserPrompt / memory formatting ────────────────────────────────

    @Test
    fun `buildUserPrompt includes memory section when present`() {
        val context = defaultContext.copy(memory = mapOf("key1" to "val1", "key2" to "val2"))
        val prompt = adapter.buildUserPrompt(context)
        assertTrue(prompt.contains("MEMORY"), "Prompt should contain MEMORY section")
        assertTrue(prompt.contains("key1"), "Prompt should contain memory key")
        assertTrue(prompt.contains("val1"), "Prompt should contain memory value")
    }

    @Test
    fun `buildUserPrompt omits memory section when memory is empty`() {
        val prompt = adapter.buildUserPrompt(defaultContext)
        assertFalse(prompt.contains("MEMORY"), "Prompt should not contain MEMORY when empty")
    }

    @Test
    fun `buildUserPrompt embeds JSON memory values as structured JSON`() {
        val context = defaultContext.copy(
            memory = mapOf("obj" to "{\"nested\":\"data\"}", "plain" to "simple")
        )
        val prompt = adapter.buildUserPrompt(context)
        assertTrue(prompt.contains("nested"), "Memory should contain nested JSON key")
        assertTrue(prompt.contains("data"), "Memory should contain nested JSON value")
    }

    @Test
    fun `buildUserPrompt includes method invocation`() {
        val context = defaultContext.copy(methodInvocation = "{\"method\": \"doSomething\"}")
        val prompt = adapter.buildUserPrompt(context)
        assertTrue(prompt.contains("# METHOD"), "Prompt should contain METHOD header")
        assertTrue(prompt.contains("doSomething"), "Prompt should contain method name")
    }
}
