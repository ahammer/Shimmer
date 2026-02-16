package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.annotations.*
import com.adamhammer.ai_shimmer.context.DefaultContextBuilder
import com.adamhammer.ai_shimmer.model.ShimmerRequest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class ContextBuilderTest {

    @Serializable
    @AiSchema(title = "Greeting", description = "A greeting response")
    data class Greeting(
        @field:AiSchema(title = "Message", description = "The greeting message")
        val message: String = ""
    )

    interface GreetingAPI {
        @AiOperation(summary = "Greet", description = "Generates a greeting for the given name")
        @AiResponse(description = "The greeting", responseClass = Greeting::class)
        fun greet(
            @AiParameter(description = "The name to greet") name: String
        ): Future<Greeting>
    }

    @Test
    fun `DefaultContextBuilder produces system instructions`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val request = ShimmerRequest(method, arrayOf("Alice"), emptyMap(), Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertTrue(context.systemInstructions.contains("specialized AI Assistant"))
        assertTrue(context.systemInstructions.contains("resultSchema"))
    }

    @Test
    fun `DefaultContextBuilder includes method invocation`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val request = ShimmerRequest(method, arrayOf("Alice"), emptyMap(), Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertTrue(context.methodInvocation.contains("greet"))
        assertTrue(context.methodInvocation.contains("Alice"))
        assertTrue(context.methodInvocation.contains("resultSchema"))
    }

    @Test
    fun `DefaultContextBuilder passes memory through without embedding in invocation`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val memory = mapOf("prev" to "Hello Bob")
        val request = ShimmerRequest(method, arrayOf("Alice"), memory, Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertEquals(memory, context.memory)
        // Memory should NOT be in the method invocation (passed as emptyMap to toJsonInvocationString)
        assertFalse(context.methodInvocation.contains("Hello Bob"))
    }

    @Test
    fun `DefaultContextBuilder produces empty properties`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val request = ShimmerRequest(method, arrayOf("Alice"), emptyMap(), Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertTrue(context.properties.isEmpty())
    }
}
