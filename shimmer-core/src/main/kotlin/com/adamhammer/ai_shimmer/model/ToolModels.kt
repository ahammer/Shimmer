package com.adamhammer.ai_shimmer.model

/**
 * Provider-agnostic description of a tool that can be invoked during an AI interaction.
 *
 * @param name unique tool identifier
 * @param description human/LLM-readable description of what the tool does
 * @param inputSchema JSON Schema string describing the tool's expected input
 * @param outputSchema optional JSON Schema string describing the tool's output
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: String,
    val outputSchema: String? = null
)

/**
 * Represents the LLM requesting a tool invocation.
 *
 * @param id unique identifier for this call (used to correlate with [ToolResult])
 * @param toolName the name of the tool to invoke (matches [ToolDefinition.name])
 * @param arguments JSON string of the arguments to pass to the tool
 */
data class ToolCall(
    val id: String,
    val toolName: String,
    val arguments: String
)

/**
 * The result of executing a [ToolCall], fed back to the LLM.
 *
 * @param id the call ID this result corresponds to (matches [ToolCall.id])
 * @param toolName the tool that was called
 * @param content the textual/JSON result content
 * @param isError whether the tool invocation failed
 */
data class ToolResult(
    val id: String,
    val toolName: String,
    val content: String,
    val isError: Boolean = false
)
