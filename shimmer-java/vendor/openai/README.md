# Shimmer OpenAI Adapter

The `shimmer-openai` module provides an `ApiAdapter` implementation for OpenAI's API. It translates Shimmer's `PromptContext` into OpenAI chat completion requests and handles native tool calling.

## Features

- **Native Tool Calling** — automatically converts Shimmer `ToolProvider` definitions into OpenAI function schemas and handles the multi-turn tool execution loop.
- **Structured Outputs** — uses OpenAI's JSON mode and structured outputs to guarantee responses match your Kotlin data classes.
- **Streaming** — supports token-by-token streaming via `Flow<String>`.
- **Configurable Models** — defaults to `gpt-4o-mini`, but can be configured for any OpenAI chat model.

## Usage

```kotlin
val instance = shimmer<MyAPI> {
    adapter(OpenAiAdapter(model = "gpt-4o"))
}
```

## Tool Calling

When tool providers are registered, the OpenAI adapter automatically:
1. Converts tool definitions to OpenAI's function-calling format
2. Sends them with the chat completion request
3. Dispatches tool calls to the appropriate provider
4. Feeds results back to the LLM
5. Repeats until the LLM produces a final response

```kotlin
val instance = shimmer<MyAPI> {
    adapter(OpenAiAdapter())
    toolProvider(myToolProvider)
}
```