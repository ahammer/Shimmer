package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.OpenAiAdapter
import com.adamhammer.ai_shimmer.agents.DecidingAgentAPI
import com.adamhammer.ai_shimmer.agents.decide
import com.adamhammer.ai_shimmer.annotations.*
import com.adamhammer.ai_shimmer.interfaces.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

@AiSchema(description = "Autonomous that reflects on the user input and delivers a result when confidentially")
interface AutonomousAIApi {

    @AiOperation(
        description = "Accept input from the user and try and understand it"
    )
    @AiResponse(
        description = "Rephrase and clarify the users input.",
        responseClass = String::class
    )
    @Memorize(label = "Users Intent")
    @Subscribe(channel = "User Input")
    fun understand(
        @AiParameter(description = "The users input we are trying to understand.")
        data: String
    ): Future<String>

    @AiOperation(
        description = "Process the gathered data to extract insights and identify potential actions."
    )
    @AiResponse(
        description = "Result of the analysis phase.",
        responseClass = String::class
    )
    @Memorize(label = "analyze")
    fun analyze(): Future<String>

    @AiOperation(
        description = "Devise a strategy based on current insights and previous memory to decide the next steps."
    )
    @AiResponse(
        description = "Result of the planning process.",
        responseClass = String::class
    )
    @Memorize(label = "plan")
    fun plan(): Future<String>

    @AiOperation(
        description = "Reflect on the current state and provide the update."
    )
    @AiResponse(
        description = "Result of the reflection process.",
        responseClass = String::class
    )
    @Memorize(label = "reflect")
    fun reflect(): Future<String>

    @AiOperation(
        description = "Deliver the result"
    )
    @AiResponse(
        description = "The final result/communication",
        responseClass = String::class
    )
    @Memorize(label = "act")
    @Publish("output")
    fun act(): Future<String>
}

// The AutonomousAgent class that exposes a step method.
// It accepts an AutonomousAIApi instance (built with AiApiBuilder) and a DecidingAgentAPI in its constructor.
class AutonomousAgent(private val api: AutonomousAIApi, private val decider: DecidingAgentAPI) {
    fun step() {
        // next.execute(this or something)
    }
}

// Test class for the agent and decider integration.
class DecidingAgentTest {

    @Test
    fun testIdeate() {
        val agentAdapter = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build()

        val deciderAdapter = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterClass(OpenAiAdapter::class)
            .build()

        val agent_api = agentAdapter.api
        val deciding_api = deciderAdapter.api

        val agent = AutonomousAgent(agent_api, deciding_api)
        val result = deciding_api.decide(agentAdapter).get()
        println(result)
    }
}
