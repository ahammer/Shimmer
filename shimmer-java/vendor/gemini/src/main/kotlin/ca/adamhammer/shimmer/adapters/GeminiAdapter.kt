package ca.adamhammer.shimmer.adapters

import ca.adamhammer.shimmer.interfaces.ApiAdapter
import ca.adamhammer.shimmer.interfaces.ToolProvider
import ca.adamhammer.shimmer.model.AdapterResponse
import ca.adamhammer.shimmer.model.ImageResult
import ca.adamhammer.shimmer.model.MessageRole
import ca.adamhammer.shimmer.model.ModelPricing
import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.model.ShimmerDeserializationException
import ca.adamhammer.shimmer.model.ToolCall
import ca.adamhammer.shimmer.model.ToolDefinition
import ca.adamhammer.shimmer.model.ToolResult
import ca.adamhammer.shimmer.model.UsageInfo
import ca.adamhammer.shimmer.utils.toJsonSchema
import com.google.genai.Client
import com.google.genai.ResponseStream
import com.google.genai.types.Content
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.GenerateImagesConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Tool
import com.google.genai.types.Type
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
import java.util.Base64
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * [ApiAdapter] implementation for the Google Gemini API.
 *
 * Supports chat completions, multi-turn tool calling, streaming, image generation
 * (via Imagen), and structured JSON output via Gemini's `responseMimeType` +
 * `responseSchema`.
 *
 * @param model the Gemini model to use for chat completions (default: gemini-2.5-pro)
 * @param client an optional pre-configured [Client]; if null, one is created from
 *   the `GEMINI_API_KEY` or `GOOGLE_API_KEY` env var
 * @param maxToolRounds the maximum number of tool-calling round-trips before giving up
 * @param imageModel the model to use for image generation requests
 */
@Suppress("TooManyFunctions")
class GeminiAdapter(
    private val model: String = "gemini-2.5-pro",
    client: Client? = null,
    private val maxToolRounds: Int = 10,
    private val imageModel: String = "imagen-4.0-generate-001",
    private val pricing: ModelPricing = ModelPricing()
) : ApiAdapter {
    private val logger = Logger.getLogger(GeminiAdapter::class.java.name)

    private val client: Client = client ?: run {
        val apiKey = System.getenv("GEMINI_API_KEY")
            ?: System.getenv("GOOGLE_API_KEY")
            ?: error("GEMINI_API_KEY or GOOGLE_API_KEY environment variable not set.")
        Client.builder().apiKey(apiKey).build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

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

    private suspend fun <R : Any> handleRequestInternal(
        context: PromptContext,
        resultClass: KClass<R>,
        toolProviders: List<ToolProvider>
    ): AdapterResponse<R> {
        if (resultClass == ImageResult::class) {
            @Suppress("UNCHECKED_CAST")
            return AdapterResponse(handleImageRequest(context) as R)
        }

        val userPrompt = buildUserPrompt(context)
        val allToolDefs = collectToolDefs(context, toolProviders)
        val hasTools = allToolDefs.isNotEmpty() && toolProviders.isNotEmpty()
        val toolIndex = buildToolIndex(hasTools, toolProviders)
        val contents = buildContentList(context, userPrompt)
        val config = buildConfig(context, resultClass, if (hasTools) allToolDefs else emptyList())

        var totalInputTokens = 0L
        var totalOutputTokens = 0L

        for (round in 1..maxToolRounds) {
            logger.fine { "Round $round — sending request with ${contents.size} content(s)" }

            val response: GenerateContentResponse =
                client.models.generateContent(model, contents, config)

            response.usageMetadata().ifPresent { meta ->
                meta.promptTokenCount().ifPresent { totalInputTokens += it }
                meta.candidatesTokenCount().ifPresent { totalOutputTokens += it }
            }

            val candidate = response.candidates().orElse(null)?.firstOrNull()
                ?: throw ShimmerDeserializationException(
                    "No response from Gemini API", context = context
                )

            val parts = candidate.content().orElse(null)?.parts()?.orElse(null) ?: emptyList()
            val functionCalls = parts.mapNotNull { it.functionCall().orElse(null) }

            if (functionCalls.isEmpty()) {
                val text = response.text()
                    ?: throw ShimmerDeserializationException(
                        "No text in response from Gemini API", context = context
                    )
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

            // Add model response with function calls to conversation
            contents.add(candidate.content().get())

            // Execute each function call and add results
            val responseParts = mutableListOf<Part>()
            for (fc in functionCalls) {
                val name = fc.name().orElse(null) ?: continue
                val args = fc.args().orElse(null) ?: emptyMap()
                val argsJson = json.encodeToString(mapToJsonElement(args))

                val shimmerCall = ToolCall(
                    id = name,
                    toolName = name,
                    arguments = argsJson
                )
                val result = dispatchToolCall(shimmerCall, toolIndex)
                responseParts.add(
                    Part.fromFunctionResponse(name, mapOf("result" to result.content))
                )
            }

            contents.add(
                Content.builder().role("user").parts(responseParts).build()
            )
        }

        throw ShimmerDeserializationException(
            "Exceeded maximum tool-calling rounds ($maxToolRounds)", context = context
        )
    }

    private fun handleImageRequest(context: PromptContext): ImageResult {
        val promptText = extractPromptFromContext(context) ?: run {
            val contents = buildContentList(context, buildUserPrompt(context))
            contents.add(
                Content.builder().role("user").parts(
                    listOf(Part.fromText(
                        "Generate a vivid image prompt for this exact scene context. " +
                            "Return only the prompt text, no JSON, no markdown, no commentary."
                    ))
                ).build()
            )
            val config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(context.systemInstructions)))
                .candidateCount(1)
                .build()

            val promptResponse = client.models.generateContent(model, contents, config)
            promptResponse.text()?.trim()?.trim('"')?.ifBlank { null }
                ?: throw ShimmerDeserializationException(
                    "No image prompt returned from Gemini", context = context
                )
        }

        val imageConfig = GenerateImagesConfig.builder()
            .numberOfImages(1)
            .outputMimeType("image/png")
            .build()

        val imageResponse = client.models.generateImages(imageModel, promptText, imageConfig)

        val images = imageResponse.generatedImages().orElse(null)
        if (images.isNullOrEmpty()) {
            throw ShimmerDeserializationException(
                "No image data returned from Gemini Imagen", context = context
            )
        }

        val image = images[0].image().orElse(null)
            ?: throw ShimmerDeserializationException(
                "No image in response from Gemini Imagen", context = context
            )

        val imageBytes = image.imageBytes().orElse(null)
            ?: throw ShimmerDeserializationException(
                "No image bytes returned from Gemini Imagen", context = context
            )

        val base64 = Base64.getEncoder().encodeToString(imageBytes)
        return ImageResult(base64 = base64, prompt = promptText, revisedPrompt = "")
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
        val contents = buildContentList(context, userPrompt)
        val config = buildConfig(context, String::class, if (hasTools) allToolDefs else emptyList())

        for (round in 1..maxToolRounds) {
            val streamResult = processStreamChunks(contents, config) { text -> emit(text) }

            if (!streamResult.hasToolCalls) return@flow

            // Add model response with function calls
            contents.add(
                Content.builder().role("model").parts(streamResult.modelParts).build()
            )

            // Execute function calls and add results
            val responseParts = mutableListOf<Part>()
            for (fc in streamResult.functionCalls) {
                val name = fc.name
                val argsJson = json.encodeToString(mapToJsonElement(fc.args))
                val shimmerCall = ToolCall(id = name, toolName = name, arguments = argsJson)
                val result = dispatchToolCall(shimmerCall, toolIndex)
                responseParts.add(
                    Part.fromFunctionResponse(name, mapOf("result" to result.content))
                )
            }

            contents.add(
                Content.builder().role("user").parts(responseParts).build()
            )
        }
    }.flowOn(Dispatchers.IO)

    private data class AccumulatedFunctionCall(
        val name: String,
        val args: Map<String, Any?>
    )

    private data class StreamResult(
        val hasToolCalls: Boolean,
        val functionCalls: List<AccumulatedFunctionCall>,
        val modelParts: List<Part>
    )

    private fun processStreamChunks(
        contents: List<Content>,
        config: GenerateContentConfig,
        emitText: suspend (String) -> Unit
    ): StreamResult {
        val accumulatedFunctionCalls = mutableListOf<AccumulatedFunctionCall>()
        val modelParts = mutableListOf<Part>()

        val stream: ResponseStream<GenerateContentResponse> =
            client.models.generateContentStream(model, contents, config)

        stream.use { responseStream ->
            for (chunk in responseStream) {
                val text = chunk.text()
                if (!text.isNullOrEmpty()) {
                    kotlinx.coroutines.runBlocking { emitText(text) }
                }

                val candidate = chunk.candidates().orElse(null)?.firstOrNull()
                val parts = candidate?.content()?.orElse(null)?.parts()?.orElse(null)
                    ?: continue

                for (part in parts) {
                    val fc = part.functionCall().orElse(null)
                    if (fc != null) {
                        val name = fc.name().orElse(null) ?: continue
                        val args = fc.args().orElse(null) ?: emptyMap()
                        accumulatedFunctionCalls.add(AccumulatedFunctionCall(name, args))
                        modelParts.add(part)
                    }
                }
            }
        }

        return StreamResult(
            hasToolCalls = accumulatedFunctionCalls.isNotEmpty(),
            functionCalls = accumulatedFunctionCalls,
            modelParts = modelParts
        )
    }

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

    private fun buildContentList(
        context: PromptContext,
        userPrompt: String
    ): MutableList<Content> {
        val contents = mutableListOf<Content>()
        for (msg in context.conversationHistory) {
            val role = when (msg.role) {
                MessageRole.SYSTEM -> continue
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "model"
            }
            contents.add(
                Content.builder().role(role).parts(listOf(Part.fromText(msg.content))).build()
            )
        }
        contents.add(
            Content.builder().role("user").parts(listOf(Part.fromText(userPrompt))).build()
        )
        return contents
    }

    private fun <R : Any> buildConfig(
        context: PromptContext,
        resultClass: KClass<R>,
        toolDefs: List<ToolDefinition>
    ): GenerateContentConfig {
        val builder = GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(context.systemInstructions)))
            .candidateCount(1)

        if (toolDefs.isNotEmpty()) {
            val declarations = toolDefs.map { it.toGeminiFunctionDeclaration() }
            builder.tools(listOf(Tool.builder().functionDeclarations(declarations).build()))
        } else if (resultClass != String::class && resultClass != ImageResult::class) {
            val schema = resultClass.toJsonSchema()
            val geminiSchema = jsonSchemaToGeminiSchema(schema)
            builder.responseMimeType("application/json")
            builder.responseSchema(geminiSchema)
        }

        return builder.build()
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

    private fun ToolDefinition.toGeminiFunctionDeclaration(): FunctionDeclaration {
        val schemaElement = json.parseToJsonElement(inputSchema).jsonObject
        val parametersSchema = jsonSchemaToGeminiSchema(schemaElement)

        return FunctionDeclaration.builder()
            .name(name)
            .description(description)
            .parameters(parametersSchema)
            .build()
    }

    private fun jsonSchemaToGeminiSchema(schemaJson: JsonObject): Schema {
        val type = schemaJson["type"]?.jsonPrimitive?.content ?: "object"
        val builder = Schema.builder()

        when (type) {
            "object" -> {
                builder.type(Type.Known.OBJECT)
                val properties = schemaJson["properties"]?.jsonObject
                if (properties != null) {
                    val propMap = properties.entries.associate { (key, value) ->
                        key to jsonSchemaToGeminiSchema(value.jsonObject)
                    }
                    builder.properties(propMap)
                }
                val required = schemaJson["required"]?.jsonArray?.map { it.jsonPrimitive.content }
                if (required != null) {
                    builder.required(required)
                }
            }
            "string" -> {
                builder.type(Type.Known.STRING)
                val enumValues = schemaJson["enum"]?.jsonArray
                if (enumValues != null) {
                    builder.enum_(enumValues.map { it.jsonPrimitive.content })
                }
            }
            "integer" -> builder.type(Type.Known.INTEGER)
            "number" -> builder.type(Type.Known.NUMBER)
            "boolean" -> builder.type(Type.Known.BOOLEAN)
            "array" -> {
                builder.type(Type.Known.ARRAY)
                val items = schemaJson["items"]?.jsonObject
                if (items != null) {
                    builder.items(jsonSchemaToGeminiSchema(items))
                }
            }
        }

        val description = schemaJson["description"]?.jsonPrimitive?.content
        if (description != null) {
            builder.description(description)
        }

        return builder.build()
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

    private fun mapToJsonElement(map: Map<String, Any?>): JsonElement {
        return JsonObject(map.mapValues { (_, v) -> anyToJsonElement(v) })
    }

    private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(
            value.entries.associate { (k, v) -> k.toString() to anyToJsonElement(v) }
        )
        is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }
}
