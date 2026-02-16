package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.StubAdapter
import com.adamhammer.ai_shimmer.annotations.*
import com.adamhammer.ai_shimmer.test.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class StubAdapterTest {

    @Test
    fun `StubAdapter handles String results`() {
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val result = api.getString().get()
        assertEquals("", result)
    }

    @Test
    fun `StubAdapter handles enum results`() {
        val api = ShimmerBuilder(EnumAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val result = api.getColor().get()
        assertEquals(TestColor.RED, result)  // first enum constant
    }

    @Test
    fun `StubAdapter handles data class with defaults`() {
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val result = api.get().get()
        assertEquals("default", result.value)
    }

    interface EnumAPI {
        @AiOperation(summary = "GetColor", description = "Gets a color")
        @AiResponse(description = "The color", responseClass = TestColor::class)
        fun getColor(): Future<TestColor>
    }
}
