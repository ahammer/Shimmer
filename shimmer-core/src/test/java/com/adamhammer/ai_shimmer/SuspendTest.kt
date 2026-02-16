package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SuspendTest {

    @Test
    fun `suspend function returns result`() = runBlocking {
        val mock = MockAdapter.scripted(SimpleResult("hello"))
        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.get()
        assertEquals("hello", result.value)
        mock.verifyCallCount(1)
    }

    @Test
    fun `suspend function returns string`() = runBlocking {
        val mock = MockAdapter.scripted("world")
        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.getString()
        assertEquals("world", result)
    }

    @Test
    fun `suspend function with parameter`() = runBlocking {
        val mock = MockAdapter.scripted(SimpleResult("param-result"))
        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.getWithParam("test-input")
        assertEquals("param-result", result.value)
        mock.verifyCallCount(1)
    }

    @Test
    fun `suspend function works with shimmer DSL`() = runBlocking {
        val mock = MockAdapter.scripted(SimpleResult("dsl"))
        val instance = shimmer<SuspendTestAPI> {
            adapter(mock)
        }

        val result = instance.api.get()
        assertEquals("dsl", result.value)
    }
}
