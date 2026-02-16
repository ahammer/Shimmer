package com.adamhammer.ai_shimmer.agents

import com.adamhammer.ai_shimmer.ShimmerInstance
import com.adamhammer.ai_shimmer.utils.toJsonClassMetadataString
import kotlinx.serialization.Serializable
import java.util.concurrent.Future

import com.adamhammer.ai_shimmer.annotations.AiSchema
import com.adamhammer.ai_shimmer.annotations.AiOperation
import com.adamhammer.ai_shimmer.annotations.AiResponse
import com.adamhammer.ai_shimmer.annotations.AiParameter

@Serializable
@AiSchema(title = "AI Decision", description = "Look at the current state and options and decide what to do next")
data class AiDecision(
    @field:AiSchema(description = "The method to call with the arguments")
    val method: String,
    @field:AiSchema(description = "The arguments to pass to this call.")
    val args: Map<String, String>
)

fun <T : Any> DecidingAgentAPI.decide(shimmerInstance: ShimmerInstance<T>): Future<AiDecision> {
    val schema = shimmerInstance.klass.toJsonClassMetadataString()
    return decideNextAction(schema)
}

interface DecidingAgentAPI {
    @AiOperation(
        summary = "Decide Next Action",
        description = "Introspects the provided object to build a decision schema and then determines the next action based on its capabilities."
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
