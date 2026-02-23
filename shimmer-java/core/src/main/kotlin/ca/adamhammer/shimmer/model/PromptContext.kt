package ca.adamhammer.shimmer.model

/**
 * Role for a message in a conversation history.
 */
/** Role for a participant in a conversation: SYSTEM prompt, USER input, or ASSISTANT response. */
enum class MessageRole { SYSTEM, USER, ASSISTANT }

/**
 * A single message in a conversation history.
 */
data class Message(
    val role: MessageRole,
    val content: String
)

/**
 * Type-safe key for storing values in [PromptContext.properties].
 *
 * Usage:
 * ```kotlin
 * val SESSION_ID = TypedKey<String>("sessionId")
 * val ctx = context.with(SESSION_ID, "abc-123")
 * val id: String? = ctx[SESSION_ID]
 * ```
 */
class TypedKey<T>(val name: String) {
    override fun equals(other: Any?) = other is TypedKey<*> && name == other.name
    override fun hashCode() = name.hashCode()
    override fun toString() = "TypedKey($name)"
}

/**
 * The assembled context that will be sent to an AI adapter.
 * Built by a [ca.adamhammer.shimmer.interfaces.ContextBuilder] and
 * optionally modified by [ca.adamhammer.shimmer.interfaces.Interceptor]s.
 */
data class PromptContext(
    val systemInstructions: String,
    val methodInvocation: String,
    val memory: Map<String, String>,
    val properties: Map<String, Any> = emptyMap(),
    val availableTools: List<ToolDefinition> = emptyList(),
    val conversationHistory: List<Message> = emptyList(),
    /** The name of the proxy method that initiated this request. Useful for routing. */
    val methodName: String = ""
) {
    /** Read a typed property, returning null if absent or of the wrong type. */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: TypedKey<T>): T? = properties[key.name] as? T

    /** Return a copy with the given typed property set. */
    fun <T : Any> with(key: TypedKey<T>, value: T): PromptContext =
        copy(properties = properties + (key.name to value))
}
