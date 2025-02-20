package com.adamhammer.ai_shimmer.interfaces

import kotlinx.serialization.Serializable
import java.util.concurrent.Future

/**
 * Represents an AI decision containing the method to invoke and its arguments.
 *
 * @property method The name of the method to be executed.
 * @property args A collection of key-value maps representing the arguments.
 */
@Serializable
data class AiDecision(
    val method: String,
    val args: Collection<Map<String, String>>
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
