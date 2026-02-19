package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.adapters.OpenAiAdapter
import com.adamhammer.shimmer.agents.AutonomousAgent
import com.adamhammer.shimmer.agents.DecidingAgentAPI
import com.adamhammer.shimmer.samples.dnd.model.*
import com.adamhammer.shimmer.shimmer
import java.util.Scanner
import kotlin.random.Random

/**
 * A multi-player text-based D&D adventure powered by Shimmer.
 *
 * Features:
 * - 1–4 player party with configurable human/AI members
 * - Full D&D ability scores, AC, proficiency, saving throws
 * - AI-generated backstories for AI companions
 * - Dice rolling system (DM requests rolls, players input values)
 * - AI party members use AutonomousAgent from shimmer-agents
 * - Round-based gameplay with DM narration between rounds
 */
fun main() {
    val scanner = Scanner(System.`in`)

    println("═══════════════════════════════════════════════════════")
    println("  ⚔  SHIMMER QUEST — A Multi-Player D&D Adventure  ⚔")
    println("═══════════════════════════════════════════════════════")
    println()

    val world = createParty(scanner)
    runGameLoop(scanner, world)
}

// ── Party Creation ──────────────────────────────────────────────────────────

private fun createParty(scanner: Scanner): World {
    print("How many party members? (1-4): ")
    val partySize = scanner.nextLine().trim()
        .toIntOrNull()?.coerceIn(1, 4) ?: 2
    println()

    val backstoryDm = shimmer<DungeonMasterAPI> {
        setAdapterClass(OpenAiAdapter::class)
        resilience { maxRetries = 1 }
    }.api

    val party = mutableListOf<Character>()

    for (i in 1..partySize) {
        val character = createCharacter(scanner, i, partySize, backstoryDm, party)
        party.add(character)
        println()
        println(CharacterUtils.formatStats(character))
        println()
    }

    return World(party = party, location = Location(), round = 0)
}

private fun createCharacter(
    scanner: Scanner,
    index: Int,
    total: Int,
    backstoryDm: DungeonMasterAPI,
    existingParty: List<Character> = emptyList()
): Character {
    println("─── Character $index of $total ────────────────────────")
    println("  (Leave any field blank to auto-generate)")

    print("  Name: ")
    val inputName = scanner.nextLine().trim()

    print("  Race (Human/Elf/Dwarf/Halfling/...): ")
    val inputRace = scanner.nextLine().trim()

    print("  Class (Fighter/Wizard/Rogue/Cleric/...): ")
    val inputClass = scanner.nextLine().trim()

    val (name, race, characterClass) = if (
        inputName.isBlank() || inputRace.isBlank() || inputClass.isBlank()
    ) {
        val existingMembers = existingParty.joinToString(", ") {
            "${it.name} the ${it.race} ${it.characterClass}"
        }.ifEmpty { "none yet" }
        val hints = buildString {
            if (inputName.isNotBlank()) append("name=$inputName ")
            if (inputRace.isNotBlank()) append("race=$inputRace ")
            if (inputClass.isNotBlank()) append("class=$inputClass")
        }.trim()
        val prompt = "EXISTING PARTY: [$existingMembers]. " +
            "GENERATE: ${if (hints.isBlank()) "name, race, class" else hints}. " +
            "HINT: ${hints.ifBlank { "surprise me" }}"
        println("  Generating character concept...")
        val concept = backstoryDm.generateCharacterConcept(prompt).get()
        val n = inputName.ifBlank { concept.name }.ifBlank { "Hero$index" }
        val r = inputRace.ifBlank { concept.race }.ifBlank { "Human" }
        val c = inputClass.ifBlank { concept.characterClass }.ifBlank { "Fighter" }
        println("  → $n the $r $c")
        Triple(n, r, c)
    } else {
        Triple(inputName, inputRace, inputClass)
    }

    print("  Controlled by (h)uman or (a)i? [h]: ")
    val isHuman = !scanner.nextLine().trim().lowercase().startsWith("a")

    val (backstory, abilityScores, startingItems) = resolveBackstoryAndStats(
        scanner, backstoryDm, name, race, characterClass, isHuman
    )

    return CharacterUtils.buildCharacter(
        name, race, characterClass, abilityScores, backstory, isHuman, startingItems
    )
}

private fun resolveBackstoryAndStats(
    scanner: Scanner,
    backstoryDm: DungeonMasterAPI,
    name: String,
    race: String,
    characterClass: String,
    isHuman: Boolean
): Triple<String, AbilityScores, List<String>> {
    if (!isHuman) {
        println("  Generating backstory for AI companion $name...")
        val result = backstoryDm
            .generateBackstory(name, race, characterClass).get()
        println("  ${result.backstory.take(200)}...")
        println()
        return Triple(result.backstory, result.suggestedAbilityScores, result.startingItems)
    }

    println("  Write a backstory (or Enter for AI-generated):")
    print("  > ")
    val typed = scanner.nextLine().trim()
    if (typed.isNotBlank()) {
        return Triple(typed, assignAbilityScores(scanner, characterClass), emptyList())
    }
    println("  Generating backstory for $name the $race $characterClass...")
    val result = backstoryDm
        .generateBackstory(name, race, characterClass).get()
    println("  ${result.backstory.take(200)}...")
    println()
    return Triple(result.backstory, assignAbilityScores(scanner, characterClass), result.startingItems)
}

// ── Game Loop ───────────────────────────────────────────────────────────────

private fun runGameLoop(scanner: Scanner, initialWorld: World) {
    var world = initialWorld

    println("═══════════════════════════════════════════════════════")
    println("  Party of ${world.party.size} sets forth on an adventure!")
    println("═══════════════════════════════════════════════════════")
    println()

    val dm = shimmer<DungeonMasterAPI> {
        setAdapterClass(OpenAiAdapter::class)
        addInterceptor(WorldStateInterceptor { world })
        resilience {
            maxRetries = 2
            retryDelayMs = 500
            resultValidator = { r ->
                when (r) {
                    is ActionResult -> r.narrative.isNotBlank()
                        && r.hpChange in -15..15
                    is SceneDescription -> r.narrative.isNotBlank()
                    else -> true
                }
            }
        }
    }.api

    val aiAgents = buildAiAgents(world)

    val openingScene = dm.describeScene().get()
    printScene(openingScene, world)

    while (world.party.any { it.hp > 0 }) {
        world = world.copy(round = world.round + 1)
        println()
        println("══════════════ Round ${world.round} ══════════════")

        world = processRound(scanner, dm, world, aiAgents)

        if (world.party.all { it.hp <= 0 }) {
            println()
            println("💀💀💀 Total Party Kill! 💀💀💀")
            println("Game Over after ${world.round} rounds.")
            printParty(world)
            return
        }

        println()
        val summary = dm.narrateRoundSummary().get()
        printRoundSummary(summary, world)
    }
}

private fun processRound(
    scanner: Scanner,
    dm: DungeonMasterAPI,
    initialWorld: World,
    aiAgents: Map<String, AutonomousAgent<PlayerAgentAPI>>
): World {
    var world = initialWorld

    for (character in world.party) {
        if (character.hp <= 0) {
            println("\n  💀 ${character.name} is unconscious.")
            continue
        }

        val action = resolveCharacterAction(
            scanner, dm, character, aiAgents, world
        ) ?: return world // quit command

        val result = dm.processAction(character.name, action).get()
        val finalResult = handleDiceRoll(
            scanner, dm, character, result
        )

        world = applyResult(world, finalResult, character.name)
        val logEntry = "Round ${world.round}: ${character.name} — $action → " +
            if (finalResult.success) "success" else "failure"
        world = world.copy(
            actionLog = (world.actionLog + logEntry).takeLast(20)
        )
        printActionResult(finalResult, world)
    }
    return world
}

private fun resolveCharacterAction(
    scanner: Scanner,
    dm: DungeonMasterAPI,
    character: Character,
    aiAgents: Map<String, AutonomousAgent<PlayerAgentAPI>>,
    world: World
): String? {
    if (!character.isHuman) {
        return resolveAiAction(character, aiAgents)
    }
    return resolveHumanAction(scanner, dm, character, aiAgents, world)
}

private fun resolveAiAction(
    character: Character,
    aiAgents: Map<String, AutonomousAgent<PlayerAgentAPI>>
): String {
    println("\n  🤖 ${character.name} is thinking...")
    val agent = aiAgents[character.name]
    if (agent != null) {
        try {
            val decisionResult = agent.step()
            val action = parseAiAction(decisionResult, character.name)
            println("  🤖 ${character.name}: \"$action\"")
            return action
        } catch (e: Exception) {
            println("  ⚠ ${character.name}'s AI stumbled: ${e.message}")
        }
    }
    val fallback = "I look around cautiously."
    println("  🤖 ${character.name}: \"$fallback\"")
    return fallback
}

private fun resolveHumanAction(
    scanner: Scanner,
    dm: DungeonMasterAPI,
    character: Character,
    aiAgents: Map<String, AutonomousAgent<PlayerAgentAPI>>,
    world: World
): String? {
    print("\n> ${character.name}, what do you do? ")
    val input = scanner.nextLine().trim()

    when {
        input.equals("quit", true) || input.equals("exit", true) -> {
            println("\nThe party lays down their arms.")
            printParty(world)
            return null
        }
        input.equals("status", true) -> {
            printParty(world)
            return resolveCharacterAction(
                scanner, dm, character, aiAgents, world
            )
        }
        input.equals("look", true) -> {
            val scene = dm.describeScene().get()
            printScene(scene, world)
            return resolveCharacterAction(
                scanner, dm, character, aiAgents, world
            )
        }
        else -> return input
    }
}

private fun handleDiceRoll(
    scanner: Scanner,
    dm: DungeonMasterAPI,
    character: Character,
    result: ActionResult
): ActionResult {
    val roll = result.diceRollRequest ?: return result

    val diceValue = if (character.isHuman) {
        println()
        println(buildString {
            append("  🎲 ${roll.characterName}: ${roll.rollType}")
            append(" (DC ${roll.difficulty}, ${formatMod(roll.modifier)})")
        })
        print("  🎲 Enter your d20 roll (1-20): ")
        scanner.nextLine().trim().toIntOrNull()
            ?.coerceIn(1, 20) ?: Random.nextInt(1, 21)
    } else {
        val rolled = Random.nextInt(1, 21)
        println("  🎲 ${roll.characterName} rolls a d20: $rolled")
        rolled
    }

    val total = diceValue + roll.modifier
    val outcome = if (total >= roll.difficulty) "SUCCESS!" else "FAILURE!"
    println(
        "  🎲 Roll: $diceValue ${formatMod(roll.modifier)}" +
            " = $total vs DC ${roll.difficulty} → $outcome"
    )

    val success = total >= roll.difficulty

    return dm.resolveRoll(
        roll.characterName,
        roll.rollType,
        diceValue,
        roll.modifier,
        roll.difficulty,
        total,
        success
    ).get()
}

// ── AI Agent Builder ────────────────────────────────────────────────────────

private fun buildAiAgents(
    world: World
): Map<String, AutonomousAgent<PlayerAgentAPI>> {
    val agents = mutableMapOf<String, AutonomousAgent<PlayerAgentAPI>>()

    for (character in world.party.filter { !it.isHuman }) {
        val playerInstance = shimmer<PlayerAgentAPI> {
            setAdapterClass(OpenAiAdapter::class)
            addInterceptor(WorldStateInterceptor { world })
            addInterceptor(CharacterInterceptor {
                world.party.first { it.name == character.name }
            })
            resilience { maxRetries = 1 }
        }

        val decider = shimmer<DecidingAgentAPI> {
            setAdapterClass(OpenAiAdapter::class)
            addInterceptor(WorldStateInterceptor { world })
            addInterceptor(CharacterInterceptor {
                world.party.first { it.name == character.name }
            })
            resilience { maxRetries = 1 }
        }.api

        agents[character.name] = AutonomousAgent(playerInstance, decider)
    }

    return agents
}

// ── Ability Score Assignment ────────────────────────────────────────────────

private fun assignAbilityScores(
    scanner: Scanner,
    characterClass: String
): AbilityScores {
    println()
    val scores = CharacterUtils.standardArray.joinToString()
    println("  Ability scores — standard array: $scores")
    println("  (a)uto-assign based on class, or (m)anual? [a]: ")
    print("  > ")
    val choice = scanner.nextLine().trim().lowercase()

    if (choice.startsWith("m")) {
        return manualAssignScores(scanner)
    }
    return CharacterUtils.autoAssignScores(characterClass)
}

private fun manualAssignScores(scanner: Scanner): AbilityScores {
    val remaining = CharacterUtils.standardArray.toMutableList()
    val assigned = mutableMapOf<String, Int>()
    for (ability in CharacterUtils.abilityNames) {
        println("  Remaining: ${remaining.joinToString()}")
        print("  $ability = ")
        val value = scanner.nextLine().trim().toIntOrNull()
        if (value != null && value in remaining) {
            remaining.remove(value)
            assigned[ability] = value
        } else {
            val auto = remaining.removeFirst()
            assigned[ability] = auto
            println("  (assigned $auto)")
        }
    }
    return AbilityScores(
        str = assigned["STR"] ?: 10,
        dex = assigned["DEX"] ?: 10,
        con = assigned["CON"] ?: 10,
        int = assigned["INT"] ?: 10,
        wis = assigned["WIS"] ?: 10,
        cha = assigned["CHA"] ?: 10
    )
}

// ── Result Application ──────────────────────────────────────────────────────

private fun applyResult(
    world: World,
    result: ActionResult,
    actingCharacterName: String
): World {
    val targetName = result.targetCharacterName.ifBlank {
        actingCharacterName
    }

    val newParty = world.party.map { c ->
        if (c.name.equals(targetName, ignoreCase = true)) {
            val newHp = (c.hp + result.hpChange)
                .coerceIn(0, c.maxHp)
            val newInv = (c.inventory + result.itemsGained) -
                result.itemsLost.toSet()
            val newStatus = result.statusChange.ifBlank { c.status }
            c.copy(hp = newHp, inventory = newInv, status = newStatus)
        } else {
            c
        }
    }

    val newLocation = if (result.newLocationName.isNotBlank()) {
        Location(
            name = result.newLocationName,
            description = result.newLocationDescription,
            exits = result.newExits,
            npcs = result.newNpcs
        )
    } else {
        world.location
    }

    val newQuestLog = if (result.questUpdate.isNotBlank()) {
        world.questLog + result.questUpdate
    } else {
        world.questLog
    }

    return world.copy(
        party = newParty,
        location = newLocation,
        questLog = newQuestLog
    )
}

// ── Display Helpers ─────────────────────────────────────────────────────────

private fun printScene(scene: SceneDescription, world: World) {
    println()
    println("═══ ${world.location.name} ═══")
    if (scene.asciiArt.isNotBlank()) {
        println(scene.asciiArt)
        println()
    }
    println(scene.narrative)
    if (scene.availableActions.isNotEmpty()) {
        println()
        val actions = scene.availableActions.joinToString(" | ")
        println("Possible actions: $actions")
    }
    println()
    println("Commands: 'status' | 'look' | 'quit'")
}

private fun printActionResult(result: ActionResult, world: World) {
    println()
    println("─── DM ───────────────────────────────────────────")
    println(result.narrative)
    val target = result.targetCharacterName.ifBlank { "Unknown" }
    val targetChar = world.party.find {
        it.name.equals(target, ignoreCase = true)
    }
    if (result.hpChange != 0 && targetChar != null) {
        val sign = if (result.hpChange > 0) "+" else ""
        println("  ♥ ${targetChar.name} HP: " +
            "$sign${result.hpChange} (${targetChar.hp}/${targetChar.maxHp})")
    }
    if (result.itemsGained.isNotEmpty()) {
        println("  + Gained: ${result.itemsGained.joinToString()}")
    }
    if (result.itemsLost.isNotEmpty()) {
        println("  - Lost: ${result.itemsLost.joinToString()}")
    }
    if (result.questUpdate.isNotBlank()) {
        println("  📜 Quest: ${result.questUpdate}")
    }
    println("──────────────────────────────────────────────────")
}

private fun printRoundSummary(
    summary: SceneDescription,
    world: World
) {
    println("─── End of Round ${world.round} ─────────────────────")
    if (summary.asciiArt.isNotBlank()) {
        println(summary.asciiArt)
        println()
    }
    println(summary.narrative)
    if (summary.availableActions.isNotEmpty()) {
        val actions = summary.availableActions.joinToString(" | ")
        println("Suggested actions: $actions")
    }
    println("──────────────────────────────────────────────────")
}

private fun printParty(world: World) {
    println()
    println("═══ Party — ${world.location.name} (Round ${world.round}) ═══")
    for (c in world.party) {
        println(CharacterUtils.formatStats(c))
    }
    if (world.questLog.isNotEmpty()) {
        println("  📜 Quests: ${world.questLog.joinToString("; ")}")
    }
}

private fun formatMod(mod: Int): String =
    if (mod >= 0) "+$mod" else "$mod"

private fun parseAiAction(
    stepResult: String,
    characterName: String
): String {
    // Try JSON format: "action": "..."
    val jsonPattern = Regex(""""action"\s*:\s*"([^"]+)"""")
    jsonPattern.find(stepResult)?.groupValues?.get(1)?.let { return it }

    // Try Kotlin toString format: PlayerAction(action=..., reasoning=...)
    val toStringPattern = Regex("""action=([^,)]+)""")
    toStringPattern.find(stepResult)?.groupValues?.get(1)
        ?.trim()?.let { return it }

    return stepResult.take(200).ifBlank {
        "$characterName looks around cautiously."
    }
}
