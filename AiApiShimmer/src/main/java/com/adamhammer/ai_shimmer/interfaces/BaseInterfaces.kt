package com.adamhammer.ai_shimmer.interfaces

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable
import java.util.concurrent.Future

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

/**
 * Interface for components that determine the next action for the AI.
 */
interface Decider {
    /**
     * Decides the next action asynchronously.
     *
     * @return A [Future] that will eventually hold the [AiDecision].
     */
    fun decideNextAction(): Future<AiDecision>
}


/**
 * Interface for executing actions determined by the AI.
 */
interface DecisionRunner {
    /**
     * Executes the provided AI decision.
     *
     * @param decision The [AiDecision] to be executed.
     */
    fun runAction(decision: AiDecision)
}

/**
 * Combines all the core AI interfaces into one.
 */
interface BaseInterfaces : Decider, DecisionRunner