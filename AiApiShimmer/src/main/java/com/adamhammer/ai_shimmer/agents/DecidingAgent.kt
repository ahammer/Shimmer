package com.adamhammer.ai_shimmer.agents

import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.utils.MethodUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter
import kotlinx.serialization.Serializable
import java.util.concurrent.Future
import kotlin.reflect.KClass

/**
 * Represents an AI decision containing the method to invoke and its arguments.
 *
 * @property method The name of the method to be executed.
 * @property args A collection of key-value maps representing the arguments.
 */
@Serializable
@Schema(title = "AI Decision", description = "Encapsulates the decision logic for the AI.")
data class AiDecision(
    @get:Schema(title = "Method", description = "The method to invoke.")
    val method: String,
    @get:Schema(title = "Arguments", description = "A collection of key-value pairs representing the arguments for the method.")
    val args: Map<String, String>
)

fun DecidingAgentAPI.decide(obj: Class<Any>) : Future<AiDecision>{
    return decideNextAction(MethodUtils.parseObjectForDecisionSchema(obj))
}

// A Special Agent, that backs the Decider.
// I.e. it can look at a class and offer choices on what to do next.
interface DecidingAgentAPI {
    @Operation(
        summary = "Decide Next Action",
        description = "Introspects the provided object to build a decision schema and then determines the next action based on its capabilities."
    )
    @ApiResponse(
        description = "A decision about the next action, based on the options and available input.",
        content = [Content(schema = Schema(implementation = AiDecision::class))]
    )
    fun decideNextAction(
        @Parameter(description = "The current state and available options")
        currentObject: String
    ): Future<AiDecision>
}
