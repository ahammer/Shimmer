package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.interfaces.RequestListener
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.test.MockAdapter
import com.adamhammer.ai_shimmer.test.SimpleResult
import com.adamhammer.ai_shimmer.test.SimpleTestAPI
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections

class RequestListenerTest {

    @Test
    fun `listener receives onRequestStart and onRequestComplete`() {
        val startCalls = Collections.synchronizedList(mutableListOf<PromptContext>())
        val completeCalls = Collections.synchronizedList(mutableListOf<Long>())

        val listener = object : RequestListener {
            override fun onRequestStart(context: PromptContext) {
                startCalls.add(context)
            }
            override fun onRequestComplete(context: PromptContext, result: Any, durationMs: Long) {
                completeCalls.add(durationMs)
            }
        }

        val mock = MockAdapter.scripted(SimpleResult("ok"))
        val instance = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .listener(listener)
            .build()

        val result = instance.api.get().get()
        assertEquals("ok", result.value)

        assertEquals(1, startCalls.size, "Expected 1 start event")
        assertEquals(1, completeCalls.size, "Expected 1 complete event")
        assertTrue(completeCalls[0] >= 0, "Duration should be non-negative")
    }

    @Test
    fun `listener receives onRequestError on failure`() {
        val errorCalls = Collections.synchronizedList(mutableListOf<Pair<Exception, Long>>())

        val listener = object : RequestListener {
            override fun onRequestError(context: PromptContext, error: Exception, durationMs: Long) {
                errorCalls.add(error to durationMs)
            }
        }

        val mock = MockAdapter.builder()
            .responses(SimpleResult("irrelevant"))
            .failOnCall(0, RuntimeException("boom"))
            .build()

        val instance = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .listener(listener)
            .build()

        assertThrows(Exception::class.java) {
            instance.api.get().get()
        }

        assertEquals(1, errorCalls.size, "Expected 1 error event")
        val reportedError = errorCalls[0].first
        assertTrue(
            reportedError.message!!.contains("boom") || reportedError.cause?.message?.contains("boom") == true,
            "Error or its cause should contain original message, got: ${reportedError.message}"
        )
        assertTrue(errorCalls[0].second >= 0, "Duration should be non-negative")
    }

    @Test
    fun `multiple listeners all get notified`() {
        val listener1Events = Collections.synchronizedList(mutableListOf<String>())
        val listener2Events = Collections.synchronizedList(mutableListOf<String>())

        val listener1 = object : RequestListener {
            override fun onRequestStart(context: PromptContext) {
                listener1Events.add("start")
            }
            override fun onRequestComplete(context: PromptContext, result: Any, durationMs: Long) {
                listener1Events.add("complete")
            }
        }
        val listener2 = object : RequestListener {
            override fun onRequestStart(context: PromptContext) {
                listener2Events.add("start")
            }
            override fun onRequestComplete(context: PromptContext, result: Any, durationMs: Long) {
                listener2Events.add("complete")
            }
        }

        val mock = MockAdapter.scripted(SimpleResult("ok"))
        val instance = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .listener(listener1)
            .listener(listener2)
            .build()

        instance.api.get().get()

        assertEquals(listOf("start", "complete"), listener1Events)
        assertEquals(listOf("start", "complete"), listener2Events)
    }

    @Test
    fun `listener with no overrides does not throw`() {
        val noopListener = object : RequestListener {}

        val mock = MockAdapter.scripted(SimpleResult("ok"))
        val instance = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .listener(noopListener)
            .build()

        val result = instance.api.get().get()
        assertEquals("ok", result.value)
    }
}
