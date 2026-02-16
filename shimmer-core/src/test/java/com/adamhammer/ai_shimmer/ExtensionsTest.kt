package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.annotations.*
import com.adamhammer.ai_shimmer.test.*
import com.adamhammer.ai_shimmer.utils.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class ExtensionsTest {

    // ── toJsonStructure / toJsonStructureString ────────────────────────────

    @Test
    fun `toJsonStructure for String class returns text primitive`() {
        val result = String::class.toJsonStructure()
        assertTrue(result is JsonPrimitive)
        assertEquals("Text", (result as JsonPrimitive).content)
    }

    @Test
    fun `toJsonStructure for enum returns enum values`() {
        val result = TestColor::class.toJsonStructure()
        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        val enumArray = obj["enum"] as JsonArray
        assertEquals(3, enumArray.size)
        assertTrue(enumArray.any { (it as JsonPrimitive).content == "RED" })
        assertTrue(enumArray.any { (it as JsonPrimitive).content == "GREEN" })
        assertTrue(enumArray.any { (it as JsonPrimitive).content == "BLUE" })
    }

    @Test
    fun `toJsonStructure for data class includes properties`() {
        val result = SimpleResult::class.toJsonStructure()
        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        assertNotNull(obj["value"], "Expected 'value' key in JSON structure")
    }

    @Test
    fun `toJsonStructure for data class with enum property shows enum values`() {
        val result = EnumResult::class.toJsonStructure()
        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        val colorField = obj["color"]
        assertNotNull(colorField)
        assertTrue(colorField is JsonPrimitive, "Enum field should be a primitive descriptor, got: $colorField")
        assertTrue((colorField as JsonPrimitive).content.contains("RED"))
    }

    @Test
    fun `toJsonStructureString returns valid JSON`() {
        val jsonString = SimpleResult::class.toJsonStructureString()
        assertDoesNotThrow { Json.parseToJsonElement(jsonString) }
    }

    // ── toJsonElement ──────────────────────────────────────────────────────

    @Test
    fun `toJsonElement for null returns JsonNull`() {
        val result = null.toJsonElement()
        assertEquals(JsonNull, result)
    }

    @Test
    fun `toJsonElement for serializable object returns json object`() {
        val result = SimpleResult("hello").toJsonElement()
        assertTrue(result is JsonObject)
        assertEquals("hello", (result as JsonObject)["value"]?.jsonPrimitive?.content)
    }

    @Test
    fun `toJsonElement for non-serializable returns error primitive`() {
        val nonSerializable = object { val x = 1 }
        val result = nonSerializable.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertTrue((result as JsonPrimitive).content.contains("error encoding"))
    }

    // ── toJsonInvocation ───────────────────────────────────────────────────

    @Serializable
    @AiSchema(title = "InvocationResult", description = "For testing invocation")
    data class InvocationResult(val data: String = "")

    interface InvocationTestAPI {
        @AiOperation(summary = "DoSomething", description = "Does something")
        @AiResponse(description = "The result", responseClass = InvocationResult::class)
        fun doSomething(
            @AiParameter(description = "An input value") input: String
        ): Future<InvocationResult>

        @AiOperation(summary = "NoArgs", description = "No arguments")
        @AiResponse(description = "The result", responseClass = InvocationResult::class)
        fun noArgs(): Future<InvocationResult>
    }

    @Test
    fun `toJsonInvocation includes method name`() {
        val method = InvocationTestAPI::class.java.getMethod("doSomething", String::class.java)
        val result = method.toJsonInvocation(arrayOf("test-val"))
        val methodField = result["method"]?.jsonPrimitive?.content ?: ""
        assertTrue(methodField.contains("doSomething"), "Expected method name, got: $methodField")
    }

    @Test
    fun `toJsonInvocation includes operation summary`() {
        val method = InvocationTestAPI::class.java.getMethod("doSomething", String::class.java)
        val result = method.toJsonInvocation(arrayOf("test-val"))
        val methodField = result["method"]?.jsonPrimitive?.content ?: ""
        assertTrue(methodField.contains("DoSomething"), "Expected summary in method field, got: $methodField")
    }

    @Test
    fun `toJsonInvocation includes parameters with values`() {
        val method = InvocationTestAPI::class.java.getMethod("doSomething", String::class.java)
        val result = method.toJsonInvocation(arrayOf("test-val"))
        val params = result["parameters"] as JsonArray
        assertEquals(1, params.size)
        val paramObj = params[0] as JsonObject
        assertNotNull(paramObj["value"])
    }

    @Test
    fun `toJsonInvocation with no args has empty parameters`() {
        val method = InvocationTestAPI::class.java.getMethod("noArgs")
        val result = method.toJsonInvocation(null)
        val params = result["parameters"] as JsonArray
        assertTrue(params.isEmpty())
    }

    @Test
    fun `toJsonInvocation includes memory when provided`() {
        val method = InvocationTestAPI::class.java.getMethod("noArgs")
        val memory = mapOf("key1" to "value1", "key2" to "value2")
        val result = method.toJsonInvocation(null, memory)
        val memoryObj = result["memory"] as JsonObject
        assertEquals("value1", memoryObj["key1"]?.jsonPrimitive?.content)
        assertEquals("value2", memoryObj["key2"]?.jsonPrimitive?.content)
    }

    @Test
    fun `toJsonInvocation omits memory when empty`() {
        val method = InvocationTestAPI::class.java.getMethod("noArgs")
        val result = method.toJsonInvocation(null, emptyMap())
        assertNull(result["memory"], "Expected no memory key when map is empty")
    }

    @Test
    fun `toJsonInvocation includes resultSchema`() {
        val method = InvocationTestAPI::class.java.getMethod("doSomething", String::class.java)
        val result = method.toJsonInvocation(arrayOf("x"))
        assertNotNull(result["resultSchema"], "Expected resultSchema in invocation JSON")
    }

    @Test
    fun `toJsonInvocationString returns valid JSON`() {
        val method = InvocationTestAPI::class.java.getMethod("doSomething", String::class.java)
        val jsonStr = method.toJsonInvocationString(arrayOf("test"))
        assertDoesNotThrow { Json.parseToJsonElement(jsonStr) }
    }

    // ── toJsonClassMetadata ────────────────────────────────────────────────

    @Test
    fun `toJsonClassMetadata includes class name`() {
        val result = SimpleTestAPI::class.toJsonClassMetadata()
        val agentName = result["Agent Name"]?.jsonPrimitive?.content
        assertEquals("SimpleTestAPI", agentName)
    }

    @Test
    fun `toJsonClassMetadata includes methods array`() {
        val result = SimpleTestAPI::class.toJsonClassMetadata()
        val methods = result["methods"] as JsonArray
        assertTrue(methods.isNotEmpty(), "Expected at least one method")
    }

    @Test
    fun `toJsonClassMetadataString returns valid JSON`() {
        val jsonStr = SimpleTestAPI::class.toJsonClassMetadataString()
        assertDoesNotThrow { Json.parseToJsonElement(jsonStr) }
    }
}
