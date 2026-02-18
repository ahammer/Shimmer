package com.adamhammer.shimmer.agents

import com.adamhammer.shimmer.ShimmerInstance
import com.adamhammer.shimmer.annotations.AiOperation
import com.adamhammer.shimmer.annotations.AiParameter
import com.adamhammer.shimmer.annotations.AiResponse
import com.adamhammer.shimmer.annotations.AiSchema
import com.adamhammer.shimmer.annotations.Memorize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Future

@AiSchema(description = "Autonomous agent that reflects on user input and delivers a result")
interface AutonomousAIApi {

    @AiOperation(description = "Accept input from the user and try to understand it")
    @AiResponse(description = "Rephrase and clarify the user's input.", responseClass = String::class)
    @Memorize(label = "Users Intent")
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
    fun act(): Future<String>
}

/**
 * Generic autonomous agent that uses a [DecidingAgentAPI] to choose which
 * method to invoke on an arbitrary Shimmer API. The pipeline is defined by
 * the methods on [T], not hardcoded — [AutonomousAIApi] is one example.
 */
class AutonomousAgent<T : Any>(
    private val apiInstance: ShimmerInstance<T>,
    private val decider: DecidingAgentAPI
) {
    private val dispatcher = AgentDispatcher(apiInstance)

    fun step(): String {
        val decision = decider.decide(apiInstance).get()
        val result = dispatcher.dispatch(decision)
        return result?.toString() ?: ""
    }

    suspend fun stepSuspend(): String = withContext(Dispatchers.IO) {
        step()
    }
}
