package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.agents.AutonomousAIApi
import com.adamhammer.ai_shimmer.agents.AutonomousAgent
import com.adamhammer.ai_shimmer.agents.DecidingAgentAPI
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DecidingAgentTest {

    @Test
    @Disabled("WIP: Requires live OpenAI API")
    fun testIdeate() {
        // val agentAdapter = ShimmerBuilder(AutonomousAIApi::class)
        //     .setAdapterClass(OpenAiAdapter::class)
        //     .build()

        // val deciderAdapter = ShimmerBuilder(DecidingAgentAPI::class)
        //     .setAdapterClass(OpenAiAdapter::class)
        //     .build()

        // val agent = AutonomousAgent(agentAdapter.api, deciderAdapter.api)
        // val result = agent.step()
        // println("Step result: $result")
    }
}
