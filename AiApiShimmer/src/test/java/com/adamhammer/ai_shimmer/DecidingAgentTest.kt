package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.interfaces.BaseInterfaces
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import com.adamhammer.ai_shimmer.interfaces.Memorize
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

interface AutonomousAIApi : BaseInterfaces{

    @Operation(
        description = "Gather environmental data and update internal state with current context."
    )
    @ApiResponse(
        description = "Result of environmental perception.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "perceive")
    fun perceive(): Future<String>

    @Operation(
        summary = "Analyze",
        description = "Process the gathered data to extract insights and identify potential actions."
    )
    @ApiResponse(
        description = "Result of the analysis phase.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "analyze")
    fun analyze(): Future<String>

    @Operation(
        summary = "Plan",
        description = "Devise a strategy based on current insights and previous memory to decide the next steps."
    )
    @ApiResponse(
        description = "Result of the planning process.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "plan")
    fun plan(): Future<String>

    @Operation(
        summary = "Act",
        description = "Execute the planned action within the environment."
    )
    @ApiResponse(
        description = "Result of action execution.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "act")
    fun act(): Future<String>

    @Operation(
        summary = "Reflect",
        description = "Evaluate the outcome of the action and update internal memory for future iterations."
    )
    @ApiResponse(
        description = "Result of the reflection process.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "reflect")
    fun reflect(): Future<String>
}




// The BasicAgent class that exposes only the ideate method.
// It accepts a BasicAIApi instance (built with AiApiBuilder) in its constructor.
class AutonomousAgent(private val api: AutonomousAIApi) {
    fun step() : Future<AiDecision> {
        return api.decideNextAction()
        // next.execute(this or something)
    }
}

// Test class for BasicAgent.
class DecidingAgentTest {

    @Test
    fun testIdeate() {

        val api = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build()

        val agent = AutonomousAgent(api)
        val r1 = agent.step().get()
        val r2 = agent.step().get()
        print (r1);
        print (r2);

    }
}
