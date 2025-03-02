package com.adamhammer.ai_shimmer.agents

import com.adamhammer.ai_shimmer.interfaces.AiDecision
import com.adamhammer.ai_shimmer.interfaces.ApiAdapter
import com.adamhammer.ai_shimmer.utils.MethodUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Parameter
import java.util.concurrent.Future

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

class DecidingAgent(val api: DecidingAgentAPI) {
    fun decideNext(obj: ApiAdapter) : Future<AiDecision> {
        val executionSchema = MethodUtils.parseObjectForDecisionSchema(obj);
        return api.decideNextAction(executionSchema)
    }
}
