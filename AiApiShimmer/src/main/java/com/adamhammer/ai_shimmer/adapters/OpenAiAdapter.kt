package com.adamhammer.ai_shimmer.adapters

import BaseApiAdapter
import com.adamhammer.ai_shimmer.utils.MethodUtils
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatCompletion
import com.openai.models.ChatCompletionCreateParams
import com.openai.models.ChatModel
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import java.lang.reflect.Method

public class OpenAiAdapter(baseType: KClass<Any>) : BaseApiAdapter(baseType = baseType) {
    // Read API key from the environment variable.
    private val apiKey: String = System.getenv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set.")

    // Build the client using the official OpenAI Java API.
    private val client: OpenAIClient = OpenAIOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    // Configure JSON parser.
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(InternalSerializationApi::class)
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>): R {
        // Build prompt parts from method metadata, parameter values, and the expected JSON schema.
        val schemaDescription = method.getDeclaredAnnotation(ApiResponse::class.java)?.description ?: ""
        val resultSchema = MethodUtils.buildResultSchema(resultClass)
        val inputs = MethodUtils.generateSerializableRequest(method, args, getMemoryMap())
        val jsonContent = json.encodeToString(inputs);

        val prompt = """
            This is a system of request/response. A Method call in JSON will be provided. Respond directly as a string or in the JSON format requested.                       
            # METHOD
            $jsonContent
            # RESULT            
            $schemaDescription
            Output Schema: $resultSchema""".trimIndent()

        // Create a ChatCompletion request using the official API.
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(ChatModel.GPT_4O)
            .build()

        val chatCompletion: ChatCompletion = client.chat().completions().create(params)

        val completionText = chatCompletion.choices().firstOrNull()?.message()?.content()?.get()?.trim()
            ?: throw RuntimeException("No response from OpenAI API")

        // If the expected result type is String, return the raw response directly.
        if (resultClass == String::class) {
            @Suppress("UNCHECKED_CAST")
            return completionText as R
        }

        // Otherwise, extract the JSON block (in case extra text is included).
        val jsonResponse = extractJson(completionText)

        // Deserialize the JSON into the expected result type.
        try {
            @Suppress("UNCHECKED_CAST")
            val serializer = resultClass.serializer() as KSerializer<R>
            return json.decodeFromString(serializer, jsonResponse)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deserialize JSON response: ${e.message}", e)
        }
    }


    // Helper function to extract JSON from a response that might contain extra text.
    private fun extractJson(text: String): String {
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        return if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            text.substring(startIndex, endIndex + 1)
        } else {
            text
        }
    }
}
