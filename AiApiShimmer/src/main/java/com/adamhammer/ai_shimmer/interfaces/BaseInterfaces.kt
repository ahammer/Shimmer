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
 * Interface for handling AI speech output.
 */
interface AiSpeak {
    /**
     * Registers a speech action.
     *
     * @param key A unique identifier for the speech function.
     * @param description A brief description of the speech function.
     * @param function A callback function that processes a [String] input.
     */
    fun add(key: String, description: String, function: (String) -> Unit)
}

/**
 * Interface for handling AI listening or input.
 */
interface AiListen {
    /**
     * Registers an input listener.
     *
     * @param key A unique identifier for the listener.
     * @param description A brief description of what the listener does.
     * @param input The input string that will be processed.
     */
    fun add(key: String, description: String, input: String)
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
interface BaseInterfaces : Decider, AiSpeak, AiListen, DecisionRunner
