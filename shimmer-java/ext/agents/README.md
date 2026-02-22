# Shimmer Agents

The `shimmer-agents` module provides abstractions for building multi-step and decision-making AI workflows on top of Shimmer's core proxy system.

## Features

- **`AutonomousAgent`** — a loop-driven agent that pairs a "brain" interface (`DecidingAgentAPI`) with an "action" interface. The brain decides which action to take next based on the current context.
- **`AgentDispatcher`** — routes tasks to a pool of specialized agents based on their capabilities.
- **`@Terminal`** — marks an action method as the end of an agent's turn, breaking the loop.

## Usage

Define an action interface for your agent:

```kotlin
interface PlayerAgentAPI {
    @AiOperation(description = "Look around the room")
    @Memorize("observations")
    fun observeSituation(): Future<String>

    @AiOperation(description = "Take a final action")
    @Terminal
    fun commitAction(action: String): Future<String>
}
```

Create an `AutonomousAgent` that will loop until a `@Terminal` method is called:

```kotlin
val agent = AutonomousAgent(
    name = "Player 1",
    actionApiClass = PlayerAgentAPI::class,
    adapter = OpenAiAdapter(),
    maxSteps = 5
)

// The agent will think, observe, and eventually commit an action
val result = agent.stepDetailedSuspend()
```