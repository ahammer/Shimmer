package com.adamhammer.shimmer

import com.adamhammer.shimmer.adapters.StubAdapter
import com.adamhammer.shimmer.annotations.*
import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.model.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlin.reflect.KClass

class ResilienceTest {

    @Serializable
    @AiSchema(title = "SimpleResult", description = "A simple result")
    data class SimpleResult(
        @field:AiSchema(title = "Value", description = "The value")
        val value: String = "default"
    )

    interface SimpleAPI {
        @AiOperation(summary = "Get", description = "Gets a simple result")
        @AiResponse(description = "The result", responseClass = SimpleResult::class)
        fun get(): Future<SimpleResult>
    }

    @Test
    fun `default resilience policy works with stub adapter`() {
        val api = ShimmerBuilder(SimpleAPI::class)
            .setAdapterClass(StubAdapter::class)
            .build().api

        val result = api.get().get()
        assertNotNull(result)
        assertEquals("default", result.value)
    }

    @Test
    fun `result validator rejects invalid results and retries`() {
        var callCount = 0

        val countingAdapter = object : ApiAdapter {
            override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                callCount++
                @Suppress("UNCHECKED_CAST")
                return SimpleResult(value = if (callCount >= 3) "valid" else "") as R
            }
        }

        val api = ShimmerBuilder(SimpleAPI::class)
            .setAdapterDirect(countingAdapter)
            .setResiliencePolicy(
                ResiliencePolicy(
                    maxRetries = 3,
                    retryDelayMs = 10,
                    resultValidator = { result ->
                        (result as? SimpleResult)?.value?.isNotBlank() == true
                    }
                )
            )
            .build().api

        val result = api.get().get()
        assertEquals("valid", result.value)
        assertEquals(3, callCount)
    }

    @Test
    fun `fallback adapter is used when primary exhausts retries`() {
        val failingAdapter = object : ApiAdapter {
            override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                throw RuntimeException("Primary failed")
            }
        }

        val fallbackAdapter = object : ApiAdapter {
            override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                @Suppress("UNCHECKED_CAST")
                return SimpleResult(value = "fallback") as R
            }
        }

        val api = ShimmerBuilder(SimpleAPI::class)
            .setAdapterDirect(failingAdapter)
            .setResiliencePolicy(
                ResiliencePolicy(
                    maxRetries = 1,
                    retryDelayMs = 10,
                    fallbackAdapter = fallbackAdapter
                )
            )
            .build().api

        val result = api.get().get()
        assertEquals("fallback", result.value)
    }

    @Test
    fun `ShimmerException thrown when all retries and fallback fail`() {
        val failingAdapter = object : ApiAdapter {
            override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                throw RuntimeException("Failed")
            }
        }

        val api = ShimmerBuilder(SimpleAPI::class)
            .setAdapterDirect(failingAdapter)
            .setResiliencePolicy(
                ResiliencePolicy(maxRetries = 1, retryDelayMs = 10)
            )
            .build().api

        val exception = assertThrows(java.util.concurrent.ExecutionException::class.java) {
            api.get().get()
        }
        assertTrue(exception.cause is ShimmerException)
    }

    @Test
    fun `interceptors are applied before adapter receives request`() {
        var receivedInstructions = ""

        val capturingAdapter = object : ApiAdapter {
            override fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
                receivedInstructions = context.systemInstructions
                @Suppress("UNCHECKED_CAST")
                return SimpleResult(value = "captured") as R
            }
        }

        val api = ShimmerBuilder(SimpleAPI::class)
            .setAdapterDirect(capturingAdapter)
            .addInterceptor { ctx ->
                ctx.copy(systemInstructions = ctx.systemInstructions + "\n[INJECTED]")
            }
            .build().api

        api.get().get()
        assertTrue(receivedInstructions.contains("[INJECTED]"))
    }
}
