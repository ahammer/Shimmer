package ca.adamhammer.shimmer.adapters

import ca.adamhammer.shimmer.interfaces.ApiAdapter
import ca.adamhammer.shimmer.interfaces.ToolProvider
import ca.adamhammer.shimmer.model.AdapterResponse
import ca.adamhammer.shimmer.model.ImageResult
import ca.adamhammer.shimmer.model.MessageRole
import ca.adamhammer.shimmer.model.ModelPricing
import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.model.ShimmerAdapterException
import ca.adamhammer.shimmer.model.ShimmerDeserializationException
import ca.adamhammer.shimmer.model.ToolCall
import ca.adamhammer.shimmer.model.ToolDefinition
import ca.adamhammer.shimmer.model.ToolResult
import ca.adamhammer.shimmer.model.UsageInfo
import ca.adamhammer.shimmer.utils.toJsonSchema
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlock
import com.anthropic.models.messages.ToolUseBlockParam
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * [ApiAdapter] implementation for the Anthropic Claude API.
 *
 * Supports chat completions, multi-turn tool calling, and streaming.
 * Structured JSON output is achieved via system prompt injection (Claude does not
 * have a native JSON Schema mode). Image generation is not supported — use
 * a routing adapter to direct [ImageResult] requests to a capable adapter.
 *
 * @param model the Claude model to use (default: claude-sonnet-4-20250514)
 * @param client an optional pre-configured [AnthropicClient]; if null, one is created
 *   from the `ANTHROPIC_API_KEY` env var
 * @param maxToolRounds the maximum number of tool-calling round-trips before giving up
 * @param maxTokens the maximum number of output tokens (required by Anthropic API)
 */
@Suppress("TooManyFunctions")
class ClaudeAdapter(
    private val model: String = "claude-sonnet-4-20250514",
    client: AnthropicClient? = null,
    private val maxToolRounds: Int = 10,
    private val maxTokens: Long = 8192,
    private val pricing: ModelPricing = ModelPricing()
) : ApiAdapter {
    private val logger = Logger.getLogger(ClaudeAdapter::class.java.name)

    private val client: AnthropicClient = client ?: run {
        val apiKey = System.getenv("ANTHROPIC_API_KEY")
            ?: error("ANTHROPIC_API_KEY environment variable not set.")
        AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    // ── ApiAdapter overrides ───────────────────────────────────────────────

    override suspend fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        return handleRequestInternal(context, resultClass, emptyList()).result
    }

    override suspend fun <R : Any> handleRequest(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): R {
        return handleRequestInternal(context, resultClass, toolProviders).result
    }

    override suspend fun <R : Any> handleRequestWithUsage(
        context: PromptContext,
        resultClass: KClass<R>
    ): AdapterResponse<R> {
        return handleRequestInternal(context, resultClass, emptyList())
    }

    override suspend fun <R : Any> handleRequestWithUsage(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): AdapterResponse<R> {
        return handleRequestInternal(context, resultClass, toolProviders)
    }

    @Suppress("NestedBlockDepth")
    private suspend fun <R : Any> handleRequestInternal(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): AdapterResponse<R> {
        if (resultClass == ImageResult::class) {
            throw ShimmerAdapterException(
                "Claude does not support image generation. " +
                    "Use RoutingAdapter to route ImageResult requests to an adapter with image support.",
                context = context
            )
        }

        val userPrompt = buildUserPrompt(context)
        val allToolDefs = collectToolDefs(context, toolProviders)
        val hasTools = allToolDefs.isNotEmpty() && toolProviders.isNotEmpty()
        val toolIndex = buildToolIndex(hasTools, toolProviders)
        val messageAdders = buildInitialMessageAdders(context, userPrompt)
        val systemPrompt = buildSystemPrompt(context, resultClass)
        val tools = if (hasTools) allToolDefs.map { it.toAnthropicTool() } else null

        var totalInputTokens = 0L
        var totalOutputTokens = 0L

        for (round in 1..maxToolRounds) {
            logger.fine { "Round $round — sending request with ${messageAdders.size} message adder(s)" }

            val params = buildParams(messageAdders, systemPrompt, tools)
            val message: Message = client.messages().create(params)

            totalInputTokens += message.usage().inputTokens()
            totalOutputTokens += message.usage().outputTokens()

            val toolUseBlocks = extractToolUseBlocks(message)

            if (toolUseBlocks.isEmpty()) {
                val text = extractTextContent(message)
                if (text.isBlank()) {
                    throw ShimmerDeserializationException(
                        "No text content in response from Claude API", context = context
                    )
                }
                val result = deserializeResponse(text.trim(), resultClass, context)
                val usageInfo = UsageInfo(
                    model = model,
                    inputTokens = totalInputTokens,
                    outputTokens = totalOutputTokens,
                    inputCostPerToken = pricing.inputCostPerToken,
                    outputCostPerToken = pricing.outputCostPerToken
                )
                return AdapterResponse(result, usageInfo)
            }

            // Replay the full assistant message (all content blocks including tool_use)
            val assistantBlocks = message.content().map { contentBlockToParam(it) }
            messageAdders.add { it.addAssistantMessageOfBlockParams(assistantBlocks) }

            // Execute tool calls and send results as a user message
            val resultBlocks = mutableListOf<ContentBlockParam>()
            for (toolUse in toolUseBlocks) {
                val argsJson = jsonValueToString(toolUse._input())
                val shimmerCall = ToolCall(
                    id = toolUse.id(),
                    toolName = toolUse.name(),
                    arguments = argsJson
                )
                val result = dispatchToolCall(shimmerCall, toolIndex)
                resultBlocks.add(
                    ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                            .toolUseId(toolUse.id())
                            .content(result.content)
                            .build()
                    )
                )
            }
            messageAdders.add { it.addUserMessageOfBlockParams(resultBlocks) }
        }

        throw ShimmerDeserializationException(
            "Exceeded maximum tool-calling rounds ($maxToolRounds)", context = context
        )
    }

    override fun handleRequestStreaming(context: PromptContext): Flow<String> =
        handleRequestStreaming(context, emptyList())

    @Suppress("NestedBlockDepth")
    override fun handleRequestStreaming(
        context: PromptContext,
        toolProviders: List<ToolProvider>
    ): Flow<String> = flow {
        val userPrompt = buildUserPrompt(context)
        val allToolDefs = collectToolDefs(context, toolProviders)
        val hasTools = allToolDefs.isNotEmpty() && toolProviders.isNotEmpty()
        val toolIndex = buildToolIndex(hasTools, toolProviders)
        val messageAdders = buildInitialMessageAdders(context, userPrompt)
        val systemPrompt = buildSystemPrompt(context, String::class)
        val tools = if (hasTools) allToolDefs.map { it.toAnthropicTool() } else null

        for (round in 1..maxToolRounds) {
            val params = buildParams(messageAdders, systemPrompt, tools)

            val message = client.messages().createStreaming(params).use { streamResponse ->
                val accumulator = MessageAccumulator.create()
                streamResponse.stream()
                    .peek { accumulator.accumulate(it) }
                    .flatMap { it.contentBlockDelta().stream() }
                    .flatMap { it.delta().text().stream() }
                    .forEach { textDelta ->
                        kotlinx.coroutines.runBlocking { emit(textDelta.text()) }
                    }
                accumulator.message()
            }

            val toolUseBlocks = extractToolUseBlocks(message)
            if (toolUseBlocks.isEmpty()) return@flow

            val assistantBlocks = message.content().map { contentBlockToParam(it) }
            messageAdders.add { it.addAssistantMessageOfBlockParams(assistantBlocks) }

            val resultBlocks = mutableListOf<ContentBlockParam>()
            for (toolUse in toolUseBlocks) {
                val argsJson = jsonValueToString(toolUse._input())
                val shimmerCall = ToolCall(
                    id = toolUse.id(),
                    toolName = toolUse.name(),
                    arguments = argsJson
                )
                val result = dispatchToolCall(shimmerCall, toolIndex)
                resultBlocks.add(
                    ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                            .toolUseId(toolUse.id())
                            .content(result.content)
                            .build()
                    )
                )
            }
            messageAdders.add { it.addUserMessageOfBlockParams(resultBlocks) }
        }
    }.flowOn(Dispatchers.IO)

    // ── Prompt & message building ──────────────────────────────────────────

    internal fun buildUserPrompt(context: PromptContext): String {
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

    internal fun <R : Any> buildSystemPrompt(context: PromptContext, resultClass: KClass<R>): String {
        val base = context.systemInstructions
        if (resultClass == String::class || resultClass == ImageResult::class) return base
        val schema = resultClass.toJsonSchema()
        val schemaStr = json.encodeToString(schema)
        return "$base\n\nYou must respond with valid JSON matching this schema:\n$schemaStr\n\n" +
            "Respond only with the JSON object, no other text."
    }

    private fun buildInitialMessageAdders(
        context: PromptContext,
        userPrompt: String
    ): MutableList<(MessageCreateParams.Builder) -> Unit> {
        val adders = mutableListOf<(MessageCreateParams.Builder) -> Unit>()
        for (msg in context.conversationHistory) {
            when (msg.role) {
                MessageRole.SYSTEM -> {} // handled via .system() param
                MessageRole.USER -> {
                    val content = msg.content
                    adders.add { it.addUserMessage(content) }
                }
                MessageRole.ASSISTANT -> {
                    val content = msg.content
                    adders.add { it.addAssistantMessage(content) }
                }
            }
        }
        adders.add { it.addUserMessage(userPrompt) }
        return adders
    }

    private fun buildParams(
        messageAdders: List<(MessageCreateParams.Builder) -> Unit>,
        systemPrompt: String,
        tools: List<Tool>?
    ): MessageCreateParams {
        val cacheBreakpoint = CacheControlEphemeral.builder().build()
        val builder = MessageCreateParams.builder()
            .model(Model.of(model))
            .maxTokens(maxTokens)
            .systemOfTextBlockParams(listOf(
                TextBlockParam.builder()
                    .text(systemPrompt)
                    .cacheControl(cacheBreakpoint)
                    .build()
            ))
        messageAdders.forEach { it(builder) }
        if (tools != null) {
            tools.dropLast(1).forEach { builder.addTool(it) }
            tools.lastOrNull()?.let { lastTool ->
                builder.addTool(
                    lastTool.toBuilder()
                        .cacheControl(cacheBreakpoint)
                        .build()
                )
            }
        }
        return builder.build()
    }

    // ── Response extraction ────────────────────────────────────────────────

    private fun extractTextContent(message: Message): String {
        return message.content()
            .mapNotNull { block -> block.text().orElse(null)?.text() }
            .joinToString("")
    }

    private fun extractToolUseBlocks(message: Message): List<ToolUseBlock> {
        return message.content()
            .mapNotNull { block -> block.toolUse().orElse(null) }
    }

    private fun contentBlockToParam(block: ContentBlock): ContentBlockParam {
        val textBlock = block.text().orElse(null)
        if (textBlock != null) {
            return ContentBlockParam.ofText(
                TextBlockParam.builder().text(textBlock.text()).build()
            )
        }

        val toolUse = block.toolUse().orElse(null)
        if (toolUse != null) {
            return ContentBlockParam.ofToolUse(
                ToolUseBlockParam.builder()
                    .id(toolUse.id())
                    .name(toolUse.name())
                    .input(toolUse._input())
                    .build()
            )
        }

        // Fallback for other block types (thinking, etc.)
        return ContentBlockParam.ofText(TextBlockParam.builder().text("").build())
    }

    // ── Tool helpers ───────────────────────────────────────────────────────

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

    private suspend fun dispatchToolCall(
        call: ToolCall,
        toolIndex: Map<String, ToolProvider>
    ): ToolResult {
        val provider = toolIndex[call.toolName]
            ?: return ToolResult(
                id = call.id,
                toolName = call.toolName,
                content = "Error: No provider found for tool '${call.toolName}'",
                isError = true
            )
        return provider.callTool(call)
    }

    private fun ToolDefinition.toAnthropicTool(): Tool {
        val schemaJson = json.parseToJsonElement(inputSchema).jsonObject
        val inputSchemaBuilder = Tool.InputSchema.builder()

        val properties = schemaJson["properties"]?.jsonObject
        if (properties != null) {
            val propsBuilder = Tool.InputSchema.Properties.builder()
            for ((key, value) in properties.entries) {
                propsBuilder.putAdditionalProperty(key, JsonValue.from(jsonElementToNative(value)))
            }
            inputSchemaBuilder.properties(propsBuilder.build())
        }

        val required = schemaJson["required"]?.jsonArray
        if (required != null) {
            for (req in required) {
                inputSchemaBuilder.addRequired(req.jsonPrimitive.content)
            }
        }

        return Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchemaBuilder.build())
            .build()
    }

    // ── Response deserialization ────────────────────────────────────────────

    internal fun <R : Any> deserializeResponse(
        completionText: String,
        resultClass: KClass<R>,
        context: PromptContext
    ): R {
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
                "Failed to deserialize response into ${resultClass.simpleName}: ${e.message}" +
                    "\nRaw response: $jsonResponse",
                e,
                context
            )
        }
    }

    internal fun extractJson(text: String): String {
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

    // ── JSON conversion helpers ────────────────────────────────────────────

    private fun jsonValueToString(value: JsonValue): String {
        return value.toString()
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
}
