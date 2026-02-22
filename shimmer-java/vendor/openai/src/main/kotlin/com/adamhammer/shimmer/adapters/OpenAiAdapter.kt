package com.adamhammer.shimmer.adapters

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.model.MessageRole
import com.adamhammer.shimmer.model.ToolCall
import com.adamhammer.shimmer.model.ToolDefinition
import com.adamhammer.shimmer.model.ShimmerDeserializationException
import com.adamhammer.shimmer.model.ImageResult
import com.adamhammer.shimmer.utils.toJsonSchema
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.models.ChatCompletion
import com.openai.models.ChatCompletionCreateParams
import com.openai.models.ChatCompletionMessageToolCall
import com.openai.models.ChatCompletionTool
import com.openai.models.ChatModel
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.ChatCompletionMessageParam
import com.openai.models.ChatCompletionToolMessageParam
import com.openai.models.ChatCompletionAssistantMessageParam
import com.openai.models.ChatCompletionSystemMessageParam
import com.openai.models.ChatCompletionUserMessageParam
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.ImageGenerateParams
import com.openai.models.ImageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import java.util.logging.Logger

/**
 * [ApiAdapter] implementation for the OpenAI API.
 *
 * Supports chat completions, multi-turn tool calling, streaming, image generation,
 * and structured JSON output via OpenAI's `response_format: json_schema`.
 *
 * @param model the chat model to use for completions (default: GPT-4o-mini)
 * @param client an optional pre-configured [OpenAIClient]; if null, one is created from the `OPENAI_API_KEY` env var
 * @param maxToolRounds the maximum number of tool-calling round-trips before giving up
 * @param imageModel the model to use for image generation requests
 * @param imageSize the default size for generated images
 */
@Suppress("TooManyFunctions")
class OpenAiAdapter(
    private val model: ChatModel = ChatModel.GPT_4O_MINI,
    client: OpenAIClient? = null,
    private val maxToolRounds: Int = 10,
    private val imageModel: ImageModel = ImageModel.DALL_E_3,
    private val imageSize: ImageGenerateParams.Size = ImageGenerateParams.Size._1024X1024
) : ApiAdapter {
    private val logger = Logger.getLogger(OpenAiAdapter::class.java.name)

    private val client: OpenAIClient = client ?: run {
        val apiKey = checkNotNull(System.getenv("OPENAI_API_KEY")) {
            "OPENAI_API_KEY environment variable not set."
        }
        OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        return handleRequestInternal(context, resultClass, emptyList())
    }

    override suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        return handleRequestInternal(context, resultClass, toolProviders)
    }

    private suspend fun <R : Any> handleRequestInternal(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        if (resultClass == ImageResult::class) {
            @Suppress("UNCHECKED_CAST")
            return handleImageRequest(context) as R
        }

        val userPrompt = buildUserPrompt(context)
        val allToolDefs = collectToolDefs(context, toolProviders)
        val hasTools = allToolDefs.isNotEmpty() && toolProviders.isNotEmpty()
        val toolIndex = buildToolIndex(hasTools, toolProviders)
        val messages = buildMessageList(context, userPrompt)
        val tools = if (hasTools) allToolDefs.map { it.toOpenAiTool() } else null

        val responseFormat = if (resultClass != String::class) {
            buildJsonSchemaResponseFormat(resultClass)
        } else null

        for (round in 1..maxToolRounds) {
            val params = buildParams(messages, tools, responseFormat)

            logger.fine { "Round $round — sending request with ${messages.size} message(s), ${tools?.size ?: 0} tool(s)" }

            val chatCompletion: ChatCompletion = client.chat().completions().create(params)
            val choice = chatCompletion.choices().firstOrNull()
                ?: throw ShimmerDeserializationException("No response from OpenAI API", context = context)

            val assistantMessage = choice.message()
            val toolCalls = assistantMessage.toolCalls().orElse(null)

            if (toolCalls.isNullOrEmpty()) {
                val completionText = assistantMessage.content().orElse(null)?.trim()
                    ?: throw ShimmerDeserializationException("No content in final response from OpenAI API", context = context)
                return deserializeResponse(completionText, resultClass, context)
            }

            messages.add(ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder().toolCalls(toolCalls).build()
            ))

            for (tc in toolCalls) {
                appendToolResult(messages, tc, toolIndex)
            }
        }

        throw ShimmerDeserializationException("Exceeded maximum tool-calling rounds ($maxToolRounds)", context = context)
    }

    private fun handleImageRequest(context: PromptContext): ImageResult {
        val promptText = extractPromptFromContext(context) ?: run {
            val promptMessages = buildImagePromptMessages(context)
            val promptParams = buildParams(messages = promptMessages, tools = null, responseFormat = null)

            val promptCompletion: ChatCompletion = client.chat().completions().create(promptParams)
            promptCompletion.choices().firstOrNull()?.message()?.content()?.orElse(null)?.trim()
                ?.trim('"')
                ?.ifBlank { null }
                ?: throw ShimmerDeserializationException("No image prompt returned from OpenAI", context = context)
        }

        val imageResponse = client.images().generate(
            ImageGenerateParams.builder()
                .model(imageModel)
                .prompt(promptText)
                .responseFormat(ImageGenerateParams.ResponseFormat.B64_JSON)
                .size(imageSize)
                .build()
        )

        val imageData = imageResponse.data().firstOrNull()
            ?: throw ShimmerDeserializationException("No image data returned from OpenAI", context = context)

        val base64 = imageData.b64Json().orElse(null)
            ?: throw ShimmerDeserializationException("No base64 image data returned from OpenAI", context = context)

        return ImageResult(
            base64 = base64,
            prompt = promptText,
            revisedPrompt = imageData.revisedPrompt().orElse("")
        )
    }

    private fun extractPromptFromContext(context: PromptContext): String? {
        try {
            val jsonElement = json.parseToJsonElement(context.methodInvocation)
            if (jsonElement is JsonObject) {
                val method = jsonElement["method"]?.jsonPrimitive?.content
                if (method != null && method.startsWith("generateImage")) {
                    val parameters = jsonElement["parameters"]?.jsonArray
                    if (parameters != null && parameters.isNotEmpty()) {
                        val firstParam = parameters[0].jsonObject
                        return firstParam["value"]?.jsonPrimitive?.content
                    }
                }
            }
        } catch (e: Exception) {
            logger.warning("Failed to extract prompt from context: ${e.message}")
        }
        return null
    }

    private fun buildImagePromptMessages(context: PromptContext): MutableList<ChatCompletionMessageParam> {
        val promptMessages = buildMessageList(context, buildUserPrompt(context))
        val instruction = "Generate a vivid DALL-E prompt for this exact RPG scene context. " +
            "Return only the prompt text, no JSON, no markdown, no commentary."
        promptMessages.add(
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(instruction).build()
            )
        )
        return promptMessages
    }

    private suspend fun appendToolResult(
        messages: MutableList<ChatCompletionMessageParam>,
        tc: ChatCompletionMessageToolCall,
        toolIndex: Map<String, ToolProvider>
    ) {
        val functionCall = tc.function()
        val shimmerToolCall = ToolCall(
            id = tc.id(),
            toolName = functionCall.name(),
            arguments = functionCall.arguments()
        )
        val toolResult = dispatchToolCall(shimmerToolCall, toolIndex)
        messages.add(ChatCompletionMessageParam.ofTool(
            ChatCompletionToolMessageParam.builder()
                .toolCallId(tc.id())
                .content(toolResult.content)
                .build()
        ))
    }

    private fun buildUserPrompt(context: PromptContext): String {
        val memorySection = if (context.memory.isNotEmpty()) {
            val memoryJson = json.encodeToString(
                JsonObject(context.memory.mapValues { entry ->
                    try { json.parseToJsonElement(entry.value) }
                    catch (_: Exception) { JsonPrimitive(entry.value) }
                })
            )
            "\n\n# MEMORY\n$memoryJson"
        } else ""
        return "# METHOD\n${context.methodInvocation}$memorySection"
    }

    private fun collectToolDefs(
        context: PromptContext,
        toolProviders: List<ToolProvider>
    ): List<ToolDefinition> = buildList {
        addAll(context.availableTools)
        toolProviders.forEach { addAll(it.listTools()) }
    }.distinctBy { it.name }

    private fun buildToolIndex(
        hasTools: Boolean,
        toolProviders: List<ToolProvider>
    ): Map<String, ToolProvider> = if (hasTools) {
        toolProviders.flatMap { p -> p.listTools().map { it.name to p } }.toMap()
    } else emptyMap()

    private fun buildParams(
        messages: List<ChatCompletionMessageParam>,
        tools: List<ChatCompletionTool>?,
        responseFormat: ChatCompletionCreateParams.ResponseFormat? = null
    ): ChatCompletionCreateParams {
        val builder = ChatCompletionCreateParams.builder().model(model)
        messages.forEach { builder.addMessage(it) }
        tools?.forEach { builder.addTool(it) }
        if (responseFormat != null) builder.responseFormat(responseFormat)
        return builder.build()
    }

    private suspend fun dispatchToolCall(
        call: ToolCall, 
        toolIndex: Map<String, ToolProvider>
    ): com.adamhammer.shimmer.model.ToolResult {
        val provider = toolIndex[call.toolName]
            ?: return com.adamhammer.shimmer.model.ToolResult(
                id = call.id,
                toolName = call.toolName,
                content = "Error: No provider found for tool '${call.toolName}'",
                isError = true
            )
        return provider.callTool(call)
    }

    override fun handleRequestStreaming(context: PromptContext): Flow<String> =
        handleRequestStreaming(context, emptyList())

    override fun handleRequestStreaming(
        context: PromptContext,
        toolProviders: List<ToolProvider>
    ): Flow<String> = flow {
        val userPrompt = buildUserPrompt(context)
        val allToolDefs = collectToolDefs(context, toolProviders)
        val hasTools = allToolDefs.isNotEmpty() && toolProviders.isNotEmpty()
        val toolIndex = buildToolIndex(hasTools, toolProviders)
        val messages = buildMessageList(context, userPrompt)
        val tools = if (hasTools) allToolDefs.map { it.toOpenAiTool() } else null

        for (round in 1..maxToolRounds) {
            val params = buildParams(messages, tools)
            val streamResult = processStreamChunks(params) { text -> emit(text) }

            if (!streamResult.hasToolCalls) return@flow

            messages.add(ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                    .toolCalls(streamResult.toolCallObjects)
                    .build()
            ))

            for (acc in streamResult.accumulatedCalls) {
                val shimmerToolCall = ToolCall(id = acc.id, toolName = acc.name, arguments = acc.args)
                val toolResult = dispatchToolCall(shimmerToolCall, toolIndex)
                messages.add(ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                        .toolCallId(acc.id)
                        .content(toolResult.content)
                        .build()
                ))
            }
        }
    }.flowOn(Dispatchers.IO)

    private data class AccumulatedToolCall(val id: String, val name: String, val args: String)

    private data class StreamResult(
        val hasToolCalls: Boolean,
        val accumulatedCalls: List<AccumulatedToolCall>,
        val toolCallObjects: List<ChatCompletionMessageToolCall>
    )

    private class AccToolCall(var id: String = "", var name: String = "", val arguments: StringBuilder = StringBuilder())

    private fun processStreamChunks(
        params: ChatCompletionCreateParams,
        emitText: suspend (String) -> Unit
    ): StreamResult {
        val accToolCalls = mutableMapOf<Long, AccToolCall>()
        var hasToolCallsInStream = false

        client.chat().completions().createStreaming(params).use { streamResponse ->
            val iter = streamResponse.stream().iterator()
            while (iter.hasNext()) {
                val chunk = iter.next()
                val delta = chunk.choices().firstOrNull()?.delta() ?: continue

                val text = delta.content().orElse(null)
                if (!text.isNullOrEmpty()) {
                    kotlinx.coroutines.runBlocking { emitText(text) }
                }

                if (delta.toolCalls().orElse(null) != null) {
                    hasToolCallsInStream = true
                    accumulateDeltaToolCalls(delta.toolCalls().get(), accToolCalls)
                }
            }
        }

        val sortedEntries = accToolCalls.entries.sortedBy { it.key }
        return StreamResult(
            hasToolCalls = hasToolCallsInStream,
            accumulatedCalls = sortedEntries.map { (_, acc) ->
                AccumulatedToolCall(acc.id, acc.name, acc.arguments.toString())
            },
            toolCallObjects = sortedEntries.map { (_, acc) ->
                ChatCompletionMessageToolCall.builder()
                    .id(acc.id)
                    .function(
                        ChatCompletionMessageToolCall.Function.builder()
                            .name(acc.name)
                            .arguments(acc.arguments.toString())
                            .build()
                    )
                    .build()
            }
        )
    }

    private fun accumulateDeltaToolCalls(
        deltaToolCalls: List<com.openai.models.ChatCompletionChunk.Choice.Delta.ToolCall>,
        accToolCalls: MutableMap<Long, AccToolCall>
    ) {
        for (tc in deltaToolCalls) {
            val acc = accToolCalls.getOrPut(tc.index()) { AccToolCall() }
            tc.id().orElse(null)?.let { acc.id = it }
            tc.function().orElse(null)?.let { fn ->
                fn.name().orElse(null)?.let { acc.name = it }
                fn.arguments().orElse(null)?.let { acc.arguments.append(it) }
            }
        }
    }

    private fun <R : Any> buildJsonSchemaResponseFormat(
        resultClass: KClass<R>
    ): ChatCompletionCreateParams.ResponseFormat {
        val schema = resultClass.toJsonSchema()
        val schemaMap = jsonElementToNative(schema)

        return ChatCompletionCreateParams.ResponseFormat.ofJsonSchema(
            ResponseFormatJsonSchema.builder()
                .jsonSchema(
                    ResponseFormatJsonSchema.JsonSchema.builder()
                        .name(resultClass.simpleName ?: "response")
                        .schema(JsonValue.from(schemaMap))
                        .strict(true)
                        .build()
                )
                .build()
        )
    }

    private fun jsonElementToNative(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.content == "true" -> true
            element.content == "false" -> false
            element.content.toLongOrNull() != null -> element.content.toLong()
            element.content.toDoubleOrNull() != null -> element.content.toDouble()
            else -> element.content
        }
        is JsonArray -> element.map { jsonElementToNative(it) }
        is JsonObject -> element.entries.associate { (k, v) -> k to jsonElementToNative(v) }
    }

    private fun ToolDefinition.toOpenAiTool(): ChatCompletionTool {
        val schemaElement = json.parseToJsonElement(inputSchema).jsonObject

        val schemaNative = jsonElementToNative(schemaElement)
        val schemaMap = schemaNative as? Map<*, *> ?: emptyMap<String, Any?>()
        val parametersBuilder = FunctionParameters.builder()
        schemaMap.forEach { (key, value) ->
            if (key != null) {
                parametersBuilder.putAdditionalProperty(key.toString(), com.openai.core.JsonValue.from(value))
            }
        }

        if (!schemaMap.containsKey("type")) {
            parametersBuilder.putAdditionalProperty("type", com.openai.core.JsonValue.from("object"))
        }

        return ChatCompletionTool.builder()
            .function(
                FunctionDefinition.builder()
                    .name(name)
                    .description(description)
                    .parameters(parametersBuilder.build())
                    .build()
            )
            .build()
    }

    private fun <R : Any> deserializeResponse(completionText: String, resultClass: KClass<R>, context: PromptContext): R {
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
            val serializer = serializer(resultClass.createType()) as kotlinx.serialization.KSerializer<R>
            return json.decodeFromString(serializer, jsonResponse)
        } catch (e: Exception) {
            throw ShimmerDeserializationException(
                "Failed to deserialize response into ${resultClass.simpleName}: ${e.message}\nRaw response: $jsonResponse",
                e,
                context
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

    private fun buildMessageList(
        context: PromptContext,
        userPrompt: String
    ): MutableList<ChatCompletionMessageParam> {
        val messages = mutableListOf<ChatCompletionMessageParam>(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(context.systemInstructions).build()
            )
        )
        for (msg in context.conversationHistory) {
            when (msg.role) {
                MessageRole.SYSTEM -> messages.add(
                    ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder().content(msg.content).build()
                    )
                )
                MessageRole.USER -> messages.add(
                    ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content(msg.content).build()
                    )
                )
                MessageRole.ASSISTANT -> messages.add(
                    ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder().content(msg.content).build()
                    )
                )
            }
        }
        messages.add(
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(userPrompt).build()
            )
        )
        return messages
    }
}
