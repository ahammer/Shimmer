package com.adamhammer.ai_shimmer.model

/**
 * Role for a message in a conversation history.
 */
enum class MessageRole { SYSTEM, USER, ASSISTANT }

/**
 * A single message in a conversation history.
 */
data class Message(
    val role: MessageRole,
    val content: String
)

/**
 * The assembled context that will be sent to an AI adapter.
 * Built by a [com.adamhammer.ai_shimmer.interfaces.ContextBuilder] and
 * optionally modified by [com.adamhammer.ai_shimmer.interfaces.Interceptor]s.
 */
data class PromptContext(
    val systemInstructions: String,
    val methodInvocation: String,
    val memory: Map<String, String>,
    val properties: Map<String, Any> = emptyMap(),
    val availableTools: List<ToolDefinition> = emptyList(),
    val conversationHistory: List<Message> = emptyList()
)
