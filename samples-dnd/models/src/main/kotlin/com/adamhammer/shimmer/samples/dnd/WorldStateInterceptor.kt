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
class WorldStateInterceptor(private val isDm: Boolean = true, private val worldProvider: () -> World) : Interceptor {

    private val json = Json { prettyPrint = true }

    private fun escalationGuidance(round: Int, maxRounds: Int): String {
        val progress = if (maxRounds > 0) round.toFloat() / maxRounds else 1f
        val pct = (progress * 100).toInt()
        return when {
            progress <= 0.2f -> "- SETUP ($pct%): Establish the scene. " +
                "Introduce the setting, key NPCs, and immediate hooks. Let the players explore."

            progress <= 0.4f -> "- RISING ACTION ($pct%): COMPLICATE. " +
                "Introduce a new threat, NPC arrival, moral dilemma, or unexpected twist. " +
                "The status quo MUST change this round."

            progress <= 0.6f -> "- CONFRONTATION ($pct%): ESCALATE. " +
                "Force a confrontation, reveal a secret, or trigger combat. " +
                "NPCs act on their own agendas. " +
                "The party CANNOT stand still — the world moves whether they act or not."

            progress <= 0.8f -> "- CLIMAX ($pct%): CRISIS. Major stakes. " +
                "Enemies attack, secrets are exposed, alliances break. " +
                "This is the turning point — make it dramatic. " +
                "Use worldEvent and partyEffects to force action."

            else -> "- RESOLUTION ($pct%): CONCLUDE. " +
                "Drive toward an ending. Wrap up encounters, reveal consequences, " +
                "deliver the final outcome. " +
                "The story MUST reach a resolution before rounds run out."
        }
    }

    /**
     * Detect stagnation by analyzing the action log for repetitive character actions.
     * Returns a pair of (stagnation warning text, character action patterns text).
     */
    private fun analyzeActionPatterns(world: World): Pair<String, String> {
        val characterActions = parseCharacterActions(world.actionLog)
        val patternsBuilder = StringBuilder()
        val stagnatingCharacters = mutableListOf<Pair<String, Int>>()

        for ((name, actions) in characterActions) {
            val recent = actions.takeLast(5)
            if (recent.size < 2) continue
            val repeatCount = countConsecutiveRepeats(recent.map { it.second })
            val label = repeatLabel(repeatCount)
            patternsBuilder.appendLine(
                "- $name: ${recent.joinToString(" → ") { "R${it.first}: ${it.second.take(60)}" }} ($label)"
            )
            if (repeatCount >= 3) stagnatingCharacters.add(name to repeatCount)
        }

        val patternsSection = patternsBuilder.toString().trimEnd().ifBlank { "- No patterns yet." }
        val stagnationWarning = buildStagnationWarning(stagnatingCharacters)
        return stagnationWarning to patternsSection
    }

    private fun parseCharacterActions(actionLog: List<String>): Map<String, List<Pair<Int, String>>> {
        val result = mutableMapOf<String, MutableList<Pair<Int, String>>>()
        val actionPattern = Regex("""Round (\d+): (.+?) — (.+?) →""")
        for (entry in actionLog) {
            val match = actionPattern.find(entry) ?: continue
            val round = match.groupValues[1].toIntOrNull() ?: continue
            val name = match.groupValues[2].trim()
            val action = match.groupValues[3].trim()
            result.getOrPut(name) { mutableListOf() }.add(round to action)
        }
        return result
    }

    private fun countConsecutiveRepeats(actions: List<String>): Int {
        var count = 1
        for (i in actions.size - 1 downTo 1) {
            if (actionsAreSimilar(actions[i], actions[i - 1])) count++ else break
        }
        return count
    }

    private fun repeatLabel(repeatCount: Int): String = when {
        repeatCount >= 3 -> "⚠ REPEATING x$repeatCount"
        repeatCount >= 2 -> "similar x$repeatCount"
        else -> "varied"
    }

    private fun buildStagnationWarning(
        stagnatingCharacters: List<Pair<String, Int>>
    ): String {
        if (stagnatingCharacters.isEmpty()) return ""
        val names = stagnatingCharacters.joinToString(", ") {
            "${it.first} (${it.second} rounds)"
        }
        return """
            |
            |## ⚠ STAGNATION WARNING
            |The following characters have been repeating similar actions: $names.
            |As DM, you MUST intervene NOW:
            |- Force a scene change (set newLocationName).
            |- Trigger combat or an ambush (use partyEffects with HP damage and worldEvent).
            |- Have an NPC interrupt with urgent news (use newNpcs/newNpcProfiles).
            |- Create a crisis that makes the current approach impossible (use worldEvent).
            |DO NOT simply describe "rising tension" again. MAKE something happen.
        """.trimMargin()
    }

    private fun actionsAreSimilar(a: String, b: String): Boolean {
        val wordsA = a.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val wordsB = b.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false
        val intersection = wordsA.intersect(wordsB)
        val smaller = minOf(wordsA.size, wordsB.size)
        return intersection.size.toFloat() / smaller >= 0.5f
    }

    override fun intercept(context: PromptContext): PromptContext {
        val world = worldProvider()
        val worldJson = json.encodeToString(world)
        val lore = world.lore
        
        val dmInstructions = if (isDm) {
            val (stagnationWarning, actionPatterns) = analyzeActionPatterns(world)
            """
            |You are the Dungeon Master for a multi-player text-based D&D adventure.
            |Stay in character. Be creative, dramatic, and fair.
            |Use the world state below to inform all your responses.
            |Do not contradict the established world state.
            |
            |## DM Philosophy
            |- You are an EXCELLENT Dungeon Master. Your job is to make the game ENGAGING.
            |- Drive the plot forward every round. Introduce new NPCs, threats, mysteries, and twists.
            |- If players repeat the same action, the world REACTS — crowds get bored, enemies attack,
            |  the environment changes, or something unexpected interrupts them.
            |- NEVER repeat a previous scene description. Each round should feel different.
            |- Build a coherent story arc with rising tension, climax, and resolution.
            |- Use the action log below to track what has happened and avoid repetition.
            |- Agents have an internal turn budget of 5 request-steps; reward clear planning and concise decisions.
            |- Private whispers may occur between characters; treat them as valid tactical coordination.
            |- You have FULL POWER to change the world: move the party, trigger events, introduce NPCs,
            |  apply damage/healing/status effects, and update quests. USE these powers actively.
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
            |- You may introduce NPCs dynamically using newNpcs and newNpcProfiles when appropriate.
            |
            |## Ability Score Reference
            |- Modifier = (score - 10) / 2, rounded down
            |- Proficiency bonus is added to proficient saves and skills
            |- AC determines how hard a character is to hit
            |
            |## Narrative Escalation (Round ${world.round}/${world.maxRounds})
            |${escalationGuidance(world.round, world.maxRounds)}
            |
            |## Character Action Patterns
            |$actionPatterns
            |$stagnationWarning
            |
            """.trimMargin()
        } else {
            """
            |You are a player character in a multi-player text-based D&D adventure.
            |Use the world state below to inform your actions and understand your surroundings.
            |
            """.trimMargin()
        }

        return context.copy(
            systemInstructions = context.systemInstructions + """
                |
                |# CURRENT WORLD STATE
                |$dmInstructions
                |## Campaign Lore Summary
                |- Premise: ${lore.campaignPremise.ifBlank { "Not established yet." }}
                |- Plot Hooks: ${lore.plotHooks.take(4).joinToString(" | ").ifBlank { "None yet" }}
                |- Factions: ${lore.factions.take(4).joinToString(" | ").ifBlank { "None yet" }}
                |- Key NPCs: ${lore.npcs.take(5).joinToString(" | ") { "${it.name} (${it.role})" }.ifBlank { "None yet" }}
                |
                |## Whisper Log (Recent)
                |${world.whisperLog.takeLast(8).joinToString("\n") { "- R${it.round} ${it.from} -> ${it.to}: ${it.message}" }.ifBlank { "- None yet" }}
                |
                |```json
                |$worldJson
                |```
            """.trimMargin()
        )
    }
}
