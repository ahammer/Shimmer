package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.interfaces.ToolProvider
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ToolCall
import com.adamhammer.ai_shimmer.model.ToolDefinition
import com.adamhammer.ai_shimmer.model.ShimmerDeserializationException
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatCompletion
import com.openai.models.ChatCompletionCreateParams
import com.openai.models.ChatCompletionTool
import com.openai.models.ChatModel
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.ChatCompletionMessageParam
import com.openai.models.ChatCompletionToolMessageParam
import com.openai.models.ChatCompletionAssistantMessageParam
import com.openai.models.ChatCompletionSystemMessageParam
import com.openai.models.ChatCompletionUserMessageParam
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import java.util.logging.Logger

class OpenAiAdapter(
    private val model: ChatModel = ChatModel.GPT_4O_MINI,
    client: OpenAIClient? = null,
    private val maxToolRounds: Int = 10
) : ApiAdapter {
    private val logger = Logger.getLogger(OpenAiAdapter::class.java.name)

    private val client: OpenAIClient = client ?: run {
        val apiKey = System.getenv("OPENAI_API_KEY")
            ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set.")
        OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @OptIn(InternalSerializationApi::class)
    override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        return handleRequestInternal(context, resultClass, emptyList())
    }

    @OptIn(InternalSerializationApi::class)
    override fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        return handleRequestInternal(context, resultClass, toolProviders)
    }

    @OptIn(InternalSerializationApi::class)
    private fun <R : Any> handleRequestInternal(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        val memorySection = if (context.memory.isNotEmpty()) {
            val memoryJson = json.encodeToString(
                JsonObject(
                    context.memory.mapValues { entry ->
                        try {
                            json.parseToJsonElement(entry.value)
                        } catch (_: Exception) {
                            JsonPrimitive(entry.value)
                        }
                    }
                )
            )
            "\n\n# MEMORY\n$memoryJson"
        } else ""

        val userPrompt = """
# METHOD
${context.methodInvocation}$memorySection""".trimIndent()

        // Collect all available tools from providers and context
        val allToolDefs = buildList {
            addAll(context.availableTools)
            toolProviders.forEach { addAll(it.listTools()) }
        }.distinctBy { it.name }

        val hasTools = allToolDefs.isNotEmpty() && toolProviders.isNotEmpty()

        // Build the initial message list
        val messages = mutableListOf<ChatCompletionMessageParam>(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(context.systemInstructions).build()
            ),
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(userPrompt).build()
            )
        )

        val tools = if (hasTools) {
            allToolDefs.map { it.toOpenAiTool() }
        } else null

        // Multi-turn tool-calling loop
        for (round in 1..maxToolRounds) {
            val paramsBuilder = ChatCompletionCreateParams.builder()
                .model(model)

            messages.forEach { paramsBuilder.addMessage(it) }

            if (tools != null) {
                tools.forEach { paramsBuilder.addTool(it) }
            }

            val params = paramsBuilder.build()

            logger.fine { "Round $round — sending request with ${messages.size} message(s), ${tools?.size ?: 0} tool(s)" }

            val chatCompletion: ChatCompletion = client.chat().completions().create(params)
            val choice = chatCompletion.choices().firstOrNull()
                ?: throw ShimmerDeserializationException("No response from OpenAI API")

            val assistantMessage = choice.message()
            val toolCalls = assistantMessage.toolCalls().orElse(null)

            if (toolCalls.isNullOrEmpty()) {
                // No tool calls — extract the final response
                val completionText = assistantMessage.content().orElse(null)?.trim()
                    ?: throw ShimmerDeserializationException("No content in final response from OpenAI API")

                return deserializeResponse(completionText, resultClass)
            }

            // Add the assistant message (with tool_calls) to the conversation
            messages.add(ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                    .toolCalls(toolCalls)
                    .build()
            ))

            // Dispatch each tool call to the appropriate provider
            for (tc in toolCalls) {
                val functionCall = tc.function()
                val shimmerToolCall = ToolCall(
                    id = tc.id(),
                    toolName = functionCall.name(),
                    arguments = functionCall.arguments()
                )

                val toolResult = dispatchToolCall(shimmerToolCall, toolProviders)

                messages.add(ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                        .toolCallId(tc.id())
                        .content(toolResult.content)
                        .build()
                ))
            }
        }

        throw ShimmerDeserializationException("Exceeded maximum tool-calling rounds ($maxToolRounds)")
    }

    private fun dispatchToolCall(call: ToolCall, providers: List<ToolProvider>): com.adamhammer.ai_shimmer.model.ToolResult {
        for (provider in providers) {
            val tools = provider.listTools()
            if (tools.any { it.name == call.toolName }) {
                return provider.callTool(call)
            }
        }
        return com.adamhammer.ai_shimmer.model.ToolResult(
            id = call.id,
            toolName = call.toolName,
            content = "Error: No provider found for tool '${call.toolName}'",
            isError = true
        )
    }

    private fun ToolDefinition.toOpenAiTool(): ChatCompletionTool {
        val schemaElement = json.parseToJsonElement(inputSchema).jsonObject

        val properties = schemaElement["properties"]?.jsonObject?.entries?.associate { (key, value) ->
            key to value
        } ?: emptyMap()

        val required = schemaElement["required"]?.let { req ->
            val arr = req as? kotlinx.serialization.json.JsonArray ?: return@let emptyList()
            arr.mapNotNull { (it as? JsonPrimitive)?.content }
        } ?: emptyList()

        return ChatCompletionTool.builder()
            .function(
                FunctionDefinition.builder()
                    .name(name)
                    .description(description)
                    .parameters(
                        FunctionParameters.builder()
                            .putAdditionalProperty("type", com.openai.core.JsonValue.from("object"))
                            .putAdditionalProperty("properties", com.openai.core.JsonValue.from(
                                properties.entries.associate { (k, v) -> k to json.parseToJsonElement(v.toString()) }
                            ))
                            .putAdditionalProperty("required", com.openai.core.JsonValue.from(required))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    @OptIn(InternalSerializationApi::class)
    private fun <R : Any> deserializeResponse(completionText: String, resultClass: KClass<R>): R {
        logger.fine { "Response:\n$completionText" }

        if (resultClass == String::class) {
            @Suppress("UNCHECKED_CAST")
            val cleanedText = when {
                completionText.trim() == "\"Text\"" -> ""
                completionText.trim().startsWith("# RESULT") -> {
                    val contentStart = completionText.indexOf("# RESULT") + "# RESULT".length
                    completionText.substring(contentStart).trim().trim('"')
                }
                else -> completionText
            }
            return cleanedText as R
        }

        val jsonResponse = extractJson(completionText)

        try {
            @Suppress("UNCHECKED_CAST")
            val serializer = resultClass.serializer() as KSerializer<R>
            return json.decodeFromString(serializer, jsonResponse)
        } catch (e: Exception) {
            throw ShimmerDeserializationException(
                "Failed to deserialize response into ${resultClass.simpleName}: ${e.message}\nRaw response: $jsonResponse",
                e
            )
        }
    }

    internal fun extractJson(text: String): String {
        // Strip markdown code fences (```json ... ``` or ``` ... ```)
        val fencePattern = Regex("```(?:json|JSON)?\\s*\\n?(.*?)\\n?\\s*```", RegexOption.DOT_MATCHES_ALL)
        val stripped = fencePattern.find(text)?.groupValues?.get(1)?.trim() ?: text

        val objectStartIndex = stripped.indexOf('{')
        val objectEndIndex = stripped.lastIndexOf('}')
        if (objectStartIndex != -1 && objectEndIndex != -1 && objectStartIndex < objectEndIndex) {
            return stripped.substring(objectStartIndex, objectEndIndex + 1)
        }

        val arrayStartIndex = stripped.indexOf('[')
        val arrayEndIndex = stripped.lastIndexOf(']')
        if (arrayStartIndex != -1 && arrayEndIndex != -1 && arrayStartIndex < arrayEndIndex) {
            return stripped.substring(arrayStartIndex, arrayEndIndex + 1)
        }

        return stripped
    }
}
