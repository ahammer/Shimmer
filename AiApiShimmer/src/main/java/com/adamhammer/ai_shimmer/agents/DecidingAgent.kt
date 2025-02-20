package com.adamhammer.ai_shimmer.agents

import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.utils.MethodUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter
import java.util.concurrent.Future

interface DecidingAgentAPI {
    @Operation(
        summary = "Decide Next Action",
        description = "Introspects the provided object to build a decision schema and then determines the next action based on its capabilities."
    )
    @ApiResponse(
        description = "Returns a Future that resolves to an AiDecision based on the introspected object's snapshot.",
        content = [Content(schema = Schema(implementation = AiDecision::class))]
    )
    fun decideNextAction(
        @Parameter(description = "A JSON string representing the introspected object and its capabilities.")
        currentObject: String
    ): Future<AiDecision>
}

class DecidingAgent(val api: DecidingAgentAPI) {
    fun decideNext(obj: Any) : Future<AiDecision> {
        return api.decideNextAction(MethodUtils.parseObjectForDecisionSchema(obj))
    }
}
