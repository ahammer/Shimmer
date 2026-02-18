package com.adamhammer.shimmer.samples.dnd.model

import com.adamhammer.shimmer.annotations.AiSchema
import kotlinx.serialization.Serializable

@Serializable
@AiSchema(title = "Player", description = "A player character in the adventure")
data class Player(
    @field:AiSchema(title = "Name", description = "The player's character name")
    val name: String = "Unknown",
    @field:AiSchema(title = "Race", description = "The character's race (e.g. Human, Elf, Dwarf)")
    val race: String = "Human",
    @field:AiSchema(title = "Class", description = "The character's class (e.g. Fighter, Wizard, Rogue)")
    val characterClass: String = "Fighter",
    @field:AiSchema(title = "HP", description = "Current hit points")
    val hp: Int = 20,
    @field:AiSchema(title = "Max HP", description = "Maximum hit points")
    val maxHp: Int = 20,
    @field:AiSchema(title = "Inventory", description = "Items the player is carrying")
    val inventory: List<String> = listOf("Torch", "Rope"),
    @field:AiSchema(title = "Status", description = "Current status effects (e.g. healthy, poisoned, wounded)")
    val status: String = "healthy"
)

@Serializable
@AiSchema(title = "Location", description = "A location in the game world")
data class Location(
    @field:AiSchema(title = "Name", description = "The name of this location")
    val name: String = "The Rusty Tankard Inn",
    @field:AiSchema(title = "Description", description = "A description of the location")
    val description: String = "A dimly lit tavern with creaking floorboards and the smell of ale in the air.",
    @field:AiSchema(title = "Exits", description = "Available exits and where they lead")
    val exits: List<String> = listOf("north: Town Square", "upstairs: Guest Rooms"),
    @field:AiSchema(title = "NPCs", description = "Non-player characters present at this location")
    val npcs: List<String> = listOf("Barkeep Marta"),
    @field:AiSchema(title = "Items", description = "Visible or discoverable items at this location")
    val items: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "World", description = "The complete state of the D&D game world")
data class World(
    @field:AiSchema(title = "Player", description = "The player character")
    val player: Player = Player(),
    @field:AiSchema(title = "Location", description = "The current location")
    val location: Location = Location(),
    @field:AiSchema(title = "Turn", description = "The current turn number")
    val turn: Int = 0,
    @field:AiSchema(title = "Quest Log", description = "Active quests and objectives")
    val questLog: List<String> = emptyList()
)

// --- AI Response Types ---

@Serializable
@AiSchema(title = "SceneDescription", description = "A vivid description of the current scene")
data class SceneDescription(
    @field:AiSchema(title = "Narrative", description = "An atmospheric description of what the player sees, hears, and smells")
    val narrative: String = "",
    @field:AiSchema(title = "Available Actions", description = "Suggested actions the player could take")
    val availableActions: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "ActionResult", description = "The outcome of a player action")
data class ActionResult(
    @field:AiSchema(title = "Narrative", description = "A vivid narration of what happens as a result of the action")
    val narrative: String = "",
    @field:AiSchema(title = "Success", description = "Whether the action succeeded")
    val success: Boolean = true,
    @field:AiSchema(title = "HP Change", description = "Change to player HP (negative for damage, positive for healing, 0 for none)")
    val hpChange: Int = 0,
    @field:AiSchema(title = "Items Gained", description = "Items added to the player's inventory")
    val itemsGained: List<String> = emptyList(),
    @field:AiSchema(title = "Items Lost", description = "Items removed from the player's inventory")
    val itemsLost: List<String> = emptyList(),
    @field:AiSchema(title = "New Location", description = "If the player moved, the new location name. Empty if they stayed.")
    val newLocationName: String = "",
    @field:AiSchema(title = "New Location Description", description = "If the player moved, a description of the new location. Empty if they stayed.")
    val newLocationDescription: String = "",
    @field:AiSchema(title = "New Exits", description = "If the player moved, the exits at the new location")
    val newExits: List<String> = emptyList(),
    @field:AiSchema(title = "New NPCs", description = "If the player moved, NPCs at the new location")
    val newNpcs: List<String> = emptyList(),
    @field:AiSchema(title = "Quest Update", description = "New quest or objective to add, or empty if none")
    val questUpdate: String = "",
    @field:AiSchema(title = "Status Change", description = "New status for the player, or empty to keep current")
    val statusChange: String = ""
)
