package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.adapters.BaseApiAdapter
import com.adamhammer.ai_shimmer.utils.toJsonInvocationString
import com.adamhammer.ai_shimmer.utils.toJsonStructureString
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
import java.lang.reflect.Method


public class OpenAiAdapter : BaseApiAdapter() {
    // Read API key from the environment variable.
    private val apiKey: String = System.getenv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set.")

    // Build the client using the official OpenAI Java API.
    private val client: OpenAIClient = OpenAIOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    // Configure JSON parser.
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @OptIn(InternalSerializationApi::class)
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>, memory: Map<String, String>): R {
        // Build prompt parts from method metadata, parameter values, and the expected JSON schema.
        val resultSchema = resultClass.toJsonStructureString()
        val methodDeclaration = method.toJsonInvocationString(args)

        // 1) Define a system-level preamble to guide the model
        val systemPreamble = """
[System Role Instruction Start]

You are a specialized AI Assistant that handles request/response method calls and returns results in JSON or plain text. 
Your responsibilities:
1. **Respect the method call structure**: A JSON block will be provided describing the method, its parameters, and any stored memory.
2. **Check the 'Output Schema'**: This indicates the JSON format you must return when asked for JSON output. 
   - Do NOT add any fields not mentioned in the schema.
   - If asked for a string, return plain text without extra JSON structure.
3. **Incorporate Memory if Provided**: If 'memory' is present, use it to inform your answer. 
4. **No Additional Commentary**: Do not include apology statements, disclaimers, or meta-commentary.
5. **Provide Clear and Concise Responses**: Only return the specified structure or text, without superfluous information.
6. **When Memorize Key is present this will be stored for later**: Minimize token count, Maximize content effectiveness


In summary, follow these steps whenever you see a request:
- Read the method call (JSON).
- Use the method description, parameters, and memory to form your response.
- Output the response in the requested format (plain text or JSON matching the schema).

[System Role Instruction End]""".trimIndent()

    // 2) Combine the system preamble with your existing prompt format
        val prompt = """
$systemPreamble

# METHOD
$methodDeclaration

# RESULT
$resultSchema""".trimIndent()

        // Create a ChatCompletion request using the official API.
        val params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(ChatModel.GPT_4O_MINI)
            .build()

        println("╔══════════════════╗")
        println("║   OPENAI TX      ║")
        println("╚══════════════════╝")
        println()
        println(prompt)
        println()
        val chatCompletion: ChatCompletion = client.chat().completions().create(params)

        val completionText = chatCompletion.choices().firstOrNull()?.message()?.content()?.get()?.trim()
            ?: throw RuntimeException("No response from OpenAI API")

        println("╔══════════════════╗")
        println("║   OPENAI RX      ║")
        println("╚══════════════════╝")
        println()
        println(completionText)
        println()

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
