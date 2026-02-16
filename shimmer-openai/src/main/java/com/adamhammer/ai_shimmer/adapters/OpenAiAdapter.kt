package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.model.PromptContext
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
    private val model: ChatModel = ChatModel.GPT_4O_MINI
) : ApiAdapter {
    private val logger = Logger.getLogger(OpenAiAdapter::class.java.name)

    private val apiKey: String = System.getenv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set.")

    private val client: OpenAIClient = OpenAIOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @OptIn(InternalSerializationApi::class)
    override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        val memorySection = if (context.memory.isNotEmpty()) {
            val memoryJson = json.encodeToString(
                kotlinx.serialization.json.JsonObject(
                    context.memory.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value) }
                )
            )
            "\n\n# MEMORY\n$memoryJson"
        } else ""

        val prompt = """
${context.systemInstructions}

# METHOD
${context.methodInvocation}$memorySection""".trimIndent()

        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(model)
            .build()

        logger.fine { "Request:\n$prompt" }

        val chatCompletion: ChatCompletion = client.chat().completions().create(params)

        val completionText = chatCompletion.choices().firstOrNull()?.message()?.content()?.get()?.trim()
            ?: throw RuntimeException("No response from OpenAI API")

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
            throw RuntimeException("Failed to deserialize JSON response: ${e.message}", e)
        }
    }

    private fun extractJson(text: String): String {
        val objectStartIndex = text.indexOf('{')
        val objectEndIndex = text.lastIndexOf('}')
        if (objectStartIndex != -1 && objectEndIndex != -1 && objectStartIndex < objectEndIndex) {
            return text.substring(objectStartIndex, objectEndIndex + 1)
        }

        val arrayStartIndex = text.indexOf('[')
        val arrayEndIndex = text.lastIndexOf(']')
        if (arrayStartIndex != -1 && arrayEndIndex != -1 && arrayStartIndex < arrayEndIndex) {
            return text.substring(arrayStartIndex, arrayEndIndex + 1)
        }

        return text
    }
}
