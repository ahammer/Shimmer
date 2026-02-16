package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ShimmerDeserializationException
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatCompletion
import com.openai.models.ChatCompletionCreateParams
import com.openai.models.ChatModel
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import java.util.logging.Logger

class OpenAiAdapter(
    private val model: ChatModel = ChatModel.GPT_4O_MINI,
    client: OpenAIClient? = null
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
        val memorySection = if (context.memory.isNotEmpty()) {
            val memoryJson = json.encodeToString(
                kotlinx.serialization.json.JsonObject(
                    context.memory.mapValues { entry ->
                        try {
                            json.parseToJsonElement(entry.value)
                        } catch (_: Exception) {
                            kotlinx.serialization.json.JsonPrimitive(entry.value)
                        }
                    }
                )
            )
            "\n\n# MEMORY\n$memoryJson"
        } else ""

        val userPrompt = """
# METHOD
${context.methodInvocation}$memorySection""".trimIndent()

        val params = ChatCompletionCreateParams.builder()
            .addSystemMessage(context.systemInstructions)
            .addUserMessage(userPrompt)
            .model(model)
            .build()

        logger.fine { "System:\n${context.systemInstructions}\nUser:\n$userPrompt" }

        val chatCompletion: ChatCompletion = client.chat().completions().create(params)

        val completionText = chatCompletion.choices().firstOrNull()?.message()?.content()?.get()?.trim()
            ?: throw ShimmerDeserializationException("No response from OpenAI API")

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
