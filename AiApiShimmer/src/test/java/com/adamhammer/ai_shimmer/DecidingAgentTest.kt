package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.interfaces.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

@Schema(description = "Autonomous that reflects on the user input and delivers a result when confidentially")
interface AutonomousAIApi : BaseInterfaces {

    @Operation(
        description = "Accept input from the user and try and understand it"
    )
    @ApiResponse(
        description = "Rephrase and clarify the users input.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "Users Intent")
    @Subscribe(channel = "User Input")
    fun understand(
        @Parameter(description = "The users input we are trying to understand.")
        data: String): Future<String>

    @Operation(
        description = "Process the gathered data to extract insights and identify potential actions."
    )
    @ApiResponse(
        description = "Result of the analysis phase.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "analyze")
    fun analyze(): Future<String>

    @Operation(
        description = "Devise a strategy based on current insights and previous memory to decide the next steps."
    )
    @ApiResponse(
        description = "Result of the planning process.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "plan")
    fun plan(): Future<String>

    @Operation(
        description = "Reflect on the current state and provide the update."
    )
    @ApiResponse(
        description = "Result of the reflection process.",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "reflect")
    fun reflect(): Future<String>

    @Operation(
        description = "Deliver the result"
    )
    @ApiResponse(
        description = "The final result/communication",
        content = [Content(schema = Schema(implementation = String::class))]
    )
    @Memorize(label = "act")
    @Publish("output")
    fun act(): Future<String>
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
        val perception = api.understand("I want to know about the meaning of life.").get()
        val nextAction = api.decideNextAction();
        val na = nextAction.get()


        print (perception);


    }
}
