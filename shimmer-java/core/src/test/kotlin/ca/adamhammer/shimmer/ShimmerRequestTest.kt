package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.model.MethodDescriptor
import ca.adamhammer.shimmer.model.ShimmerRequest
import ca.adamhammer.shimmer.test.SimpleTestAPI
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShimmerRequestTest {

    private val method = SimpleTestAPI::class.java.getMethod("get")
    private val methodWithParam = SimpleTestAPI::class.java.getMethod("getWithParam", String::class.java)

    @Test
    fun `equal requests with same args are equal`() {
        val desc = MethodDescriptor.from(methodWithParam, listOf("a"), String::class)
        val r1 = ShimmerRequest(desc, mapOf("k" to "v"), String::class)
        val r2 = ShimmerRequest(desc, mapOf("k" to "v"), String::class)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `requests with different args are not equal`() {
        val r1 = ShimmerRequest(MethodDescriptor.from(methodWithParam, listOf("a"), String::class), emptyMap(), String::class)
        val r2 = ShimmerRequest(MethodDescriptor.from(methodWithParam, listOf("b"), String::class), emptyMap(), String::class)
        assertNotEquals(r1, r2)
    }

    @Test
    fun `requests with null args are equal`() {
        val r1 = ShimmerRequest(MethodDescriptor.from(method, null, String::class), emptyMap(), String::class)
        val r2 = ShimmerRequest(MethodDescriptor.from(method, null, String::class), emptyMap(), String::class)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `request with null args not equal to request with args`() {
        val r1 = ShimmerRequest(MethodDescriptor.from(methodWithParam, null, String::class), emptyMap(), String::class)
        val r2 = ShimmerRequest(MethodDescriptor.from(methodWithParam, listOf("a"), String::class), emptyMap(), String::class)
        assertNotEquals(r1, r2)
    }
}
