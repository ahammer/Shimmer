# Shimmer — TODO

Items identified from a full codebase review. Phase 1 cleanup (encapsulation, bugs, hygiene) is committed. Everything below remains.

---

## High Priority

### 1. Use OpenAI Structured Output (`response_format: json_schema`)
The adapter builds prompts asking the AI to "return JSON matching this schema" then parses free-text with regex-based `extractJson()` (first `{` to last `}`). OpenAI's API supports `response_format: json_schema` for guaranteed-valid structured output. `KClass.toJsonSchema()` already exists in `Extensions.kt` but is never called by the adapter. This would eliminate the entire class of deserialization failures.

- **Where:** `OpenAiAdapter.handleRequestInternal()`
- **What:** When `resultClass != String::class`, set `response_format` to `json_schema` using the output of `resultClass.toJsonSchema()`, and remove the `extractJson()` fallback heuristic.

### 2. Extract & Deduplicate Resilience Logic
`Shimmer.kt` contains two near-identical ~40-line retry loops: `executeWithResilience()` (blocking, `Thread.sleep`) and `executeSuspendWithResilience()` (suspend, `delay`). They will drift over time.

- **Option A:** Go coroutine-first — make the canonical implementation a `suspend` function, and have the blocking path call `runBlocking`.
- **Option B:** Extract a shared `ResilienceExecutor` that accepts a `sleep` strategy and a `call` strategy as lambdas.

### 3. Decompose the `Shimmer` Class
`Shimmer.kt` (~240 lines) is the InvocationHandler, resilience engine, rate limiter orchestrator, listener notifier, and memory manager. The `Future<T>` path wraps in `CompletableFuture.supplyAsync` in `invoke()` and again in `executeWithTimeout()` — double-async wrapping wastes threads.

- Extract resilience into `ResilienceExecutor`
- Extract listener notification into a helper
- Move timeout wrapping out of the hot path (use a single async boundary)

---

## Medium Priority

### 4. Coroutine-Friendly Rate Limiter & Concurrency Semaphore
`TokenBucketRateLimiter` uses `Object.wait()`/`notifyAll()` which blocks threads. `acquireRateLimit()` is called before `withContext(Dispatchers.IO)` on the suspend path, meaning it blocks the caller's dispatcher. The concurrency semaphore (`java.util.concurrent.Semaphore`) has the same issue.

- Replace `TokenBucketRateLimiter` with a suspend-compatible implementation (e.g., `kotlinx.coroutines.sync.Semaphore` or a `Mutex`-based design).
- Provide separate blocking/suspend acquire paths, or go coroutine-first.

### 5. O(1) Tool Dispatch
`OpenAiAdapter.dispatchToolCall()` iterates all providers and calls `listTools()` on each one for every tool call to match by name. Pre-build a `Map<String, ToolProvider>` at the start of `handleRequestInternal()`.

- **Where:** `OpenAiAdapter.handleRequestInternal()`, `dispatchToolCall()`
- **What:** `val toolIndex = toolProviders.flatMap { p -> p.listTools().map { it.name to p } }.toMap()` then look up `toolIndex[call.toolName]`.

### 6. Streaming + Tool Calling Gap
`handleRequestStreaming()` ignores tool providers. The streaming path in `Shimmer.kt` injects tools into the context but calls `adapter.handleRequestStreaming()` which has no tool-calling overload.

- Add `handleRequestStreaming(context, toolProviders)` to `ApiAdapter` with a default that delegates to the no-tool version.
- Implement in `OpenAiAdapter` using OpenAI's streaming + tool-calling support.

### 7. Avoid Leaking `java.lang.reflect.Method` in `ShimmerRequest`
`ShimmerRequest` exposes `val method: Method` — a Java reflection type in the core model. Custom `ContextBuilder` implementations must work with raw reflection.

- Introduce a `MethodDescriptor` abstraction (method name, annotations, parameter names/types/descriptions, return type).
- `DefaultContextBuilder` and other consumers should work with this instead of `Method`.

### 8. Typed Properties on `PromptContext`
`properties: Map<String, Any>` is an untyped bag. Interceptors put arbitrary data in, consumers cast blindly. Consider a typed-key mechanism like Ktor's `Attributes` or a `TypedKey<T>` pattern.

---

## Low Priority / Polish

### 9. Generalize the Agent Module
- `AutonomousAIApi` hardcodes a 5-step pipeline (understand → analyze → plan → reflect → act). This is one specific agent pattern, not a general abstraction.
- `DecidingAgentAPI` only returns `Future<AiDecision>` — no `suspend` support.
- `AgentDispatcher` uses reflective `Method.invoke` on the proxy, fragile with parameter name matching.
- Consider making the pipeline steps configurable, or providing it as an example rather than a core abstraction.

### 10. `-parameters` Flag Dependency
Both `AgentDispatcher.resolveArguments()` and `ShimmerMcpServer.resolveMethodArguments()` rely on `param.name` matching JSON keys. This requires the `-parameters` / `javaParameters = true` compiler flags. If a consumer doesn't set these, parameter names become `arg0`, `arg1` and nothing matches.

- Add a runtime check / clear error message when parameter names look synthetic.
- Or fall back to positional matching when names don't match.

### 11. Package Naming Convention
`com.adamhammer.ai_shimmer` uses underscores, which is unconventional in Kotlin/JVM and explicitly suppressed in `detekt.yml` (`PackageNaming: active: false`). Standard would be `com.adamhammer.shimmer` or `com.adamhammer.aishimmer`. This is a breaking change, so only do it before a public release.

### 12. Remove Vestigial Generic on `Shimmer<T>`
`Shimmer<T : Any>` never uses `T` internally — it's only stored as `klass: KClass<T>`. The generic parameter serves no purpose on the `InvocationHandler`. Consider using `KClass<*>` instead.

### 13. `StubAdapter` Collection Support
`StubAdapter` handles primitives, enums, and data classes with all-default constructors, but fails on `List`, `Map`, `Set`, or any collection return type. Add support for common collection types.

### 14. `MockAdapter` Streaming Timing
`MockAdapter.handleRequestStreaming()` applies `delayMs` before returning the `Flow`, not during collection. For timeout testing, the delay should happen inside the flow's `collect`. Wrap in `flow { delay(delayMs); emit(response) }`.

---

## Done (Phase 1)

- [x] Fix `ShimmerInstance` encapsulation — hide `_memory`, expose `mutableMemory()` for internal use
- [x] Remove buggy `AutonomousAgent` secondary constructor (isolated empty memory)
- [x] Delete stale `shimmer-mcp/src/main/java/` directory
- [x] Fix `TokenBucketRateLimiter` unnecessary `notifyAll()` on acquire
- [x] Tighten detekt: re-enable `SwallowedException`, lower cyclomatic complexity threshold
- [x] Add `KClass.toJsonSchema()` for proper JSON Schema generation
