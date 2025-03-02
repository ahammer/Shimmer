package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.interfaces.Memorize
import kotlinx.serialization.Serializable
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import java.util.concurrent.Future
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// =============================================================================
// Data classes representing the final idea and the complete ideation result.
// =============================================================================

@Serializable
@Schema(title = "Idea", description = "The final idea produced by the agent.")
data class Idea(
    @field:Schema(title = "Content", description = "The content of the final idea.")
    val content: String
)

@Serializable
@Schema(
    title = "IdeationResult",
    description = "The complete ideation report including the final idea."
)
data class IdeationResult(
    @field:Schema(title = "Idea", description = "The final idea produced by the agent.")
    val idea: Idea
)

// =============================================================================
// Simplified API interface with three LLM-driven methods.
// =============================================================================

interface SimpleAIApi {
    @Operation(
        summary = "Initiate",
        description = "Generates an initial set of ideas based on the provided concept."
    )
    @ApiResponse(
        description = "Initial ideas as a plain text string.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "initial ideas")
    fun initiate(
        @Parameter(description = "A phrase or concept to spark the ideation process.")
        input: String
    ): Future<String>

    @Operation(
        summary = "Expand",
        description = "Expands and elaborates on the initial ideas by adding more detail."
    )
    @ApiResponse(
        description = "Expanded ideas, Generate a lot (like 20+, with diversity) as plain text.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "expanded ideas")
    fun expand(): Future<String>

    @Operation(
        summary = "Generate Markdown Report",
        description = "Generates a detailed markdown on the users idea. You can go verbose, detailed and well formatted"
    )
    @ApiResponse(
        description = "A markdown-formatted report.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    fun report(): Future<String>
}

// =============================================================================
// The SimpleAgent class: chains the LLM calls in a fixed, deterministic order.
// =============================================================================

class SimpleAgent(private val api: SimpleAIApi) {

    /**
     * Runs the ideation process using three fixed steps:
     *
     * 1. Initiation: Generate a set of initial ideas from the input concept.
     * 2. Expansion: Elaborate on the initial ideas with further details.
     * 3. Report Generation: Create a markdown report from the expanded ideas.
     *
     * The final report is encapsulated as an [Idea] and returned inside an [IdeationResult].
     */
    fun ideate(input: String): IdeationResult {
        // Step 1: Generate initial ideas from the provided input.
        api.initiate(input).get()

        // Step 2: Expand on the generated ideas.
        api.expand().get()

        // Step 3: Generate the final markdown report.
        val reportContent = api.report().get()
        val finalIdea = Idea(content = reportContent)
        return IdeationResult(idea = finalIdea)
    }
}

// =============================================================================
// Test class for the SimpleAgent using the simplified API.
// =============================================================================

class SimpleAgentTest {

    @Test
    fun testIdeate() {
        // Build the API using ShimmerBuilder with the OpenAiAdapter.
        // (Assumes ShimmerBuilder and OpenAiAdapter are available in your project.)
        val api = ShimmerBuilder(SimpleAIApi::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build()

        val agent = SimpleAgent(api)
        val input = "I have an AI library similar to Retrofit with annotations. " +
                "I need to define interfaces for sinks and faucets (child/parent relationships) " +
                "to handle information exchange using Java's invocation handler. " +
                "Generate a report on potential Kotlin interfaces to manage this API and delegates."
        val result = agent.ideate(input)

        assertNotNull(result.idea, "Final idea should not be null")
    }
}
