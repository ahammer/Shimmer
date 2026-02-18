package com.adamhammer.shimmer.interfaces

import com.adamhammer.shimmer.model.ToolCall
import com.adamhammer.shimmer.model.ToolDefinition
import com.adamhammer.shimmer.model.ToolResult

/**
 * Provides tools that can be invoked during AI interactions.
 *
 * Implementations discover and execute tools from various sources:
 * MCP servers, local functions, or other tool registries.
 *
 * Registered via [com.adamhammer.shimmer.ShimmerBuilder.toolProvider],
 * tool providers are made available to adapters that support multi-turn
 * tool-calling loops.
 */
interface ToolProvider {
    /** Returns the tools currently available from this provider. */
    fun listTools(): List<ToolDefinition>

    /** Executes a tool call and returns the result. */
    fun callTool(call: ToolCall): ToolResult
}
