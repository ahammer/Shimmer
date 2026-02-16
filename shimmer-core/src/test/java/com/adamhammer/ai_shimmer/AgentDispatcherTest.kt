package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.agents.AgentDispatcher
import com.adamhammer.ai_shimmer.agents.AiDecision
import com.adamhammer.ai_shimmer.agents.AutonomousAIApi
import com.adamhammer.ai_shimmer.test.MockAdapter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgentDispatcherTest {

    private fun buildDispatcher(vararg responses: String): AgentDispatcher<AutonomousAIApi> {
        val mock = MockAdapter.scripted(*responses)
        val instance = ShimmerBuilder(AutonomousAIApi::class)
            .setAdapterDirect(mock)
            .build()
        return AgentDispatcher(instance)
    }

    @Test
    fun `dispatch calls method by name and returns result`() {
        val dispatcher = buildDispatcher("analysis-result")
        val result = dispatcher.dispatch(AiDecision("analyze", emptyMap()))
        assertEquals("analysis-result", result)
    }

    @Test
    fun `dispatch passes argument to single-param method`() {
        val dispatcher = buildDispatcher("understood")
        val result = dispatcher.dispatch(AiDecision("understand", mapOf("data" to "input")))
        assertEquals("understood", result)
    }

    @Test
    fun `dispatch throws for unknown method`() {
        val dispatcher = buildDispatcher("irrelevant")
        assertThrows(IllegalArgumentException::class.java) {
            dispatcher.dispatch(AiDecision("nonexistent", emptyMap()))
        }
    }

    @Test
    fun `dispatch throws when required arg is missing`() {
        val dispatcher = buildDispatcher("irrelevant")
        assertThrows(IllegalArgumentException::class.java) {
            dispatcher.dispatch(AiDecision("understand", emptyMap()))
        }
    }

    @Test
    fun `dispatch calls all parameterless methods`() {
        val methods = listOf("analyze", "plan", "reflect", "act")
        for (methodName in methods) {
            val dispatcher = buildDispatcher("$methodName-result")
            val result = dispatcher.dispatch(AiDecision(methodName, emptyMap()))
            assertEquals("$methodName-result", result)
        }
    }
}
