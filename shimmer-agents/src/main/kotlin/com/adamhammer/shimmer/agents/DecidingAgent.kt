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

@Serializable
@AiSchema(title = "AI Argument", description = "A single named argument")
data class AiArg(
    @field:AiSchema(description = "The name of the argument")
    val name: String,
    @field:AiSchema(description = "The value of the argument")
    val value: String
)

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

fun <T : Any> DecidingAgentAPI.decide(shimmerInstance: ShimmerInstance<T>): Future<AiDecision> {
    val schema = shimmerInstance.klass.toJsonClassMetadataString()
    return decideNextAction(schema)
}

suspend fun <T : Any> DecidingAgentAPI.decideSuspend(shimmerInstance: ShimmerInstance<T>): AiDecision =
    withContext(Dispatchers.IO) { decide(shimmerInstance).get() }

interface DecidingAgentAPI {
    @AiOperation(
        summary = "Decide Next Action",
        description = "Introspects the provided object to build a decision schema" +
            " and then determines the next action based on its capabilities."
    )
    @AiResponse(
        description = "A decision about the next action, based on the options and available input.",
        responseClass = AiDecision::class
    )
    fun decideNextAction(
        @AiParameter(description = "The current state and available options")
        currentObject: String
    ): Future<AiDecision>
}
