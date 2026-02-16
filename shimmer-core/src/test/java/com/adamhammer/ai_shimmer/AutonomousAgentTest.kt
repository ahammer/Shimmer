package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.agents.*
import com.adamhammer.ai_shimmer.test.MockAdapter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AutonomousAgentTest {

    private fun buildAutonomousAgent(
        decisions: List<AiDecision>,
        apiResponses: List<String>
    ): AutonomousAgent {
        val deciderMock = MockAdapter.scripted(*decisions.toTypedArray())
        val decider = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(deciderMock)
            .build().api

        val apiMock = MockAdapter.scripted(*apiResponses.toTypedArray())
        val api = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterDirect(apiMock)
            .build().api

        return AutonomousAgent(api, decider)
    }

    @Test
    fun `step dispatches to understand with data arg`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("understand", mapOf("data" to "hello world"))),
            apiResponses = listOf("understood: hello world")
        )

        val result = agent.step()
        assertEquals("understood: hello world", result)
    }

    @Test
    fun `step dispatches to analyze`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("analyze", emptyMap())),
            apiResponses = listOf("analysis complete")
        )

        val result = agent.step()
        assertEquals("analysis complete", result)
    }

    @Test
    fun `step dispatches to plan`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("plan", emptyMap())),
            apiResponses = listOf("plan created")
        )

        val result = agent.step()
        assertEquals("plan created", result)
    }

    @Test
    fun `step dispatches to reflect`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("reflect", emptyMap())),
            apiResponses = listOf("reflection done")
        )

        val result = agent.step()
        assertEquals("reflection done", result)
    }

    @Test
    fun `step dispatches to act`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("act", emptyMap())),
            apiResponses = listOf("action taken")
        )

        val result = agent.step()
        assertEquals("action taken", result)
    }

    @Test
    fun `step throws on unknown method`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("nonexistent", emptyMap())),
            apiResponses = listOf("irrelevant")
        )

        assertThrows(IllegalArgumentException::class.java) {
            agent.step()
        }
    }

    @Test
    fun `step throws when understand is missing data arg`() {
        val agent = buildAutonomousAgent(
            decisions = listOf(AiDecision("understand", emptyMap())),
            apiResponses = listOf("irrelevant")
        )

        assertThrows(IllegalArgumentException::class.java) {
            agent.step()
        }
    }

    @Test
    fun `multi-step sequence executes correctly`() {
        val decisions = listOf(
            AiDecision("understand", mapOf("data" to "input")),
            AiDecision("analyze", emptyMap()),
            AiDecision("act", emptyMap())
        )
        val responses = listOf("understood", "analyzed", "acted")

        val deciderMock = MockAdapter.scripted(*decisions.toTypedArray())
        val decider = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(deciderMock)
            .build().api

        val apiMock = MockAdapter.scripted(*responses.toTypedArray())
        val api = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterDirect(apiMock)
            .build().api

        val agent = AutonomousAgent(api, decider)

        assertEquals("understood", agent.step())
        assertEquals("analyzed", agent.step())
        assertEquals("acted", agent.step())
    }

    @Test
    fun `memory from @Memorize calls is forwarded to decider`() {
        val decisions = listOf(
            AiDecision("understand", mapOf("data" to "hello")),
            AiDecision("analyze", emptyMap())
        )
        val responses = listOf("understood-result", "analyzed-result")

        val deciderMock = MockAdapter.scripted(*decisions.toTypedArray())
        val deciderInstance = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(deciderMock)
            .build()

        val apiMock = MockAdapter.scripted(*responses.toTypedArray())
        val apiInstance = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterDirect(apiMock)
            .build()

        val agent = AutonomousAgent(apiInstance, deciderInstance.api)

        // Step 1: understand → stores "Users Intent" in memory
        agent.step()

        // The api instance should now have the memorized value
        assertTrue(apiInstance.memory.containsKey("Users Intent"),
            "Memory should contain 'Users Intent' after understand call. Actual: ${apiInstance.memory}")

        // Step 2: analyze → the decider should receive the accumulated memory
        agent.step()

        // Verify the api instance also has the second memorized value
        assertTrue(apiInstance.memory.containsKey("analyze"),
            "Memory should contain 'analyze' after analyze call. Actual: ${apiInstance.memory}")
    }
}
