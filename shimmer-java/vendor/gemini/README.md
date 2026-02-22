# Shimmer Gemini Adapter

The `shimmer-gemini` module provides an `ApiAdapter` implementation for Google's Gemini API. It translates Shimmer's `PromptContext` into Gemini content generation requests.

## Features

- **Native Tool Calling** — automatically converts Shimmer `ToolProvider` definitions into Gemini function declarations and handles the multi-turn tool execution loop.
- **Structured Outputs** — uses Gemini's JSON schema support to guarantee responses match your Kotlin data classes.
- **Streaming** — supports token-by-token streaming via `Flow<String>`.
- **Configurable Models** — defaults to `gemini-2.5-flash`, but can be configured for any Gemini model.

## Usage

```kotlin
val instance = shimmer<MyAPI> {
    adapter(GeminiAdapter(model = "gemini-2.5-pro"))
}
```