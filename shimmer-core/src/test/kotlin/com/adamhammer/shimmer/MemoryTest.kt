package com.adamhammer.shimmer

import com.adamhammer.shimmer.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MemoryTest {

    @Test
    fun `@Memorize stores result in memory map`() {
        val mock = MockAdapter.scripted("stored-response", "recalled")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("hello").get()

        assertTrue(instance.memory.containsKey("stored-value"),
            "Memory should contain the @Memorize label. Actual: ${instance.memory}")
    }

    @Test
    fun `stored memory is passed to subsequent calls`() {
        val mock = MockAdapter.scripted("first-result", "second-result")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("input").get()
        instance.api.recall().get()

        // Memory is now stored as JSON string (quoted)
        mock.contextAt(1).assertMemoryContains("stored-value", "\"first-result\"")
    }

    @Test
    fun `multiple @Memorize methods accumulate independently`() {
        val mock = MockAdapter.scripted("alpha", "beta")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("x").get()
        instance.api.recall().get()

        // Memory values are stored as JSON (quoted strings)
        assertEquals("\"alpha\"", instance.memory["stored-value"])
        assertEquals("\"beta\"", instance.memory["recalled-value"])
    }

    @Test
    fun `first call has empty memory`() {
        val mock = MockAdapter.scripted("result")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("test").get()

        mock.contextAt(0).assertMemoryEmpty()
    }
}
