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

// Data classes for the final idea and the ideation result.
@Serializable
@Schema(title = "Idea", description = "The final idea produced by the agent.")
data class Idea(
    @field:Schema(title = "Content", description = "The final idea content.")
    val content: String
)

@Serializable
@Schema(
    title = "IdeationResult",
    description = "The ideation report containing the final idea and the breakdown of each step."
)
data class IdeationResult(
    @field:Schema(title = "Idea", description = "The final idea produced by the agent.")
    val idea: Idea,
)

// --- BasicAIApi Interface ---
// Every method is annotated with @Memorize. Only 'initiate' accepts an input parameter.
interface BasicAIApi {
    @Operation(summary = "Initiate", description = "Generate a variety of ideas around the given concept.")
    @ApiResponse(
        description = "Result of ideation.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "initiatal ideas")
    fun initiate(
        @Parameter(description = "Phrase to initiate the ideation process.")
        input: String
    ): Future<String>

    @Operation(summary = "Deconstruct", description = "Given an initial idea, deconstruct it by:\n" +
            "\n" +
            "Identifying and questioning its underlying assumptions.\n" +
            "Breaking down its structure into fundamental components.\n" +
            "Uncovering any hidden layers, contradictions, or implicit meanings.\n" +
            "Suggesting potential refinements or improvements based on your analysis.")
    @ApiResponse(
        description = "Result of deconstruction.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "deconstruct")
    fun deconstruct(): Future<String>

    @Operation(summary = "Expand", description = "Given the deconstructed components of an initial idea, expand it by:\n" +
            "\n" +
            "Synthesizing Elements: Reassemble the identified components into a cohesive, enriched concept.\n" +
            "Elaborating on Details: Develop each component further by exploring additional nuances and implications.\n" +
            "Connecting Dots: Identify and articulate new relationships and synergies between components.\n" +
            "Exploring Contexts: Consider broader applications, potential impacts, and alternative perspectives.\n" +
            "Suggesting Enhancements: Propose creative modifications or extensions that could improve or evolve the idea.")
    @ApiResponse(
        description = "Result of expansion.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "expand")
    fun expand(): Future<String>

    @Operation(summary = "Unify", description = "Given the expanded components of an idea, unify them by:\n" +
            "\n" +
            "Integrating Elements: Combine all insights, details, and nuances into one cohesive concept.\n" +
            "Resolving Inconsistencies: Identify and reconcile any conflicting ideas or gaps from previous analyses.\n" +
            "Establishing Coherence: Ensure the final idea flows logically and maintains a balanced structure.\n" +
            "Synthesizing Perspectives: Merge the strengths of each component into a unified whole that is comprehensive and ready for further application or evaluation.")
    @ApiResponse(
        description = "Result of unification.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "unified idea: ")
    fun unify(): Future<String>

    @Operation(summary = "Apply", description = "Given the unified idea, apply it by:\n" +
            "\n" +
            "Contextualizing the Concept: Identify relevant real-world scenarios or specific problems where this unified idea can be implemented effectively.\n" +
            "Developing an Action Plan: Outline clear, actionable steps for integrating the idea into practice or testing it within a chosen context.\n" +
            "Anticipating Challenges: Highlight potential obstacles or risks, and propose strategies for mitigation.\n" +
            "Defining Success Metrics: Establish criteria to evaluate the idea's effectiveness, including desired outcomes and measurable indicators.\n" +
            "Iterative Refinement: Suggest a process for monitoring feedback and results to further refine and improve the applied concept")
    @ApiResponse(
        description = "Result of application.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "apply")
    fun apply(): Future<String>

    @Operation(summary = "Preserve", description = "Given the applied concept, preserve it by:\n" +
            "\n" +
            "Documenting the Journey: Record the evolution from the initial idea through deconstruction, expansion, unification, and application, including key decisions and insights.\n" +
            "Archiving Versions: Store all iterations, revisions, and feedback in a structured archive for future reference.\n" +
            "Ensuring Reproducibility: Create clear documentation and version control so the process can be reviewed or replicated.\n" +
            "Establishing a Feedback Loop: Set up mechanisms for ongoing evaluation and periodic review to capture new insights or necessary adjustments.\n" +
            "Maintaining Accessibility: Organize the preserved data in a way that it remains accessible and understandable for future use or further development.")
    @ApiResponse(
        description = "Result of preservation.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "preserve")
    fun preserve(): Future<String>

    @Operation(summary = "Transform", description = "Given the preserved concept, transform it by:\n" +
            "\n" +
            "Reinterpretation: Reframe the idea from its archived form into new perspectives or alternative formats.\n" +
            "Adaptation: Modify or adjust key components to align with new objectives, emerging trends, or changed contexts.\n" +
            "Innovation: Infuse creative enhancements that elevate the idea, integrating fresh insights and novel approaches.\n" +
            "Evaluation: Assess the transformed concept for viability, coherence, and its potential impact in the new context.\n" +
            "Iteration: Refine the transformation through iterative feedback, ensuring it remains robust and adaptable for future applications.")
    @ApiResponse(
        description = "Final transformed result.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "transform")
    fun transform(): Future<String>

    @Operation(summary = "Generate Markdown Report", description = "Generate a report/document in well formatted markdown with emoji's and personality. Unify the ideas collected as an engineering design document. Be robust and detailed.")
    @ApiResponse(
        description = "The Markdown Report",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    fun report(): Future<String>
}

// The BasicAgent class that exposes only the ideate method.
// It accepts a BasicAIApi instance (built with AiApiBuilder) in its constructor.
class BasicAgent(private val api: BasicAIApi) {

    /**
     * Runs the ideation process using the following steps:
     * 1. Initiation
     * 2. Deconstruction
     * 3. Expansion
     * 4. Unification
     * 5. Application
     * 6. Preservation
     * 7. Transformation
     *
     * Each step calls the corresponding method on the provided BasicAIApi.
     * The results are stored in memory and the final transformed result becomes the idea.
     */
    fun ideate(text: String): IdeationResult {

        api.initiate(text).get()
        api.deconstruct().get()
        api.expand().get()
        api.unify().get()
        api.apply().get()
        api.preserve().get()
        api.transform().get()
        val finalIdea = Idea(content = api.report().get())
        return IdeationResult(idea = finalIdea)
    }
}

// Test class for BasicAgent.
class BasicAgentTest {

    @Test
    fun testIdeate() {
        // Build the API using the builder with a com.adamhammer.ai_shimmer.adapters.StubAdapter.
        // (Assumes AiApiBuilder and com.adamhammer.ai_shimmer.adapters.StubAdapter are available in your project.)
        val api = ShimmerBuilder(BasicAIApi::class)
            .setAdapter(OpenAiAdapter())
            .build()

        val agent = BasicAgent(api)
        val input = "I have an AI library, similar to retrofit with annotations. I want to add some specific interfaces for sinks/faucets, i.e. child/parent relationships to send/receive information from. The current API/SDK uses the java invocation handler. I'm expecting the AI Api's to also take these interfaces, and the invocation handler will manage them with delegates. Sinks and Faucets should be paired and one directional. Generate a report on potential kotlin interfaces to manage this API, delegates etc."
        val result = agent.ideate(input)

        assertNotNull(result.idea, "Final idea should not be null")
    }
}
