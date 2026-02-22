package com.adamhammer.shimmer

import com.adamhammer.shimmer.agents.AutonomousAIApi
import com.adamhammer.shimmer.agents.AutonomousAgent
import com.adamhammer.shimmer.agents.DecidingAgentAPI
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
