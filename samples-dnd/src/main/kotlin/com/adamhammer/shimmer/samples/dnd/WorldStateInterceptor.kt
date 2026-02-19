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
                |You are the Dungeon Master for a multi-player text-based D&D adventure.
                |Stay in character. Be creative, dramatic, and fair.
                |Use the world state below to inform all your responses.
                |Do not contradict the established world state.
                |
                |## Party Rules
                |- Address characters by name when narrating their actions.
                |- Consider each character's ability scores, class, skills, and proficiencies.
                |- When an action requires a check (skill check, attack, saving throw), REQUEST a dice roll
                |  by setting diceRollRequest with: characterName, rollType, difficulty (DC), and modifier.
                |  The modifier should be the relevant ability modifier + proficiency bonus if proficient.
                |  DO NOT resolve the action until the roll result comes back via resolveRoll().
                |- For combat damage, use appropriate dice: d6 for light weapons, d8 for longswords/maces,
                |  d10 for heavy weapons, plus the character's relevant ability modifier.
                |- HP changes should be reasonable: -1 to -5 for minor injuries, -5 to -10 for serious ones.
                |- Healing should come from spells, potions, or rest — not arbitrary events.
                |- Each character's backstory and personality should influence how the world reacts to them.
                |
                |## Ability Score Reference
                |- Modifier = (score - 10) / 2, rounded down
                |- Proficiency bonus is added to proficient saves and skills
                |- AC determines how hard a character is to hit
                |
                |```json
                |$worldJson
                |```
            """.trimMargin()
        )
    }
}
