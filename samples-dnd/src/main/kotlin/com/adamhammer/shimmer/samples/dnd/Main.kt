package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.adapters.OpenAiAdapter
import com.adamhammer.shimmer.shimmer
import com.adamhammer.shimmer.samples.dnd.model.*
import java.util.Scanner

/**
 * A text-based D&D adventure powered by Shimmer.
 *
 * The AI serves as the Dungeon Master — narrating scenes, resolving actions,
 * and making the world react to the player's choices. A WorldStateInterceptor
 * keeps the AI aware of full game state on every call, and a ResiliencePolicy
 * validates results and retries if the AI returns something unreasonable.
 */
fun main() {
    val scanner = Scanner(System.`in`)

    println("═══════════════════════════════════════════════════")
    println("       ⚔  SHIMMER QUEST — A Text D&D Adventure  ⚔")
    println("═══════════════════════════════════════════════════")
    println()

    // --- Character creation ---
    print("Enter your character's name: ")
    val name = scanner.nextLine().ifBlank { "Adventurer" }

    print("Choose a race (Human/Elf/Dwarf/Halfling): ")
    val race = scanner.nextLine().ifBlank { "Human" }

    print("Choose a class (Fighter/Wizard/Rogue/Cleric): ")
    val characterClass = scanner.nextLine().ifBlank { "Fighter" }

    var world = World(
        player = Player(
            name = name,
            race = race,
            characterClass = characterClass,
            hp = 20,
            maxHp = 20,
            inventory = listOf("Torch", "Rope", startingItem(characterClass))
        ),
        location = Location(),
        turn = 0
    )

    println()
    println("Welcome, $name the $race $characterClass!")
    println("Your adventure begins...")
    println()

    // --- Build the Shimmer-powered DM ---
    val dm = shimmer<DungeonMasterAPI> {
        setAdapterClass(OpenAiAdapter::class)
        addInterceptor(WorldStateInterceptor { world })
        resilience {
            maxRetries = 2
            retryDelayMs = 500
            resultValidator = { result ->
                when (result) {
                    is ActionResult -> result.narrative.isNotBlank() && result.hpChange in -15..15
                    is SceneDescription -> result.narrative.isNotBlank()
                    else -> true
                }
            }
        }
    }.api

    // --- Opening scene ---
    val scene = dm.describeScene().get()
    printScene(scene, world)

    // --- Game loop ---
    while (world.player.hp > 0) {
        world = world.copy(turn = world.turn + 1)

        print("\n> What do you do? ")
        val input = scanner.nextLine().trim()

        if (input.equals("quit", ignoreCase = true) || input.equals("exit", ignoreCase = true)) {
            println("\nYou lay down your arms. The adventure ends here.")
            println("Final stats: HP ${world.player.hp}/${world.player.maxHp} | " +
                    "Inventory: ${world.player.inventory.joinToString()}")
            break
        }

        if (input.equals("status", ignoreCase = true)) {
            printStatus(world)
            continue
        }

        if (input.equals("look", ignoreCase = true)) {
            val lookScene = dm.describeScene().get()
            printScene(lookScene, world)
            continue
        }

        // Process the player's action
        val result = dm.processAction(input).get()
        world = applyResult(world, result)

        println()
        println("─── DM ───────────────────────────────────────────")
        println(result.narrative)
        if (result.hpChange != 0) {
            val sign = if (result.hpChange > 0) "+" else ""
            println("  ♥ HP: $sign${result.hpChange} (${world.player.hp}/${world.player.maxHp})")
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

        if (world.player.hp <= 0) {
            println()
            println("💀 You have fallen. Your adventure ends here.")
            println("Game Over after ${world.turn} turns.")
        }
    }
}

private fun startingItem(characterClass: String): String = when (characterClass.lowercase()) {
    "wizard" -> "Spellbook"
    "rogue" -> "Lockpicks"
    "cleric" -> "Holy Symbol"
    "fighter" -> "Longsword"
    else -> "Dagger"
}

private fun printScene(scene: SceneDescription, world: World) {
    println()
    println("═══ ${world.location.name} ═══")
    println(scene.narrative)
    if (scene.availableActions.isNotEmpty()) {
        println()
        println("Possible actions: ${scene.availableActions.joinToString(" | ")}")
    }
    println()
    println("(Type 'status' for stats, 'look' to re-examine, 'quit' to end)")
}

private fun printStatus(world: World) {
    val p = world.player
    println()
    println("═══ ${p.name} the ${p.race} ${p.characterClass} ═══")
    println("  HP: ${p.hp}/${p.maxHp}  |  Status: ${p.status}")
    println("  Inventory: ${p.inventory.joinToString()}")
    println("  Location: ${world.location.name}")
    if (world.questLog.isNotEmpty()) {
        println("  Quests: ${world.questLog.joinToString("; ")}")
    }
    println("  Turn: ${world.turn}")
}

private fun applyResult(world: World, result: ActionResult): World {
    val player = world.player
    val newHp = (player.hp + result.hpChange).coerceIn(0, player.maxHp)
    val newInventory = (player.inventory + result.itemsGained) - result.itemsLost.toSet()
    val newStatus = result.statusChange.ifBlank { player.status }

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
        player = player.copy(
            hp = newHp,
            inventory = newInventory,
            status = newStatus
        ),
        location = newLocation,
        questLog = newQuestLog
    )
}
