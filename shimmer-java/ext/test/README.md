# Shimmer Test

The `shimmer-test` module provides everything you need for offline testing of Shimmer interfaces and prompt pipelines.

## MockAdapter

Use `MockAdapter` to script responses or dynamically generate them based on the `PromptContext`.

```kotlin
// Scripted responses
val mock = MockAdapter.scripted(result1, result2)
val (api, mock) = shimmerTest<MyAPI>(mock)
api.get().get()
mock.verifyCallCount(1)
mock.lastContext!!.assertSystemInstructionsContain("specialized AI")

// Dynamic responses
val mock = MockAdapter.dynamic { context, resultClass ->
    if (resultClass == String::class) "dynamic" else MyResult("computed")
}

// Builder for complex scenarios
val mock = MockAdapter.builder()
    .responses(result1, result2)
    .delayMs(500)           // simulate latency
    .failOnCall(0)          // throw on first call
    .build()
```

## Test Helpers

```kotlin
// Quick setup with MockAdapter
val (api, mock) = shimmerTest<MyAPI>(MockAdapter.scripted(result))

// Quick setup with StubAdapter (returns defaults)
val api = shimmerStub<MyAPI>()
```

## Prompt Assertions

Verify that your `ContextBuilder` and `Interceptor`s are assembling the prompt correctly:

```kotlin
mock.lastContext!!
    .assertSystemInstructionsContain("specialized AI")
    .assertMethodInvocationContains("greet")
    .assertMemoryContains("key", "value")
    .assertMemoryEmpty()
    .assertPropertyEquals("key", expectedValue)
    .assertHasTools("calculator", "search")
    .assertToolCount(2)
```

## MockToolProvider

Test tool-calling loops without hitting real external services:

```kotlin
val mockTools = MockToolProvider.builder()
    .tool("calculator", "Performs math", """{"type":"object","properties":{"expr":{"type":"string"}},"required":["expr"]}""")
    .handler("calculator") { call -> ToolResult(call.id, call.toolName, "42") }
    .build()

val instance = shimmer<MyAPI> {
    adapter(mock)
    toolProvider(mockTools)
}

// After calls:
mockTools.verifyCallCount(1)
mockTools.lastCall  // most recent ToolCall
```