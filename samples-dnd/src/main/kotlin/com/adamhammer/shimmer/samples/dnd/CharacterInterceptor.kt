package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.samples.dnd.model.Character

/**
 * Interceptor that injects a specific character's identity, stats, backstory,
 * and personality into the system instructions for an AI-controlled party member.
 */
class CharacterInterceptor(private val characterProvider: () -> Character) : Interceptor {

    override fun intercept(context: PromptContext): PromptContext {
        val c = characterProvider()
        val scores = c.abilityScores
        fun mod(score: Int): String {
            val m = (score - 10) / 2
            return if (m >= 0) "+$m" else "$m"
        }
        return context.copy(
            systemInstructions = context.systemInstructions + """
                |
                |# YOUR CHARACTER
                |You ARE ${c.name}, a ${c.race} ${c.characterClass} (Level ${c.level}).
                |You must stay in character at all times. Speak and act as ${c.name} would.
                |
                |## Stats
                |STR ${scores.str} (${mod(scores.str)}) | DEX ${scores.dex} (${mod(scores.dex)}) | CON ${scores.con} (${mod(scores.con)})
                |INT ${scores.int} (${mod(scores.int)}) | WIS ${scores.wis} (${mod(scores.wis)}) | CHA ${scores.cha} (${mod(scores.cha)})
                |HP: ${c.hp}/${c.maxHp} | AC: ${c.ac} | Proficiency: +${c.proficiencyBonus}
                |Saves: ${c.savingThrows.joinToString()} | Skills: ${c.skills.joinToString()}
                |
                |## Inventory
                |${c.inventory.joinToString(", ")}
                |
                |## Backstory
                |${c.backstory}
                |
                |## Behavior Guidelines
                |- Act according to your backstory and personality.
                |- Cooperate with the party but stay true to your character.
                |- Use your class abilities and skills when appropriate.
                |- Be specific about what you do — don't be vague.
                |- Consider your inventory and equipment in your decisions.
                |Status: ${c.status}
            """.trimMargin()
        )
    }
}
