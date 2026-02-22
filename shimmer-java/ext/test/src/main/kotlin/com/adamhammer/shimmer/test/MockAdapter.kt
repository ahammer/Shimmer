package com.adamhammer.shimmer.test

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.model.PromptContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * A configurable test adapter for offline testing of Shimmer APIs.
 *
 * Supports:
 * - **Scripted responses**: queue responses returned in order per call
 * - **Dynamic responses**: a lambda that produces responses from context
 * - **Prompt capture**: records every [PromptContext] received
 * - **Failure simulation**: throw on specific call indices
 * - **Delay simulation**: sleep for a configurable duration (for timeout testing)
 *
 * Usage:
 * ```kotlin
 * val mock = MockAdapter.scripted(
 *     SimpleResult("first"),
 *     SimpleResult("second")
 * )
 * val api = ShimmerBuilder(MyAPI::class)
 *     .setAdapterDirect(mock)
 *     .build().api
 * ```
 */
class MockAdapter private constructor(
    private val responseProvider: (Int, PromptContext, KClass<*>) -> Any,
    private val delayMs: Long,
    private val failOnCalls: Map<Int, Exception>
) : ApiAdapter {

    private val _capturedContexts: MutableList<PromptContext> = Collections.synchronizedList(mutableListOf())
    private val _callCounter = AtomicInteger(0)

    /** All captured [PromptContext] objects received by this adapter, in call order. */
    val capturedContexts: List<PromptContext> get() = _capturedContexts.toList()

    /** The most recent [PromptContext] received, or null if no calls were made. */
    val lastContext: PromptContext? get() = _capturedContexts.lastOrNull()

    /** The [PromptContext] at the given call index (0-based). */
    fun contextAt(index: Int): PromptContext = _capturedContexts[index]

    /** Total number of calls made to this adapter. */
    val callCount: Int get() = _capturedContexts.size

    /** Assert that exactly [expected] calls were made. Throws [AssertionError] if not. */
    fun verifyCallCount(expected: Int) {
        if (_capturedContexts.size != expected) {
            throw AssertionError("Expected $expected call(s) but got ${_capturedContexts.size}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        val callIndex = _callCounter.getAndIncrement()
        _capturedContexts.add(context)

        failOnCalls[callIndex]?.let { throw it }

        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs)
        }

        return responseProvider(callIndex, context, resultClass) as R
    }

    override fun handleRequestStreaming(context: PromptContext): Flow<String> {
        val callIndex = _callCounter.getAndIncrement()
        _capturedContexts.add(context)

        failOnCalls[callIndex]?.let { throw it }

        val response = responseProvider(callIndex, context, String::class)
        return flow {
            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs)
            }
            emit(response.toString())
        }
    }

    companion object {
        /**
         * Create a [MockAdapter] that returns responses from a pre-defined queue.
         * Responses are returned in order. If more calls are made than responses provided,
         * the last response is repeated.
         */
        fun scripted(vararg responses: Any): MockAdapter {
            require(responses.isNotEmpty()) { "At least one response is required" }
            return MockAdapter(
                responseProvider = { index, _, _ ->
                    responses[index.coerceAtMost(responses.lastIndex)]
                },
                delayMs = 0,
                failOnCalls = emptyMap()
            )
        }

        /**
         * Create a [MockAdapter] that uses a lambda to produce responses dynamically.
         */
        fun dynamic(provider: (context: PromptContext, resultClass: KClass<*>) -> Any): MockAdapter {
            return MockAdapter(
                responseProvider = { _, context, klass -> provider(context, klass) },
                delayMs = 0,
                failOnCalls = emptyMap()
            )
        }

        /**
         * Create a builder for more complex mock configurations.
         */
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var responseProvider: (Int, PromptContext, KClass<*>) -> Any = { _, _, _ ->
            throw IllegalStateException("No response configured for MockAdapter")
        }
        private var delayMs: Long = 0
        private val failOnCalls = mutableMapOf<Int, Exception>()

        fun responses(vararg responses: Any): Builder {
            require(responses.isNotEmpty()) { "At least one response is required" }
            responseProvider = { index, _, _ ->
                responses[index.coerceAtMost(responses.lastIndex)]
            }
            return this
        }

        fun dynamicResponse(provider: (context: PromptContext, resultClass: KClass<*>) -> Any): Builder {
            responseProvider = { _, context, klass -> provider(context, klass) }
            return this
        }

        /** Simulate a delay on every call (useful for timeout testing). */
        fun delayMs(ms: Long): Builder {
            this.delayMs = ms
            return this
        }

        /** Throw the given exception on the Nth call (0-based index). */
        fun failOnCall(callIndex: Int, exception: Exception = RuntimeException("Simulated failure")): Builder {
            failOnCalls[callIndex] = exception
            return this
        }

        fun build(): MockAdapter = MockAdapter(responseProvider, delayMs, failOnCalls)
    }
}
