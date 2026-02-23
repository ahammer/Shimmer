package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MemoryTest {

    @Test
    fun `@Memorize stores result in memory map`() = runBlocking {
        val mock = MockAdapter.scripted("stored-response", "recalled")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("hello").get()

        assertTrue(instance.memoryStore.getAll().containsKey("stored-value"),
            "Memory should contain the @Memorize label. Actual: ${instance.memoryStore.getAll()}")
    }

    @Test
    fun `stored memory is passed to subsequent calls`() = runBlocking {
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
    fun `multiple @Memorize methods accumulate independently`() = runBlocking {
        val mock = MockAdapter.scripted("alpha", "beta")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("x").get()
        instance.api.recall().get()

        // Memory values are stored as JSON (quoted strings)
        assertEquals("\"alpha\"", instance.memoryStore.getAll()["stored-value"])
        assertEquals("\"beta\"", instance.memoryStore.getAll()["recalled-value"])
    }

    @Test
    fun `first call has empty memory`() = runBlocking {
        val mock = MockAdapter.scripted("result")
        val instance = ShimmerBuilder(MemoryTestAPI::class)
            .setAdapterDirect(mock)
            .build()

        instance.api.store("test").get()

        mock.contextAt(0).assertMemoryEmpty()
    }
}
