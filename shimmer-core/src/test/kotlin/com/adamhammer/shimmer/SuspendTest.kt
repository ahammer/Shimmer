package com.adamhammer.shimmer

import com.adamhammer.shimmer.model.ResiliencePolicy
import com.adamhammer.shimmer.model.ShimmerException
import com.adamhammer.shimmer.test.*
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

    @Test
    fun `suspend memorize stores result in memory`() = runBlocking {
        val mock = MockAdapter.scripted("stored-value", "recalled-value")
        val instance = ShimmerBuilder(SuspendMemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("input")
        assertTrue(instance.memoryStore.getAll().containsKey("suspend-stored"),
            "Memory should contain 'suspend-stored'. Actual: ${instance.memoryStore.getAll()}")
    }

    @Test
    fun `suspend memorize passes memory to subsequent calls`() = runBlocking {
        val mock = MockAdapter.scripted("first", "second")
        val instance = ShimmerBuilder(SuspendMemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("x")
        instance.api.recall()

        mock.contextAt(1).assertMemoryContains("suspend-stored", "\"first\"")
    }

    @Test
    fun `suspend function propagates adapter exception`() {
        val mock = MockAdapter.builder()
            .responses(SimpleResult("unused"))
            .failOnCall(0, RuntimeException("adapter-error"))
            .build()

        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val ex = assertThrows(ShimmerException::class.java) {
            runBlocking { api.get() }
        }
        assertTrue(ex.message!!.contains("failed"), "Expected failure message, got: ${ex.message}")
    }

    @Test
    fun `suspend function works with resilience retry`() = runBlocking {
        val mock = MockAdapter.builder()
            .responses(SimpleResult("ok"))
            .failOnCall(0, RuntimeException("transient"))
            .build()

        val api = ShimmerBuilder(SuspendTestAPI::class)
            .setAdapterDirect(mock)
            .resilience {
                maxRetries = 1
                retryDelayMs = 10
            }
            .build().api

        val result = api.get()
        assertEquals("ok", result.value)
        mock.verifyCallCount(2)
    }
}
