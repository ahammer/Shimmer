package com.adamhammer.ai_shimmer.mcp

import com.adamhammer.ai_shimmer.interfaces.ToolProvider
import com.adamhammer.ai_shimmer.model.ToolCall
import com.adamhammer.ai_shimmer.model.ToolDefinition
import com.adamhammer.ai_shimmer.model.ToolResult
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.spec.McpClientTransport
import io.modelcontextprotocol.spec.McpSchema
import java.time.Duration
import java.util.logging.Logger

/**
 * A [ToolProvider] that discovers and invokes tools from an MCP server.
 *
 * Wraps an [McpSyncClient] and translates between Shimmer's tool model and the MCP protocol.
 *
 * Usage:
 * ```kotlin
 * val provider = McpToolProvider(transport)
 * provider.connect()
 *
 * val instance = shimmer<MyAPI> {
 *     adapter(OpenAiAdapter())
 *     toolProvider(provider)
 * }
 * ```
 *
 * Implements [AutoCloseable] to cleanly shut down the MCP connection.
 */
class McpToolProvider(
    private val transport: McpClientTransport,
    private val clientName: String = "shimmer-mcp-client",
    private val clientVersion: String = "1.0.0",
    private val requestTimeout: Duration = Duration.ofSeconds(30)
) : ToolProvider, AutoCloseable {

    private val logger = Logger.getLogger(McpToolProvider::class.java.name)

    private lateinit var client: McpSyncClient
    private var cachedTools: List<McpSchema.Tool> = emptyList()

    /**
     * Connects to the MCP server, performs the initialization handshake,
     * and fetches the initial tool list.
     */
    fun connect() {
        client = McpClient.sync(transport)
            .requestTimeout(requestTimeout)
            .clientInfo(McpSchema.Implementation(clientName, clientVersion))
            .toolsChangeConsumer { notification ->
                logger.info { "MCP tools changed, refreshing tool list" }
                refreshTools()
            }
            .build()

        client.initialize()
        refreshTools()
        logger.info { "Connected to MCP server, discovered ${cachedTools.size} tool(s)" }
    }

    private fun refreshTools() {
        cachedTools = client.listTools().tools() ?: emptyList()
    }

    override fun listTools(): List<ToolDefinition> {
        return cachedTools.map { mcpTool ->
            ToolDefinition(
                name = mcpTool.name(),
                description = mcpTool.description() ?: "",
                inputSchema = serializeInputSchema(mcpTool.inputSchema()),
                outputSchema = null
            )
        }
    }

    override fun callTool(call: ToolCall): ToolResult {
        val arguments = parseArguments(call.arguments)

        val mcpResult = client.callTool(
            McpSchema.CallToolRequest(call.toolName, arguments)
        )

        val content = mcpResult.content()
            ?.filterIsInstance<McpSchema.TextContent>()
            ?.joinToString("\n") { it.text() }
            ?: ""

        return ToolResult(
            id = call.id,
            toolName = call.toolName,
            content = content,
            isError = mcpResult.isError() ?: false
        )
    }

    override fun close() {
        if (::client.isInitialized) {
            client.close()
        }
    }

    private val json = kotlinx.serialization.json.Json

    private fun serializeInputSchema(schema: McpSchema.JsonSchema?): String {
        if (schema == null) return """{"type":"object","properties":{}}"""

        val obj = kotlinx.serialization.json.buildJsonObject {
            put("type", kotlinx.serialization.json.JsonPrimitive(schema.type() ?: "object"))
            val properties = schema.properties()
            if (!properties.isNullOrEmpty()) {
                put("properties", anyToJsonElement(properties))
            }
            val required = schema.required()
            if (!required.isNullOrEmpty()) {
                put("required", kotlinx.serialization.json.JsonArray(
                    required.map { kotlinx.serialization.json.JsonPrimitive(it) }
                ))
            }
        }
        return json.encodeToString(obj)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyToJsonElement(value: Any?): kotlinx.serialization.json.JsonElement = when (value) {
        null -> kotlinx.serialization.json.JsonNull
        is String -> kotlinx.serialization.json.JsonPrimitive(value)
        is Number -> kotlinx.serialization.json.JsonPrimitive(value)
        is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
        is Map<*, *> -> kotlinx.serialization.json.buildJsonObject {
            (value as Map<String, Any?>).forEach { (k, v) -> put(k, anyToJsonElement(v)) }
        }
        is List<*> -> kotlinx.serialization.json.JsonArray(value.map { anyToJsonElement(it) })
        else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseArguments(json: String): Map<String, Any> {
        // Simple JSON object parsing — arguments are always a flat JSON object
        val trimmed = json.trim()
        if (trimmed == "{}" || trimmed.isBlank()) return emptyMap()

        // Use kotlinx.serialization for robust parsing
        val element = kotlinx.serialization.json.Json.parseToJsonElement(trimmed)
        if (element is kotlinx.serialization.json.JsonObject) {
            return element.entries.associate { (key, value) ->
                key to jsonElementToAny(value)
            }
        }
        return emptyMap()
    }

    private fun jsonElementToAny(element: kotlinx.serialization.json.JsonElement): Any = when (element) {
        is kotlinx.serialization.json.JsonPrimitive -> when {
            element.isString -> element.content
            element.content == "true" -> true
            element.content == "false" -> false
            element.content.contains(".") -> element.content.toDouble()
            else -> element.content.toLongOrNull() ?: element.content
        }
        is kotlinx.serialization.json.JsonArray -> element.map { jsonElementToAny(it) }
        is kotlinx.serialization.json.JsonObject -> element.entries.associate { (k, v) -> k to jsonElementToAny(v) }
        else -> element.toString()
    }
}
