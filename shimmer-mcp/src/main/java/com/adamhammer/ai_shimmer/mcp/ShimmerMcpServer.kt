package com.adamhammer.ai_shimmer.mcp

import com.adamhammer.ai_shimmer.annotations.AiOperation
import com.adamhammer.ai_shimmer.annotations.AiParameter
import com.adamhammer.ai_shimmer.utils.toToolDefinitions
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpServerTransportProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.lang.reflect.Method
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * Exposes a Kotlin interface's annotated methods as MCP tools.
 *
 * Takes an implementation instance and its interface class, introspects the
 * annotations to generate MCP tool definitions, and dispatches incoming tool
 * calls to the matching method on the implementation.
 *
 * Usage:
 * ```kotlin
 * val server = ShimmerMcpServer.builder()
 *     .transportProvider(StdioServerTransportProvider(objectMapper))
 *     .expose(MyApiImpl(), MyApi::class)
 *     .serverInfo("my-server", "1.0.0")
 *     .build()
 * ```
 */
class ShimmerMcpServer private constructor(
    private val server: McpSyncServer
) : AutoCloseable {

    override fun close() {
        server.close()
    }

    class Builder {
        private val logger = Logger.getLogger(ShimmerMcpServer::class.java.name)
        private var transportProvider: McpServerTransportProvider? = null
        private var serverName: String = "shimmer-mcp-server"
        private var serverVersion: String = "1.0.0"
        private val toolSpecs = mutableListOf<McpServerFeatures.SyncToolSpecification>()

        /** Set the MCP transport provider (stdio, SSE, etc.). */
        fun transportProvider(provider: McpServerTransportProvider): Builder {
            this.transportProvider = provider
            return this
        }

        /** Set the server identity. */
        fun serverInfo(name: String, version: String): Builder {
            this.serverName = name
            this.serverVersion = version
            return this
        }

        /**
         * Expose an implementation's annotated methods as MCP tools.
         *
         * @param implementation the object that handles tool calls
         * @param apiInterface the annotated interface class describing available operations
         */
        fun <T : Any> expose(implementation: T, apiInterface: KClass<T>): Builder {
            val toolDefinitions = apiInterface.toToolDefinitions()
            val methods = apiInterface.java.declaredMethods
                .filter { it.isAnnotationPresent(AiOperation::class.java) }

            for (method in methods) {
                val toolDef = toolDefinitions.find { it.name == method.name } ?: continue
                val mcpTool = buildMcpTool(method, toolDef.description, toolDef.inputSchema)
                val handler = buildToolHandler(implementation, method)
                toolSpecs.add(
                    McpServerFeatures.SyncToolSpecification.builder()
                        .tool(mcpTool)
                        .callHandler(handler)
                        .build()
                )
            }

            logger.info { "Registered ${toolSpecs.size} tool(s) from ${apiInterface.simpleName}" }
            return this
        }

        /** Convenience overload using reified type. */
        inline fun <reified T : Any> expose(implementation: T): Builder =
            expose(implementation, T::class)

        fun build(): ShimmerMcpServer {
            val transport = transportProvider
                ?: throw IllegalStateException("Transport provider must be set")

            var spec = McpServer.sync(transport)
                .serverInfo(serverName, serverVersion)
                .capabilities(
                    McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build()
                )

            for (toolSpec in toolSpecs) {
                spec = spec.tools(toolSpec)
            }

            return ShimmerMcpServer(spec.build())
        }

        private fun buildMcpTool(method: Method, description: String, inputSchemaJson: String): McpSchema.Tool {
            val schemaElement = Json.parseToJsonElement(inputSchemaJson).jsonObject

            val properties = mutableMapOf<String, Any>()
            val schemaProps = schemaElement["properties"]
            if (schemaProps is JsonObject) {
                for ((key, value) in schemaProps) {
                    if (value is JsonObject) {
                        properties[key] = value.entries.associate { (k, v) ->
                            k to when (v) {
                                is JsonPrimitive -> v.content
                                else -> v.toString()
                            }
                        }
                    }
                }
            }

            val requiredList = schemaElement["required"]?.let { req ->
                val arr = req as? kotlinx.serialization.json.JsonArray ?: return@let emptyList()
                arr.mapNotNull { (it as? JsonPrimitive)?.content }
            } ?: emptyList()

            return McpSchema.Tool.builder()
                .name(method.name)
                .description(description)
                .inputSchema(
                    McpSchema.JsonSchema("object", properties, requiredList, null, null, null)
                )
                .build()
        }

        private fun <T : Any> buildToolHandler(
            implementation: T,
            method: Method
        ): java.util.function.BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> {
            return java.util.function.BiFunction { _, request ->
                try {
                    val args = resolveMethodArguments(method, request.arguments() ?: emptyMap())
                    val result = method.invoke(implementation, *args)

                    // Unwrap Future results
                    val unwrapped = if (result is java.util.concurrent.Future<*>) {
                        result.get()
                    } else {
                        result
                    }

                    val content = unwrapped?.toString() ?: ""

                    McpSchema.CallToolResult.builder()
                        .addTextContent(content)
                        .isError(false)
                        .build()
                } catch (e: Exception) {
                    val cause = if (e is java.lang.reflect.InvocationTargetException) e.cause ?: e else e
                    McpSchema.CallToolResult.builder()
                        .addTextContent("Error: ${cause.message}")
                        .isError(true)
                        .build()
                }
            }
        }

        private fun resolveMethodArguments(method: Method, arguments: Map<String, Any>): Array<Any?> {
            return method.parameters.map { param ->
                val value = arguments[param.name]
                when {
                    value == null -> null
                    param.type == String::class.java -> value.toString()
                    param.type == Int::class.java || param.type == Integer::class.java ->
                        (value as? Number)?.toInt() ?: value.toString().toIntOrNull()
                    param.type == Long::class.java || param.type == java.lang.Long::class.java ->
                        (value as? Number)?.toLong() ?: value.toString().toLongOrNull()
                    param.type == Double::class.java || param.type == java.lang.Double::class.java ->
                        (value as? Number)?.toDouble() ?: value.toString().toDoubleOrNull()
                    param.type == Float::class.java || param.type == java.lang.Float::class.java ->
                        (value as? Number)?.toFloat() ?: value.toString().toFloatOrNull()
                    param.type == Boolean::class.java || param.type == java.lang.Boolean::class.java ->
                        value as? Boolean ?: value.toString().toBoolean()
                    else -> value
                }
            }.toTypedArray()
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
