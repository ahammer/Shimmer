package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.samples.dnd.model.World
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Interceptor that injects the current world state into the system instructions
 * so the AI Dungeon Master always knows the full game context.
 */
class WorldStateInterceptor(private val worldProvider: () -> World) : Interceptor {

    private val json = Json { prettyPrint = true }

    override fun intercept(context: PromptContext): PromptContext {
        val worldJson = json.encodeToString(worldProvider())
        return context.copy(
            systemInstructions = context.systemInstructions + """
                |
                |# CURRENT WORLD STATE
                |You are the Dungeon Master for a text-based D&D adventure.
                |Stay in character. Be creative, dramatic, and fair.
                |Use the world state below to inform all your responses.
                |Do not contradict the established world state.
                |
                |```json
                |$worldJson
                |```
            """.trimMargin()
        )
    }
}
