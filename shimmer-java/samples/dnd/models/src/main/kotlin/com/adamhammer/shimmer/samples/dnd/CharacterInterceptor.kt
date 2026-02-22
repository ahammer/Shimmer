package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.model.PromptContext
import com.adamhammer.shimmer.samples.dnd.model.Character

/**
 * Interceptor that injects a specific character's identity, stats, backstory,
 * and personality into the system instructions for an AI-controlled party member.
 */
class CharacterInterceptor(private val characterProvider: () -> Character) : Interceptor {

    private fun classInstincts(characterClass: String): String =
        when (characterClass.lowercase()) {
        "rogue" -> "You prefer scouting, stealth, and cunning. " +
            "Check for traps, pick locks, sneak ahead. Act before others notice."
        "cleric" -> "You protect and heal. Check on wounded allies first. " +
            "Use divine knowledge for mysteries and undead threats."
        "bard" -> "You inspire and negotiate. Talk to NPCs, use performance, " +
            "gather information socially. Charm before combat."
        "fighter" -> "You lead the charge. Position for combat, " +
            "protect the vulnerable, test physical obstacles. Action over hesitation."
        "wizard", "sorcerer" -> "You analyze and exploit. Study magical phenomena, " +
            "identify arcane threats, use spells strategically. Knowledge is power."
        "ranger" -> "You track and scout. Read the environment, identify creatures, find paths. The wilderness speaks to you."
        "paladin" -> "You uphold your oath. Protect the innocent, confront evil directly, inspire through action. Duty first."
        "warlock" -> "You bargain and manipulate. Use your patron's gifts creatively, seek hidden knowledge, embrace the shadows."
        "druid" -> "You commune with nature. Read the land, speak with animals, shape the elements. Balance above all."
        "monk" -> "You observe and strike. Move swiftly, exploit openings, remain disciplined. Patience yields precision."
        "barbarian" -> "You charge in fearlessly. Smash obstacles, intimidate foes, protect allies through sheer force."
        else -> "Use your unique abilities. Lean into what makes your class special — don't default to generic actions."
    }

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
                |## Goals
                |${c.goals.joinToString(" | ").ifBlank { "Survive and support the party." }}
                |
                |## Relationships
                |${c.relationships.entries.joinToString(" | ") { "${it.key}: ${it.value}" }.ifBlank { "No strong stances yet." }}
                |
                |## Emotional State
                |${c.emotionalState}
                |
                |## Journal (Recent)
                |${c.journal.takeLast(6).joinToString("\n") { "- $it" }.ifBlank { "- No entries yet." }}
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
                |- NEVER repeat the same action two rounds in a row.
                |- React to what changed since your last action.
                |- Keep your goals and emotional state coherent as events evolve.
                |- Explore, interact with NPCs, investigate, or fight — vary your approach.
                |- **SEEK EXITS AND TRAVEL:** Do not linger in one place. Actively look for ways to move the story forward.
                |
                |## Class Instincts (${c.characterClass})
                |- ${classInstincts(c.characterClass)}
                |
                |## Step Policy
                |- Observation has already been performed for this turn.
                |- You MAY call checkAbilities or whisper ONCE per turn.
                |- You MUST end your turn with commitAction.
                |Status: ${c.status}
            """.trimMargin()
        )
    }
}
