package com.adamhammer.ai_shimmer

import com.adamhammer.ai_shimmer.adapters.StubAdapter
import com.adamhammer.ai_shimmer.context.DefaultContextBuilder
import com.adamhammer.ai_shimmer.interfaces.ContextBuilder
import com.adamhammer.ai_shimmer.model.PromptContext
import com.adamhammer.ai_shimmer.model.ShimmerConfigurationException
import com.adamhammer.ai_shimmer.model.ShimmerRequest
import com.adamhammer.ai_shimmer.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShimmerBuilderTest {

    @Test
    fun `build without adapter throws ShimmerConfigurationException`() {
        assertThrows(ShimmerConfigurationException::class.java) {
            ShimmerBuilder(SimpleTestAPI::class).build()
        }
    }

    @Test
    fun `setAdapterClass with no all-optional constructor throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            ShimmerBuilder(SimpleTestAPI::class)
                .setAdapterClass(NoDefaultConstructorAdapter::class)
                .build()
        }
    }

    @Test
    fun `setAdapterDirect sets adapter correctly`() {
        val mock = MockAdapter.scripted(SimpleResult("direct"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .build().api

        assertEquals("direct", api.get().get().value)
    }

    @Test
    fun `setContextBuilder replaces default`() {
        val customBuilder = object : ContextBuilder {
            override fun build(request: ShimmerRequest): PromptContext {
                return PromptContext(
                    systemInstructions = "CUSTOM BUILDER",
                    methodInvocation = "{}",
                    memory = request.memory
                )
            }
        }

        val mock = MockAdapter.scripted(SimpleResult("custom"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .setContextBuilder(customBuilder)
            .build().api

        api.get().get()
        mock.lastContext!!.assertSystemInstructionsContain("CUSTOM BUILDER")
    }

    @Test
    fun `multiple addInterceptor preserves registration order`() {
        val mock = MockAdapter.scripted(SimpleResult("ordered"))
        val api = ShimmerBuilder(SimpleTestAPI::class)
            .setAdapterDirect(mock)
            .addInterceptor { ctx -> ctx.copy(systemInstructions = ctx.systemInstructions + " [A]") }
            .addInterceptor { ctx -> ctx.copy(systemInstructions = ctx.systemInstructions + " [B]") }
            .addInterceptor { ctx -> ctx.copy(systemInstructions = ctx.systemInstructions + " [C]") }
            .build().api

        api.get().get()
        val instructions = mock.lastContext!!.systemInstructions
        val indexA = instructions.indexOf("[A]")
        val indexB = instructions.indexOf("[B]")
        val indexC = instructions.indexOf("[C]")
        assertTrue(indexA < indexB && indexB < indexC, "Interceptors should run in order: $instructions")
    }

    @Test
    fun `building multiple instances from same builder produces independent instances`() {
        val mock = MockAdapter.scripted(SimpleResult("v1"), SimpleResult("v2"))
        val builder = ShimmerBuilder(SimpleTestAPI::class).setAdapterDirect(mock)

        val instance1 = builder.build()
        val instance2 = builder.build()

        assertNotSame(instance1.api, instance2.api)
    }
}

// Test helper: an adapter class with required constructor params
class NoDefaultConstructorAdapter(required: String) : com.adamhammer.ai_shimmer.interfaces.ApiAdapter {
    override fun <R : Any> handleRequest(context: PromptContext, resultClass: kotlin.reflect.KClass<R>): R {
        throw UnsupportedOperationException()
    }
}
