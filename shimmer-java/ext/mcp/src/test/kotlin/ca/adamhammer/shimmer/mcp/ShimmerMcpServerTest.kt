package ca.adamhammer.shimmer.mcp

import ca.adamhammer.shimmer.annotations.*
import ca.adamhammer.shimmer.model.ToolCall
import ca.adamhammer.shimmer.model.ToolDefinition
import ca.adamhammer.shimmer.utils.toToolDefinitions
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class ShimmerMcpServerTest {

    @Serializable
    @AiSchema(title = "EchoResult", description = "Echo result")
    data class EchoResult(val message: String = "")

    interface EchoApi {
        @AiOperation(summary = "Echo", description = "Echoes back the input message")
        @AiResponse(description = "The echoed message", responseClass = EchoResult::class)
        fun echo(
            @AiParameter(description = "The message to echo") message: String
        ): Future<EchoResult>

        @AiOperation(summary = "Greet", description = "Returns a greeting")
        @AiResponse(description = "The greeting", responseClass = String::class)
        fun greet(
            @AiParameter(description = "Name to greet") name: String
        ): Future<String>
    }

    @Test
    fun `toToolDefinitions generates tools for annotated interface`() {
        val tools = EchoApi::class.toToolDefinitions()
        assertEquals(2, tools.size)
        assertTrue(tools.any { it.name == "echo" })
        assertTrue(tools.any { it.name == "greet" })
    }

    @Test
    fun `toToolDefinitions produces valid input schemas`() {
        val tools = EchoApi::class.toToolDefinitions()
        val echoTool = tools.first { it.name == "echo" }
        val schema = kotlinx.serialization.json.Json.parseToJsonElement(echoTool.inputSchema)
        assertTrue(schema is kotlinx.serialization.json.JsonObject)
        val obj = schema as kotlinx.serialization.json.JsonObject
        assertEquals("object", obj["type"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content })
    }

    @Test
    fun `builder without transport throws`() {
        assertThrows(IllegalStateException::class.java) {
            ShimmerMcpServer.builder()
                .serverInfo("test", "1.0.0")
                .build()
        }
    }

    @Test
    fun `ToolDefinition round-trip preserves data`() {
        val def = ToolDefinition(
            name = "test_tool",
            description = "A test tool",
            inputSchema = """{"type":"object","properties":{"x":{"type":"string"}},"required":["x"]}""",
            outputSchema = """{"type":"string"}"""
        )
        assertEquals("test_tool", def.name)
        assertEquals("A test tool", def.description)
        assertNotNull(def.outputSchema)
    }

    @Test
    fun `ToolCall and ToolResult data classes work correctly`() {
        val call = ToolCall("id-1", "my_tool", """{"arg":"val"}""")
        assertEquals("id-1", call.id)
        assertEquals("my_tool", call.toolName)

        val result = ca.adamhammer.shimmer.model.ToolResult("id-1", "my_tool", "success", false)
        assertFalse(result.isError)
        assertEquals("success", result.content)
    }
}
