package com.adamhammer.ai_shimmer.test

import com.adamhammer.ai_shimmer.ShimmerBuilder
import com.adamhammer.ai_shimmer.ShimmerInstance
import com.adamhammer.ai_shimmer.adapters.StubAdapter
import com.adamhammer.ai_shimmer.annotations.*
import kotlinx.serialization.Serializable
import java.util.concurrent.Future
import kotlin.reflect.KClass

// ── Reusable test data classes ──────────────────────────────────────────────────

@Serializable
@AiSchema(title = "SimpleResult", description = "A simple test result")
data class SimpleResult(
    @AiSchema(title = "Value", description = "The value")
    val value: String = "default"
)

@Serializable
@AiSchema(title = "CounterResult", description = "A result with a count")
data class CounterResult(
    @AiSchema(title = "Count", description = "The count value")
    val count: Int = 0
)

@Serializable
@AiSchema(title = "TestColor", description = "A test color enum")
enum class TestColor { RED, GREEN, BLUE }

@Serializable
@AiSchema(title = "EnumResult", description = "A result containing an enum")
data class EnumResult(
    @AiSchema(title = "Color", description = "The selected color")
    val color: TestColor = TestColor.RED
)

// ── Reusable test API interfaces ────────────────────────────────────────────────

interface SimpleTestAPI {
    @AiOperation(summary = "Get", description = "Gets a simple result")
    @AiResponse(description = "The result", responseClass = SimpleResult::class)
    fun get(): Future<SimpleResult>

    @AiOperation(summary = "GetWithParam", description = "Gets a result with a parameter")
    @AiResponse(description = "The result", responseClass = SimpleResult::class)
    fun getWithParam(
        @AiParameter(description = "An input string") input: String
    ): Future<SimpleResult>

    @AiOperation(summary = "GetString", description = "Gets a string result")
    @AiResponse(description = "The result", responseClass = String::class)
    fun getString(): Future<String>
}

interface MemoryTestAPI {
    @AiOperation(summary = "Store", description = "Stores a value in memory")
    @AiResponse(description = "Stored result", responseClass = String::class)
    @Memorize(label = "stored-value")
    fun store(
        @AiParameter(description = "The value to store") value: String
    ): Future<String>

    @AiOperation(summary = "Recall", description = "Recalls the stored value")
    @AiResponse(description = "The recalled value", responseClass = String::class)
    @Memorize(label = "recalled-value")
    fun recall(): Future<String>
}

interface NonFutureAPI {
    @AiOperation(summary = "Bad", description = "Returns a non-Future type")
    fun badMethod(): String
}

interface SuspendTestAPI {
    @AiOperation(summary = "Get", description = "Gets a simple result")
    @AiResponse(description = "The result", responseClass = SimpleResult::class)
    suspend fun get(): SimpleResult

    @AiOperation(summary = "GetString", description = "Gets a string result")
    @AiResponse(description = "The result", responseClass = String::class)
    suspend fun getString(): String

    @AiOperation(summary = "GetWithParam", description = "Gets a result with a parameter")
    @AiResponse(description = "The result", responseClass = SimpleResult::class)
    suspend fun getWithParam(
        @AiParameter(description = "An input string") input: String
    ): SimpleResult
}

// ── Test helper functions ───────────────────────────────────────────────────────

/**
 * Create a Shimmer proxy backed by a [MockAdapter] for testing.
 *
 * ```kotlin
 * val (api, mock) = shimmerTest<MyAPI>(MockAdapter.scripted(result))
 * api.doSomething().get()
 * mock.verifyCallCount(1)
 * ```
 */
inline fun <reified T : Any> shimmerTest(mock: MockAdapter): Pair<T, MockAdapter> {
    val instance = ShimmerBuilder(T::class)
        .setAdapterDirect(mock)
        .build()
    return instance.api to mock
}

/**
 * Create a Shimmer proxy backed by a [StubAdapter] (returns default values).
 *
 * ```kotlin
 * val api = shimmerStub<MyAPI>()
 * val result = api.get().get() // returns default-constructed result
 * ```
 */
inline fun <reified T : Any> shimmerStub(): T {
    return ShimmerBuilder(T::class)
        .setAdapterClass(StubAdapter::class)
        .build()
        .api
}
