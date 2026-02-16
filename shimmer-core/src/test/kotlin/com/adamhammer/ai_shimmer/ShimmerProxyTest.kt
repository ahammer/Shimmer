package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.StubAdapter
import com.adamhammer.ai_shimmer.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShimmerProxyTest {

    @Test
    fun `non-Future return type throws UnsupportedOperationException`() {
        val api = ShimmerBuilder(NonFutureAPI::class)
            .setAdapterDirect(MockAdapter.scripted(SimpleResult()))
            .build().api

        assertThrows(UnsupportedOperationException::class.java) {
            api.badMethod()
        }
    }

    @Test
    fun `toString on proxy returns descriptive string`() {
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val str = api.toString()
        assertTrue(str.contains("SimpleTestAPI"), "Expected proxy toString to contain interface name, got: $str")
    }

    @Test
    fun `hashCode on proxy does not throw`() {
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val hash = api.hashCode()
        assertNotEquals(0, hash)
    }

    @Test
    fun `equals on proxy uses reference equality`() {
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        assertEquals(api, api)
        assertNotEquals(api, "not a proxy")
    }

    @Test
    fun `zero-arg method works correctly`() {
        val mock = MockAdapter.scripted(SimpleResult("hello"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.get().get()
        assertEquals("hello", result.value)
        mock.verifyCallCount(1)
    }

    @Test
    fun `method with parameter passes args through context`() {
        val mock = MockAdapter.scripted(SimpleResult("response"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        api.getWithParam("test-input").get()

        mock.verifyCallCount(1)
        mock.lastContext!!.assertMethodInvocationContains("test-input")
    }

    @Test
    fun `string result type works with stub adapter`() {
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val result = api.getString().get()
        assertEquals("", result)
    }
}
