package com.adamhammer.shimmer

import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.ToolCall
import com.adamhammer.shimmer.model.ToolDefinition
import com.adamhammer.shimmer.model.ToolResult
import com.adamhammer.shimmer.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolProviderTest {

    private val calculatorTool = ToolDefinition(
        name = "calculator",
        description = "Performs math calculations",
        inputSchema = """{"type":"object","properties":{"expression":{"type":"string"}},"required":["expression"]}"""
    )

    @Test
    fun `tool provider tools are injected into PromptContext`() {
        val mockTools = MockToolProvider.builder()
            .tool(calculatorTool)
            .defaultHandler { call -> ToolResult(call.id, call.toolName, "42") }
            .build()

        val mock = MockAdapter.scripted(SimpleResult("result"))
        val instance = shimmer<SimpleTestAPI> {
            adapter(mock)
            toolProvider(mockTools)
        }

        instance.api.get().get()

        val context = mock.lastContext!!
        context.assertToolCount(1)
        context.assertHasTools("calculator")
    }

    @Test
    fun `multiple tool providers are combined`() {
        val provider1 = MockToolProvider.builder()
            .tool("tool_a", "Tool A", """{"type":"object","properties":{}}""")
            .defaultHandler { call -> ToolResult(call.id, call.toolName, "a") }
            .build()

        val provider2 = MockToolProvider.builder()
            .tool("tool_b", "Tool B", """{"type":"object","properties":{}}""")
            .defaultHandler { call -> ToolResult(call.id, call.toolName, "b") }
            .build()

        val mock = MockAdapter.scripted(SimpleResult("result"))
        val instance = shimmer<SimpleTestAPI> {
            adapter(mock)
            toolProvider(provider1)
            toolProvider(provider2)
        }

        instance.api.get().get()

        val context = mock.lastContext!!
        context.assertToolCount(2)
        context.assertHasTools("tool_a", "tool_b")
    }

    @Test
    fun `no tool providers produces empty availableTools`() {
        val mock = MockAdapter.scripted(SimpleResult("result"))
        val instance = shimmer<SimpleTestAPI> {
            adapter(mock)
        }

        instance.api.get().get()

        val context = mock.lastContext!!
        context.assertToolCount(0)
    }

    @Test
    fun `MockToolProvider captures calls and verifies count`() = kotlinx.coroutines.runBlocking {
        val mockTools = MockToolProvider.builder()
            .tool(calculatorTool)
            .handler("calculator") { call ->
                ToolResult(call.id, call.toolName, "Result: 42")
            }
            .build()

        val call = ToolCall("call-1", "calculator", """{"expression":"6*7"}""")
        val result = mockTools.callTool(call)

        assertEquals("Result: 42", result.content)
        assertFalse(result.isError)
        assertEquals(1, mockTools.callCount)
        assertEquals(call, mockTools.lastCall)
        mockTools.verifyCallCount(1)
    }

    @Test
    fun `MockToolProvider returns error for unhandled tool without default`() = kotlinx.coroutines.runBlocking {
        val mockTools = MockToolProvider.builder()
            .tool("known", "A tool", """{"type":"object","properties":{}}""")
            .build()

        val result = mockTools.callTool(ToolCall("1", "unknown", "{}"))
        assertTrue(result.isError)
    }

    @Test
    fun `MockToolProvider dynamic factory works`() = kotlinx.coroutines.runBlocking {
        val tools = listOf(calculatorTool)
        val provider = MockToolProvider.dynamic(tools) { call ->
            ToolResult(call.id, call.toolName, "dynamic-result")
        }

        assertEquals(1, provider.listTools().size)
        val result = provider.callTool(ToolCall("1", "calculator", "{}"))
        assertEquals("dynamic-result", result.content)
    }

    @Test
    fun `ToolDefinition has optional outputSchema`() {
        val withOutput = ToolDefinition("t", "desc", "{}", outputSchema = """{"type":"string"}""")
        val withoutOutput = ToolDefinition("t", "desc", "{}")

        assertNotNull(withOutput.outputSchema)
        assertNull(withoutOutput.outputSchema)
    }

    @Test
    fun `tool providers list passed via toolProviders builder method`() {
        val pa = MockToolProvider.builder()
            .tool("x", "X", """{"type":"object","properties":{}}""")
            .defaultHandler { call -> ToolResult(call.id, call.toolName, "x") }
            .build()
        val pb = MockToolProvider.builder()
            .tool("y", "Y", """{"type":"object","properties":{}}""")
            .defaultHandler { call -> ToolResult(call.id, call.toolName, "y") }
            .build()

        val mock = MockAdapter.scripted(SimpleResult("ok"))
        val instance = ShimmerBuilder(SimpleTestAPI::class)
            .adapter(mock)
            .toolProviders(listOf(pa, pb))
            .build()

        instance.api.get().get()
        mock.lastContext!!.assertToolCount(2)
    }
}
