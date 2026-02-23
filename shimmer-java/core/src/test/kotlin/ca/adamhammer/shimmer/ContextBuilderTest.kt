package ca.adamhammer.shimmer

import ca.adamhammer.shimmer.annotations.*
import ca.adamhammer.shimmer.context.DefaultContextBuilder
import ca.adamhammer.shimmer.model.MethodDescriptor
import ca.adamhammer.shimmer.model.ShimmerRequest
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
        val request = ShimmerRequest(MethodDescriptor.from(method, listOf("Alice"), Greeting::class), emptyMap(), Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertTrue(context.systemInstructions.contains("specialized AI Assistant"))
        assertTrue(context.systemInstructions.contains("resultSchema"))
    }

    @Test
    fun `DefaultContextBuilder includes method invocation`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val request = ShimmerRequest(MethodDescriptor.from(method, listOf("Alice"), Greeting::class), emptyMap(), Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertTrue(context.methodInvocation.contains("greet"))
        assertTrue(context.methodInvocation.contains("Alice"))
        assertTrue(context.methodInvocation.contains("resultSchema"))
    }

    @Test
    fun `DefaultContextBuilder passes memory through without embedding in invocation`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val memory = mapOf("prev" to "Hello Bob")
        val request = ShimmerRequest(MethodDescriptor.from(method, listOf("Alice"), Greeting::class), memory, Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertEquals(memory, context.memory)
        // Memory should NOT be in the method invocation (passed as emptyMap to toJsonInvocationString)
        assertFalse(context.methodInvocation.contains("Hello Bob"))
    }

    @Test
    fun `DefaultContextBuilder produces empty properties`() {
        val method = GreetingAPI::class.java.getMethod("greet", String::class.java)
        val request = ShimmerRequest(MethodDescriptor.from(method, listOf("Alice"), Greeting::class), emptyMap(), Greeting::class)

        val context = DefaultContextBuilder().build(request)

        assertTrue(context.properties.isEmpty())
    }
}
