package com.adamhammer.shimmer.samples.dnd.model

import com.adamhammer.shimmer.annotations.AiSchema
import kotlinx.serialization.Serializable

// ── Core Character Model ────────────────────────────────────────────────────

@Serializable
@AiSchema(title = "AbilityScores", description = "The six D&D ability scores for a character")
data class AbilityScores(
    @field:AiSchema(description = "Strength — melee attacks, carrying capacity, athletics")
    val str: Int = 10,
    @field:AiSchema(description = "Dexterity — ranged attacks, AC, stealth, acrobatics")
    val dex: Int = 10,
    @field:AiSchema(description = "Constitution — hit points, endurance, concentration")
    val con: Int = 10,
    @field:AiSchema(description = "Intelligence — arcana, investigation, wizard spellcasting")
    val int: Int = 10,
    @field:AiSchema(description = "Wisdom — perception, insight, cleric spellcasting")
    val wis: Int = 10,
    @field:AiSchema(description = "Charisma — persuasion, deception, intimidation")
    val cha: Int = 10
)

@Serializable
@AiSchema(title = "Character", description = "A player character in the adventuring party")
data class Character(
    @field:AiSchema(description = "The character's name")
    val name: String = "Unknown",
    @field:AiSchema(description = "Race (Human, Elf, Dwarf, Halfling)")
    val race: String = "Human",
    @field:AiSchema(description = "Class (Fighter, Wizard, Rogue, Cleric)")
    val characterClass: String = "Fighter",
    @field:AiSchema(description = "Character level")
    val level: Int = 1,
    @field:AiSchema(description = "The six ability scores")
    val abilityScores: AbilityScores = AbilityScores(),
    @field:AiSchema(description = "Armor Class")
    val ac: Int = 10,
    @field:AiSchema(description = "Current hit points")
    val hp: Int = 10,
    @field:AiSchema(description = "Maximum hit points")
    val maxHp: Int = 10,
    @field:AiSchema(description = "Proficiency bonus")
    val proficiencyBonus: Int = 2,
    @field:AiSchema(description = "Saving throw proficiencies (e.g. STR, CON)")
    val savingThrows: List<String> = emptyList(),
    @field:AiSchema(description = "Skill proficiencies (e.g. Athletics, Stealth)")
    val skills: List<String> = emptyList(),
    @field:AiSchema(description = "Items the character is carrying")
    val inventory: List<String> = emptyList(),
    @field:AiSchema(description = "Current status effects (e.g. healthy, poisoned, wounded)")
    val status: String = "healthy",
    @field:AiSchema(description = "The character's backstory and personality")
    val backstory: String = "",
    @field:AiSchema(description = "Whether this character is controlled by a human player")
    val isHuman: Boolean = true
)

// ── World Model ─────────────────────────────────────────────────────────────

@Serializable
@AiSchema(title = "Location", description = "A location in the game world")
data class Location(
    @field:AiSchema(description = "The name of this location")
    val name: String = "The Rusty Tankard Inn",
    @field:AiSchema(description = "A description of the location")
    val description: String = "A dimly lit tavern with creaking floorboards and the smell of ale in the air.",
    @field:AiSchema(description = "Available exits and where they lead")
    val exits: List<String> = listOf("north: Town Square", "upstairs: Guest Rooms"),
    @field:AiSchema(description = "Non-player characters present at this location")
    val npcs: List<String> = listOf("Barkeep Marta"),
    @field:AiSchema(description = "Visible or discoverable items at this location")
    val items: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "World", description = "The complete state of the D&D game world")
data class World(
    @field:AiSchema(description = "The adventuring party — all player characters")
    val party: List<Character> = emptyList(),
    @field:AiSchema(description = "The current location")
    val location: Location = Location(),
    @field:AiSchema(description = "The current round number")
    val round: Int = 0,
    @field:AiSchema(description = "Active quests and objectives")
    val questLog: List<String> = emptyList(),
    @field:AiSchema(description = "Log of recent actions and events (most recent last)")
    val actionLog: List<String> = emptyList()
)

// ── AI Response Types ───────────────────────────────────────────────────────

@Serializable
@AiSchema(title = "SceneDescription", description = "A vivid description of the current scene")
data class SceneDescription(
    @field:AiSchema(
        description = "A small ASCII art illustration of the scene (3-6 lines, " +
            "using box-drawing and simple characters). Evocative, not detailed."
    )
    val asciiArt: String = "",
    @field:AiSchema(description = "An atmospheric description of what the party sees, hears, and smells")
    val narrative: String = "",
    @field:AiSchema(description = "Suggested actions the party could take")
    val availableActions: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "DiceRollRequest", description = "A request for a character to roll dice")
data class DiceRollRequest(
    @field:AiSchema(description = "Name of the character who must roll")
    val characterName: String = "",
    @field:AiSchema(description = "Type of roll (e.g. 'Strength check', 'Attack roll', 'Dexterity saving throw')")
    val rollType: String = "",
    @field:AiSchema(description = "The Difficulty Class to beat")
    val difficulty: Int = 10,
    @field:AiSchema(description = "Ability modifier to add to the roll (computed from character stats)")
    val modifier: Int = 0
)

@Serializable
@AiSchema(title = "ActionResult", description = "The outcome of a character's action")
data class ActionResult(
    @field:AiSchema(description = "A vivid narration of what happens as a result of the action")
    val narrative: String = "",
    @field:AiSchema(description = "Whether the action succeeded")
    val success: Boolean = true,
    @field:AiSchema(description = "Name of the character affected (defaults to acting character)")
    val targetCharacterName: String = "",
    @field:AiSchema(description = "Change to target character's HP (negative for damage, positive for healing, 0 for none)")
    val hpChange: Int = 0,
    @field:AiSchema(description = "Items added to the character's inventory")
    val itemsGained: List<String> = emptyList(),
    @field:AiSchema(description = "Items removed from the character's inventory")
    val itemsLost: List<String> = emptyList(),
    @field:AiSchema(description = "If the party moved, the new location name. Empty if they stayed.")
    val newLocationName: String = "",
    @field:AiSchema(description = "If the party moved, a description of the new location.")
    val newLocationDescription: String = "",
    @field:AiSchema(description = "If the party moved, the exits at the new location")
    val newExits: List<String> = emptyList(),
    @field:AiSchema(description = "If the party moved, NPCs at the new location")
    val newNpcs: List<String> = emptyList(),
    @field:AiSchema(description = "New quest or objective to add, or empty if none")
    val questUpdate: String = "",
    @field:AiSchema(description = "New status effect for the character, or empty to keep current")
    val statusChange: String = "",
    @field:AiSchema(
        description = "If a dice roll is required before this action resolves, " +
            "provide the request here. Null/default if no roll needed."
    )
    val diceRollRequest: DiceRollRequest? = null
)

@Serializable
@AiSchema(title = "CharacterConcept", description = "A randomly generated character concept for a D&D adventure")
data class CharacterConcept(
    @field:AiSchema(description = "A creative, memorable character name fitting the race and setting")
    val name: String = "",
    @field:AiSchema(description = "The character's race (e.g. Human, Elf, Dwarf, Halfling, Gnome, Tiefling, Dragonborn, Half-Orc)")
    val race: String = "",
    @field:AiSchema(description = "The character's class (e.g. Fighter, Wizard, Rogue, Cleric, Ranger, Bard, Paladin, Warlock)")
    val characterClass: String = ""
)

@Serializable
@AiSchema(title = "BackstoryResult", description = "An AI-generated backstory with suggested stats for a character")
data class BackstoryResult(
    @field:AiSchema(description = "A rich 2-3 paragraph backstory for the character")
    val backstory: String = "",
    @field:AiSchema(description = "Suggested ability scores tailored to the character's race and class")
    val suggestedAbilityScores: AbilityScores = AbilityScores(),
    @field:AiSchema(description = "Starting equipment appropriate for the character")
    val startingItems: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "PlayerAction", description = "An AI-controlled character's chosen action")
data class PlayerAction(
    @field:AiSchema(
        description = "A short, specific player command in first person " +
            "(1-2 sentences max, e.g. 'I attack the goblin'). NOT narration or scene description."
    )
    val action: String = "",
    @field:AiSchema(description = "Brief internal reasoning for why this action was chosen")
    val reasoning: String = ""
)
