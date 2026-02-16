package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.model.ShimmerRequest
import com.adamhammer.ai_shimmer.test.SimpleTestAPI
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShimmerRequestTest {

    private val method = SimpleTestAPI::class.java.getMethod("get")

    @Test
    fun `equal requests with same args are equal`() {
        val r1 = ShimmerRequest(method, listOf("a", "b"), mapOf("k" to "v"), String::class)
        val r2 = ShimmerRequest(method, listOf("a", "b"), mapOf("k" to "v"), String::class)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `requests with different args are not equal`() {
        val r1 = ShimmerRequest(method, listOf("a"), emptyMap(), String::class)
        val r2 = ShimmerRequest(method, listOf("b"), emptyMap(), String::class)
        assertNotEquals(r1, r2)
    }

    @Test
    fun `requests with null args are equal`() {
        val r1 = ShimmerRequest(method, null, emptyMap(), String::class)
        val r2 = ShimmerRequest(method, null, emptyMap(), String::class)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `request with null args not equal to request with empty args`() {
        val r1 = ShimmerRequest(method, null, emptyMap(), String::class)
        val r2 = ShimmerRequest(method, emptyList(), emptyMap(), String::class)
        assertNotEquals(r1, r2)
    }
}
