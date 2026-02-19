package com.adamhammer.shimmer

import com.adamhammer.shimmer.agents.*
import com.adamhammer.shimmer.test.MockAdapter
import com.adamhammer.shimmer.test.SimpleResult
import com.adamhammer.shimmer.test.assertMethodInvocationContains
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DecidingAgentTest {

    @Test
    fun `decide returns AiDecision from scripted adapter`() {
        val expectedDecision = AiDecision(method = "analyze", args = emptyList())
        val mock = MockAdapter.scripted(expectedDecision)

        val decider = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(mock)
            .build()

        val result = decider.api.decideNextAction("{methods: [analyze, plan]}").get()
        assertEquals("analyze", result.method)
        assertTrue(result.args.isEmpty())
        mock.verifyCallCount(1)
    }

    @Test
    fun `decide captures schema in method invocation context`() {
        val decision = AiDecision(method = "test", args = listOf(AiArg("key", "val")))
        val mock = MockAdapter.scripted(decision)

        val decider = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(mock)
            .build()

        decider.api.decideNextAction("my-schema-data").get()

        mock.lastContext!!.assertMethodInvocationContains("my-schema-data")
    }

    @Test
    fun `decide extension function creates proper schema from ShimmerInstance`() {
        val decision = AiDecision(method = "get", args = emptyList())
        val mock = MockAdapter.scripted(decision)

        val decider = ShimmerBuilder(DecidingAgentAPI::class)
            .setAdapterDirect(mock)
            .build()

        // Create a ShimmerInstance wrapping SimpleTestAPI to test the decide() extension
        val apiMock = MockAdapter.scripted(SimpleResult())
        val targetInstance = ShimmerBuilder(com.adamhammer.shimmer.test.SimpleTestAPI::class)
            .setAdapterDirect(apiMock)
            .build()

        val result = decider.api.decide(targetInstance).get()
        assertEquals("get", result.method)

        // Verify that the schema was passed in the invocation
        val invocation = mock.lastContext!!.methodInvocation
        assertTrue(invocation.contains("SimpleTestAPI"), "Schema should contain target API name, got: $invocation")
    }
}
