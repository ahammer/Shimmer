package com.adamhammer.ai_shimmer.agents

import com.adamhammer.ai_shimmer.ShimmerInstance
import com.adamhammer.ai_shimmer.annotations.*
import java.util.concurrent.Future

@AiSchema(description = "Autonomous agent that reflects on user input and delivers a result")
interface AutonomousAIApi {

    @AiOperation(description = "Accept input from the user and try to understand it")
    @AiResponse(description = "Rephrase and clarify the user's input.", responseClass = String::class)
    @Memorize(label = "Users Intent")
    @Subscribe(channel = "User Input")
    fun understand(
        @AiParameter(description = "The user's input we are trying to understand.")
        data: String
    ): Future<String>

    @AiOperation(description = "Process the gathered data to extract insights and identify potential actions.")
    @AiResponse(description = "Result of the analysis phase.", responseClass = String::class)
    @Memorize(label = "analyze")
    fun analyze(): Future<String>

    @AiOperation(description = "Devise a strategy based on current insights and previous memory to decide the next steps.")
    @AiResponse(description = "Result of the planning process.", responseClass = String::class)
    @Memorize(label = "plan")
    fun plan(): Future<String>

    @AiOperation(description = "Reflect on the current state and provide the update.")
    @AiResponse(description = "Result of the reflection process.", responseClass = String::class)
    @Memorize(label = "reflect")
    fun reflect(): Future<String>

    @AiOperation(description = "Deliver the result")
    @AiResponse(description = "The final result/communication", responseClass = String::class)
    @Memorize(label = "act")
    @Publish("output")
    fun act(): Future<String>
}

class AutonomousAgent(private val api: AutonomousAIApi, private val decider: DecidingAgentAPI) {
    fun step(): String {
        val decision = decider.decide(ShimmerInstance(api, mutableMapOf(), AutonomousAIApi::class)).get()

        return when (decision.method) {
            "understand" -> {
                val data = decision.args["data"]
                    ?: throw IllegalArgumentException("Missing 'data' argument for understand method")
                api.understand(data).get()
            }
            "analyze" -> api.analyze().get()
            "plan" -> api.plan().get()
            "reflect" -> api.reflect().get()
            "act" -> api.act().get()
            else -> throw IllegalArgumentException("Unknown method: ${decision.method}")
        }
    }
}
