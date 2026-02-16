package com.adamhammer.ai_shimmer.test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestHelpersTest {

    @Test
    fun `shimmerTest creates proxy with mock adapter`() {
        val mock = MockAdapter.scripted(SimpleResult("from-helper"))
        val (api, returnedMock) = shimmerTest<SimpleTestAPI>(mock)

        val result = api.get().get()
        assertEquals("from-helper", result.value)
        assertSame(mock, returnedMock)
        returnedMock.verifyCallCount(1)
    }

    @Test
    fun `shimmerTest captures context for verification`() {
        val mock = MockAdapter.scripted(SimpleResult("ctx-check"))
        val (api, adapter) = shimmerTest<SimpleTestAPI>(mock)

        api.getWithParam("hello-param").get()
        adapter.lastContext!!.assertMethodInvocationContains("hello-param")
    }

    @Test
    fun `shimmerStub returns default-constructed results`() {
        val api = shimmerStub<SimpleTestAPI>()

        val result = api.get().get()
        assertNotNull(result)
        assertEquals("default", result.value)
    }

    @Test
    fun `shimmerStub getString returns empty string`() {
        val api = shimmerStub<SimpleTestAPI>()

        val result = api.getString().get()
        assertEquals("", result)
    }
}
