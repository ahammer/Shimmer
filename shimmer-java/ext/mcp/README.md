# Shimmer MCP

The `shimmer-mcp` module provides full [Model Context Protocol](https://modelcontextprotocol.io/) integration — both as a client (consuming tools from MCP servers) and as a server (exposing Shimmer interfaces as MCP tools).

## MCP Client — Consuming MCP Server Tools

Use `McpToolProvider` to connect to an MCP server and make its tools available to the LLM:

```kotlin
// Connect to an MCP server via stdio
val transport = StdioClientTransport(
    ServerParameters.builder("npx")
        .args("-y", "@modelcontextprotocol/server-everything")
        .build()
)

val mcpTools = McpToolProvider(transport)
mcpTools.connect()

// Wire MCP tools into Shimmer
val instance = shimmer<MyAPI> {
    adapter(OpenAiAdapter())
    toolProvider(mcpTools)
}

// The LLM can now call MCP tools during generation
val result = instance.api.doSomething().get()

// Clean up
mcpTools.close()
```

## MCP Server — Exposing Shimmer Interfaces

Use `ShimmerMcpServer` to expose annotated Kotlin interfaces as MCP tools:

```kotlin
// Your implementation of the annotated interface
class MyApiImpl : MyApi {
    override fun echo(message: String): Future<EchoResult> {
        return CompletableFuture.completedFuture(EchoResult(message))
    }
}

// Expose as an MCP server
val server = ShimmerMcpServer.builder()
    .transportProvider(StdioServerTransportProvider(objectMapper))
    .expose(MyApiImpl(), MyApi::class)
    .serverInfo("my-server", "1.0.0")
    .build()

// The server is now running — external MCP clients (Claude Desktop, etc.)
// can discover and call your tools
```

Shimmer automatically generates MCP tool definitions from your `@AiOperation`, `@AiParameter`, and `@AiSchema` annotations.