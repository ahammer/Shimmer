package com.adamhammer.shimmer.agents

import com.adamhammer.shimmer.ShimmerInstance
import com.adamhammer.shimmer.utils.toJsonClassMetadataString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.concurrent.Future

import com.adamhammer.shimmer.annotations.AiSchema
import com.adamhammer.shimmer.annotations.AiOperation
import com.adamhammer.shimmer.annotations.AiResponse
import com.adamhammer.shimmer.annotations.AiParameter
import com.adamhammer.shimmer.annotations.Terminal

/**
 * A single named argument for an [AiDecision].
 *
 * @param name the parameter name (must match the target method's parameter name)
 * @param value the parameter value as a string
 */
@Serializable
@AiSchema(title = "AI Argument", description = "A single named argument")
data class AiArg(
    @field:AiSchema(description = "The name of the argument")
    val name: String,
    @field:AiSchema(description = "The value of the argument")
    val value: String
)

/**
 * Represents the AI's choice of which method to call next, along with its arguments.
 *
 * @param method the name of the method to invoke on the target API
 * @param args the named arguments to pass to the method
 */
@Serializable
@AiSchema(title = "AI Decision", description = "Look at the current state and options and decide what to do next")
data class AiDecision(
    @field:AiSchema(description = "The method to call with the arguments")
    val method: String,
    @field:AiSchema(description = "The arguments to pass to this call.")
    val args: List<AiArg>
) {
    constructor(method: String, argsMap: Map<String, String>) : this(
        method,
        argsMap.map { (k, v) -> AiArg(k, v) }
    )

    fun argsMap(): Map<String, String> = args.associate { it.name to it.value }
}

/**
 * Extension that asks the [DecidingAgentAPI] to choose the next action for a given [ShimmerInstance].
 *
 * @param shimmerInstance the target API instance whose methods are available for selection
 * @param excludedMethods method names to exclude from the decision (e.g., already-called methods)
 */
fun <T : Any> DecidingAgentAPI.decide(
    shimmerInstance: ShimmerInstance<T>,
    excludedMethods: Set<String> = emptySet()
): Future<AiDecision> {
    val schema = shimmerInstance.klass.toJsonClassMetadataString(excludedMethods)
    return decideNextAction(schema)
}

/** Suspend-friendly version of [decide]. */
suspend fun <T : Any> DecidingAgentAPI.decideSuspend(shimmerInstance: ShimmerInstance<T>): AiDecision =
    withContext(Dispatchers.IO) { decide(shimmerInstance).get() }

/**
 * Interface for an AI that selects which method to call on a target API.
 *
 * Used by [AutonomousAgent] to drive multi-step agent loops: at each step the
 * deciding agent inspects the available methods (via a JSON schema) and returns
 * an [AiDecision] identifying which method to invoke and with what arguments.
 */
interface DecidingAgentAPI {
    @AiOperation(
        summary = "Decide Next Action",
        description = "Examine the available methods listed in the provided schema and choose " +
            "which one to call next, along with its arguments. " +
            "IMPORTANT: The 'method' field in your response MUST be one of the method names " +
            "from the schema in 'currentObject'."
    )
    @AiResponse(
        description = "A decision selecting one of the methods from the provided schema, with arguments.",
        responseClass = AiDecision::class
    )
    fun decideNextAction(
        @AiParameter(description = "JSON schema of the target API's available methods. " +
            "Pick one of these method names for the 'method' field in your response.")
        currentObject: String
    ): Future<AiDecision>
}
