# AiApiShimmer

AiApiShimmer is a Kotlin library that provides a clean, annotation-based approach to interfacing with AI APIs. It follows a design pattern similar to Retrofit, where you define interfaces with annotated methods, and the library handles the implementation details of making API calls and processing responses.

## Features

- **Interface-Driven Design**: Define your AI interactions through simple interfaces
- **Annotation-Based Metadata**: Use annotations to provide rich context to the AI
- **Multiple Adapters**: Support for different AI providers (currently OpenAI)
- **Memory System**: Store and retrieve results from previous calls
- **Agent Patterns**: Build complex AI workflows with simple or decision-making agents
- **Type-Safe Responses**: Get strongly-typed responses from AI calls
- **Asynchronous API**: All operations return Futures for non-blocking execution

## Installation

Add the dependency to your project:

```gradle
dependencies {
    implementation 'com.adamhammer:ai-api-shimmer:1.0.0'
}
```

## Quick Start

### 1. Define your API interface

```kotlin
interface QuestionAPI {
    @AiOperation(
        summary = "Ask",
        description = "Provide an in-depth answer to the question within its context."
    )
    @AiResponse(
        description = "The answer to the question",
        responseClass = Answer::class
    )
    fun askStruct(
        @AiParameter(description = "The question and its context for the API call")
        question: Question?
    ): Future<Answer?>

    @AiOperation(
        summary = "AskString",
        description = "Provide an in-depth answer to the question within its context, returning a string response."
    )
    @AiResponse(
        description = "The answer as a string",
        responseClass = String::class
    )
    @Memorize("The last answer to the question.")
    fun askString(
        @AiParameter(description = "The question and its context for the API call")
        question: Question?
    ): Future<String?>
}
```

### 2. Define your data classes

```kotlin
@Serializable
@AiSchema(title = "Question", description = "Holds info about the question")
class Question(
    @field:AiSchema(title = "Question", description = "The question to be asked")
    val question: String = "",
    @field:AiSchema(title = "Context", description = "Who is asking the Question")
    val context: String = ""
)

@Serializable
@AiSchema(title = "The Answer", description = "Holds the answer to the question.")
class Answer(
    @field:AiSchema(title = "Answer", description = "A resoundingly deep answer to the question")
    val answer: String = ""
)
```

### 3. Create an instance of your API

```kotlin
// Set your OpenAI API key as an environment variable
System.setProperty("OPENAI_API_KEY", "your-api-key")

// Create the API instance
val api = ShimmerBuilder(QuestionAPI::class)
    .setAdapterClass(OpenAiAdapter::class)
    .build().api

// Use the API
val question = Question("What is the meaning of life?", "A curious student")
val answer = api.askStruct(question).get()
println(answer?.answer)
```

## Working with Agents

AiApiShimmer supports building AI agents that can orchestrate sequences of API calls:

### Simple Agent with Fixed Steps

```kotlin
class SimpleAgent(private val api: SimpleAIApi) {
    fun ideate(input: String): IdeationResult {
        // Step 1: Generate initial ideas from the provided input
        api.initiate(input).get()

        // Step 2: Expand on the generated ideas
        api.expand().get()

        // Step 3: Generate the final markdown report
        val reportContent = api.report().get()
        val finalIdea = Idea(content = reportContent)
        return IdeationResult(idea = finalIdea)
    }
}
```

### Decision-Making Agent

```kotlin
// Create the agent and decider APIs
val agentAdapter = ShimmerBuilder(AutonomousAIApi::class)
    .setAdapterClass(OpenAiAdapter::class)
    .build()

val deciderAdapter = ShimmerBuilder(DecidingAgentAPI::class)
    .setAdapterClass(OpenAiAdapter::class)
    .build()

val agent_api = agentAdapter.api
val deciding_api = deciderAdapter.api

// Let the decider choose the next action
val decision = deciding_api.decide(agentAdapter).get()
```

## Annotations

AiApiShimmer uses annotations to provide metadata to the AI:

- `@AiOperation`: Describes an API operation
- `@AiParameter`: Describes a parameter
- `@AiResponse`: Describes the expected response
- `@AiSchema`: Provides metadata for data structures
- `@Memorize`: Indicates results that should be cached
- `@Subscribe`, `@Publish`: For pub/sub patterns

## Adapters

AiApiShimmer supports multiple adapters for different AI providers:

- `OpenAiAdapter`: Sends requests to OpenAI's API
- `StubAdapter`: Simple implementation for testing

You can create your own adapters by implementing the `ApiAdapter` interface.

## Advanced Usage

### Working with Enums

```kotlin
@Serializable
enum class CardRank {
    UNDEFINED, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE
}

@Serializable
enum class CardSuit {
    UNDEFINED, HEARTS, DIAMONDS, CLUBS, SPADES
}

@Serializable
data class HigherCardResponse(
    val rank: CardRank = CardRank.UNDEFINED,
    val suit: CardSuit = CardSuit.UNDEFINED
)

interface HigherCardAPI {
    fun drawHigherCard(value: Int, suit: CardSuit): Future<HigherCardResponse>
}
```

### Using Memory

```kotlin
interface MemoryAPI {
    @Memorize("user-input")
    fun storeInput(input: String): Future<String>
    
    fun retrieveWithContext(): Future<String>
}

// The memory is passed to each request
val api = ShimmerBuilder(MemoryAPI::class)
    .setAdapterClass(OpenAiAdapter::class)
    .build().api

api.storeInput("Remember this information").get()
val result = api.retrieveWithContext().get() // Has access to the stored memory
```

## Requirements

- Java 8+
- Kotlin 1.8+
- kotlinx.serialization
- OpenAI Java client (for OpenAiAdapter)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
