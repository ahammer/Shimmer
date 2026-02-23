package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.test.MockAdapter
import ca.adamhammer.shimmer.test.StreamingTestAPI
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StreamingTest {

    @Test
    fun `streaming method returns Flow that emits response`() = runBlocking {
        val mock = MockAdapter.scripted("streamed-output")
        val api = ShimmerBuilder(StreamingTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.stream().toList()
        assertEquals(listOf("streamed-output"), result)
        mock.verifyCallCount(1)
    }

    @Test
    fun `streaming method with parameter passes context`() = runBlocking {
        val mock = MockAdapter.scripted("echo-result")
        val api = ShimmerBuilder(StreamingTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        val result = api.streamWithParam("hello").toList()
        assertEquals(listOf("echo-result"), result)
        mock.verifyCallCount(1)
        assertTrue(mock.lastContext!!.methodInvocation.contains("hello"))
    }
}
