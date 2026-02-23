package ca.adamhammer.shimmer.test

import ca.adamhammer.shimmer.interfaces.ToolProvider
import ca.adamhammer.shimmer.model.ToolCall
import ca.adamhammer.shimmer.model.ToolDefinition
import ca.adamhammer.shimmer.model.ToolResult

/**
 * A configurable test [ToolProvider] for offline testing of tool-calling flows.
 *
 * Supports:
 * - **Scripted tool definitions**: declare which tools are available
 * - **Scripted responses**: queue tool results returned in order per tool name
 * - **Dynamic responses**: a lambda that produces tool results from the call
 * - **Call capture**: records every [ToolCall] received
 *
 * Usage:
 * ```kotlin
 * val tools = MockToolProvider.builder()
 *     .tool("calculator", "Performs math", """{"type":"object","properties":{"expr":{"type":"string"}},"required":["expr"]}""")
 *     .handler("calculator") { call -> ToolResult(call.id, call.toolName, "42") }
 *     .build()
 * ```
 */
class MockToolProvider private constructor(
    private val tools: List<ToolDefinition>,
    private val handlers: Map<String, (ToolCall) -> ToolResult>,
    private val defaultHandler: ((ToolCall) -> ToolResult)?
) : ToolProvider {

    private val _capturedCalls: MutableList<ToolCall> = mutableListOf()

    /** All captured [ToolCall] objects received by this provider, in call order. */
    val capturedCalls: List<ToolCall> get() = _capturedCalls.toList()

    /** The most recent [ToolCall] received, or null if no calls were made. */
    val lastCall: ToolCall? get() = _capturedCalls.lastOrNull()

    /** Total number of calls made to this provider. */
    val callCount: Int get() = _capturedCalls.size

    /** Assert that exactly [expected] calls were made. Throws [AssertionError] if not. */
    fun verifyCallCount(expected: Int) {
        if (_capturedCalls.size != expected) {
            throw AssertionError("Expected $expected tool call(s) but got ${_capturedCalls.size}")
        }
    }

    override fun listTools(): List<ToolDefinition> = tools

    override suspend fun callTool(call: ToolCall): ToolResult {
        _capturedCalls.add(call)

        val handler = handlers[call.toolName] ?: defaultHandler
            ?: return ToolResult(call.id, call.toolName, "Error: No handler for tool '${call.toolName}'", isError = true)

        return handler(call)
    }

    companion object {
        /** Create a simple mock with a fixed set of tools and a dynamic handler. */
        fun dynamic(
            tools: List<ToolDefinition>,
            handler: (ToolCall) -> ToolResult
        ): MockToolProvider = MockToolProvider(tools, emptyMap(), handler)

        /** Create a builder for more complex configurations. */
        fun builder(): Builder = Builder()
    }

    class Builder {
        private val tools = mutableListOf<ToolDefinition>()
        private val handlers = mutableMapOf<String, (ToolCall) -> ToolResult>()
        private var defaultHandler: ((ToolCall) -> ToolResult)? = null

        /** Register a tool definition. */
        fun tool(name: String, description: String, inputSchema: String): Builder {
            tools.add(ToolDefinition(name, description, inputSchema))
            return this
        }

        /** Register a tool definition. */
        fun tool(definition: ToolDefinition): Builder {
            tools.add(definition)
            return this
        }

        /** Register a handler for a specific tool name. */
        fun handler(toolName: String, handler: (ToolCall) -> ToolResult): Builder {
            handlers[toolName] = handler
            return this
        }

        /** Register a default handler for unhandled tools. */
        fun defaultHandler(handler: (ToolCall) -> ToolResult): Builder {
            defaultHandler = handler
            return this
        }

        fun build(): MockToolProvider = MockToolProvider(tools.toList(), handlers.toMap(), defaultHandler)
    }
}
