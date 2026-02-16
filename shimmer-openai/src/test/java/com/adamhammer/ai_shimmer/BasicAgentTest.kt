package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.annotations.*
import kotlinx.serialization.Serializable
import java.util.concurrent.Future
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@Serializable
@AiSchema(title = "Idea", description = "The final idea produced by the agent.")
data class Idea(
    @field:AiSchema(title = "Content", description = "The content of the final idea.")
    val content: String
)

@Serializable
@AiSchema(
    title = "IdeationResult",
    description = "The complete ideation report including the final idea."
)
data class IdeationResult(
    @field:AiSchema(title = "Idea", description = "The final idea produced by the agent.")
    val idea: Idea
)

interface SimpleAIApi {
    @AiOperation(
        summary = "Initiate",
        description = "Generates an initial set of ideas based on the provided concept."
    )
    @AiResponse(
        description = "Initial ideas as a plain text string.",
        responseClass = String::class
    )
    @Memorize(label = "initial ideas")
    fun initiate(
        @AiParameter(description = "A phrase or concept to spark the ideation process.")
        input: String
    ): Future<String>

    @AiOperation(
        summary = "Expand",
        description = "Expands and elaborates on the initial ideas by adding more detail."
    )
    @AiResponse(
        description = "Expanded ideas, generate a lot (like 20+, with diversity) as plain text.",
        responseClass = String::class
    )
    @Memorize(label = "expanded ideas")
    fun expand(): Future<String>

    @AiOperation(
        summary = "Generate Markdown Report",
        description = "Generates a detailed markdown on the user's idea. You can go verbose, detailed and well formatted."
    )
    @AiResponse(
        description = "A markdown-formatted report.",
        responseClass = String::class
    )
    fun report(): Future<String>
}

class SimpleAgent(private val api: SimpleAIApi) {
    fun ideate(input: String): IdeationResult {
        api.initiate(input).get()
        api.expand().get()
        val reportContent = api.report().get()
        val finalIdea = Idea(content = reportContent)
        return IdeationResult(idea = finalIdea)
    }
}

class BasicAgentTest {

    @Test
    fun testIdeate() {
        val api = ShimmerBuilder(SimpleAIApi::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build().api

        val agent = SimpleAgent(api)
        val input = "I have an AI library similar to Retrofit with annotations. " +
                "I need to define interfaces for sinks and faucets (child/parent relationships) " +
                "to handle information exchange using Java's invocation handler. " +
                "Generate a report on potential Kotlin interfaces to manage this API and delegates."
        val result = agent.ideate(input)

        assertNotNull(result.idea, "Final idea should not be null")
        assertTrue(result.idea.content.isNotBlank(), "Final idea content should not be blank")

        println("Final idea content: ${result.idea.content}")
    }
}
