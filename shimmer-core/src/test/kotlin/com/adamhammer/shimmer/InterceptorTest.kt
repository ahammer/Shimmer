package com.adamhammer.shimmer

import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.model.PromptContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InterceptorTest {

    private fun baseContext() = PromptContext(
        systemInstructions = "You are an AI.",
        methodInvocation = """{"method": "test"}""",
        memory = mapOf("key" to "value")
    )

    @Test
    fun `single interceptor can modify system instructions`() {
        val interceptor = Interceptor { ctx ->
            ctx.copy(systemInstructions = ctx.systemInstructions + "\nBe concise.")
        }

        val result = interceptor.intercept(baseContext())

        assertTrue(result.systemInstructions.endsWith("Be concise."))
    }

    @Test
    fun `interceptor can add properties`() {
        val interceptor = Interceptor { ctx ->
            ctx.copy(properties = ctx.properties + ("temperature" to 0.7))
        }

        val result = interceptor.intercept(baseContext())

        assertEquals(0.7, result.properties["temperature"])
    }

    @Test
    fun `interceptors chain in order`() {
        val first = Interceptor { ctx ->
            ctx.copy(systemInstructions = ctx.systemInstructions + " [FIRST]")
        }
        val second = Interceptor { ctx ->
            ctx.copy(systemInstructions = ctx.systemInstructions + " [SECOND]")
        }

        var context = baseContext()
        for (interceptor in listOf(first, second)) {
            context = interceptor.intercept(context)
        }

        assertTrue(context.systemInstructions.endsWith("[FIRST] [SECOND]"))
    }

    @Test
    fun `interceptor can filter memory`() {
        val context = baseContext().copy(memory = mapOf("keep" to "yes", "secret" to "no"))

        val interceptor = Interceptor { ctx ->
            ctx.copy(memory = ctx.memory.filterKeys { it != "secret" })
        }

        val result = interceptor.intercept(context)

        assertEquals(mapOf("keep" to "yes"), result.memory)
    }

    @Test
    fun `interceptor can enrich system instructions with world state`() {
        val worldState = """{"hp": 20, "location": "tavern"}"""
        val interceptor = Interceptor { ctx ->
            ctx.copy(
                systemInstructions = ctx.systemInstructions + "\n\n# WORLD STATE\n$worldState"
            )
        }

        val result = interceptor.intercept(baseContext())

        assertTrue(result.systemInstructions.contains("WORLD STATE"))
        assertTrue(result.systemInstructions.contains("tavern"))
    }
}
