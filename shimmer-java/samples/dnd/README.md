# Shimmer D&D Case Study

The `samples-dnd` module is a complete text-based Dungeons & Dragons adventure demonstrating Shimmer's advanced AI patterns. It uses the AI as both the Dungeon Master (DM) and the Player Characters (PCs), showcasing how to build complex, multi-agent, stateful AI applications.

## Running the Sample

Requires an `OPENAI_API_KEY` environment variable.

```bash
# Run the Compose Desktop UI
./gradlew :samples-dnd:compose:run

# Or run the CLI version
./gradlew :samples-dnd:cli:run --console=plain
```

## AI Architecture

The sample leverages five key Shimmer features to create a cohesive game loop:

### 1. Shimmer Agents (`AutonomousAgent`)

The game uses an `AutonomousAgent` for each player character to simulate independent decision-making.

- It pairs a **`PlayerAgentAPI`** (which defines available actions like `observeSituation`, `checkAbilities`, and the `@Terminal` action `commitAction`) with a **`DecidingAgentAPI`**.
- The `DecidingAgentAPI` acts as the "brain," dynamically choosing which method in the `PlayerAgentAPI` to call next based on the current context.
- The agent is given a strict turn budget (`AGENT_TURN_BUDGET = 5`). It loops through steps using `agent.stepDetailedSuspend()` until it decides to call the terminal `commitAction` or runs out of budget (triggering a fallback commit).

### 2. Interceptors (`WorldStateInterceptor`)

Interceptors are used to dynamically inject real-time game state into the AI's system instructions before every prompt:

- **`WorldStateInterceptor`**: Serializes the current `World` state to JSON and injects it. For the DM, it also analyzes the action log to inject pacing guidance and stagnation warnings if players are repeating actions.
- **`CharacterInterceptor`**: Injects the specific character's stats, backstory, and inventory so the player agent knows who it is playing.
- **`TurnStateInterceptor`**: Injects the agent's current turn budget, phase, and recent steps so the AI understands where it is in its decision loop.

### 3. ResiliencePolicy for Validation

The `resilience { ... }` block is used during agent construction to handle transient errors and enforce business logic on the AI's structured outputs:

- It configures basic retry logic (`maxRetries`, `retryDelayMs`).
- It uses a **`resultValidator`** lambda to ensure the AI's output makes sense. For example, the DM's validator checks that an `ActionResult` has a non-blank narrative and that HP changes are within a reasonable bound (`-15..15`). It also ensures that proposal lists contain at least 2 valid candidates.

### 4. Memory (`@Memorize`)

The `@Memorize` annotation is used on interface methods to automatically persist the AI's intermediate thoughts across its multi-step turn:

- In `PlayerAgentAPI`, methods like `observeSituation()` and `checkAbilities()` are annotated with `@Memorize(label = "Current observations")` and `@Memorize(label = "Capability assessment")`.
- This allows the agent to "remember" its observations and tactical assessments when it finally calls `commitAction`, without needing to manually pass those strings as parameters.

### 5. Tool Calling

The project uses the `ToolProvider` interface to give agents access to deterministic game data:

- **`PlayerToolProvider`** defines tools like `inspect_inventory` and `inspect_skills` using `ToolDefinition` schemas.
- When the AI decides it needs to check its gear or stats, it invokes the tool. The `callTool` method intercepts this, reads the actual `Character` object, calculates modifiers, and returns a `ToolResult`.
- This is registered to the player agents via `toolProvider(toolProvider)`, allowing them to query their exact mechanical state dynamically rather than relying on hallucinated stats.