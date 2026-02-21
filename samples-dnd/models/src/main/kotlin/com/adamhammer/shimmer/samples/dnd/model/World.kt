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
    @field:AiSchema(description = "A physical description of the character's appearance and style")
    val look: String = "",
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
    @field:AiSchema(description = "Current personal goals, both short and long term")
    val goals: List<String> = emptyList(),
    @field:AiSchema(description = "Relationship stances toward other characters and notable NPCs")
    val relationships: Map<String, String> = emptyMap(),
    @field:AiSchema(description = "Current emotional state and motivation")
    val emotionalState: String = "focused",
    @field:AiSchema(description = "Personal journal entries authored by this character")
    val journal: List<String> = emptyList()
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
    @field:AiSchema(description = "Maximum number of rounds in this session")
    val maxRounds: Int = 40,
    @field:AiSchema(description = "Number of consecutive rounds spent at the current location")
    val turnsAtCurrentLocation: Int = 0,
    @field:AiSchema(description = "Persistent world lore and backstory scaffolding")
    val lore: WorldLore = WorldLore(),
    @field:AiSchema(description = "Active quests and objectives")
    val questLog: List<String> = emptyList(),
    @field:AiSchema(description = "Log of recent actions and events (most recent last)")
    val actionLog: List<String> = emptyList(),
    @field:AiSchema(description = "Private whispers between party members")
    val whisperLog: List<WhisperMessage> = emptyList()
)

@Serializable
@AiSchema(title = "WorldLore", description = "Background world scaffolding created before the game starts")
data class WorldLore(
    @field:AiSchema(description = "High-level campaign premise")
    val campaignPremise: String = "",
    @field:AiSchema(description = "Known locations and their role in the story")
    val locations: List<LoreLocation> = emptyList(),
    @field:AiSchema(description = "NPC registry with motivations")
    val npcs: List<NpcProfile> = emptyList(),
    @field:AiSchema(description = "Plot hooks to seed early turns")
    val plotHooks: List<String> = emptyList(),
    @field:AiSchema(description = "Factions and influential groups in the setting")
    val factions: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "LoreLocation", description = "A location node used in world backstory planning")
data class LoreLocation(
    @field:AiSchema(description = "Location name")
    val name: String = "",
    @field:AiSchema(description = "Short location description")
    val description: String = "",
    @field:AiSchema(description = "Connected location names")
    val connections: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "NpcProfile", description = "An NPC with role and motivation")
data class NpcProfile(
    @field:AiSchema(description = "NPC name")
    val name: String = "",
    @field:AiSchema(description = "Narrative role")
    val role: String = "",
    @field:AiSchema(description = "Current motivation")
    val motivation: String = "",
    @field:AiSchema(description = "Current location")
    val currentLocation: String = ""
)

@Serializable
@AiSchema(title = "NpcRegistryResult", description = "Structured NPC registry generated during world setup")
data class NpcRegistryResult(
    @field:AiSchema(description = "NPC profiles created for this campaign")
    val npcs: List<NpcProfile> = emptyList()
)

@Serializable
@AiSchema(title = "WorldBuildResult", description = "Committed world setup used to start the game")
data class WorldBuildResult(
    @field:AiSchema(description = "Campaign premise for this run")
    val campaignPremise: String = "",
    @field:AiSchema(description = "Pre-game world lore")
    val lore: WorldLore = WorldLore(),
    @field:AiSchema(description = "Opening scene after world setup")
    val openingScene: SceneDescription = SceneDescription(),
    @field:AiSchema(description = "Suggested starting location")
    val startingLocation: Location = Location(),
    @field:AiSchema(description = "Initial quest hooks")
    val initialQuests: List<String> = emptyList()
)

@Serializable
@AiSchema(title = "WhisperMessage", description = "A private in-character message between party members")
data class WhisperMessage(
    @field:AiSchema(description = "Character sending the whisper")
    val from: String = "",
    @field:AiSchema(description = "Character receiving the whisper")
    val to: String = "",
    @field:AiSchema(description = "Short private message content")
    val message: String = "",
    @field:AiSchema(description = "Round when this whisper occurred")
    val round: Int = 0
)

// ── AI Response Types ───────────────────────────────────────────────────────

@Serializable
@AiSchema(title = "PartyEffect", description = "An effect applied to a specific character during the round summary")
data class PartyEffect(
    @field:AiSchema(description = "Name of the affected character")
    val characterName: String = "",
    @field:AiSchema(description = "HP change (negative for damage, positive for healing, 0 for none)")
    val hpChange: Int = 0,
    @field:AiSchema(description = "New status effect, or empty to keep current")
    val statusChange: String = ""
)

@Serializable
@AiSchema(title = "RoundSummaryResult", description = "The DM's round summary with narrative AND world mutations")
data class RoundSummaryResult(
    @field:AiSchema(description = "An atmospheric summary of what happened this round")
    val narrative: String = "",
    @field:AiSchema(description = "Suggested actions the party could take next round")
    val availableActions: List<String> = emptyList(),
    @field:AiSchema(
        description = "A short world event the DM injects " +
            "(e.g. 'The shadowy figures attack!' or 'A messenger arrives with urgent news'). Empty if none."
    )
    val worldEvent: String = "",
    @field:AiSchema(description = "If the DM moves the party, the new location name. Empty if they stay.")
    val newLocationName: String = "",
    @field:AiSchema(description = "If the DM moves the party, describe the new location.")
    val newLocationDescription: String = "",
    @field:AiSchema(description = "If the party moved, exits at the new location")
    val newExits: List<String> = emptyList(),
    @field:AiSchema(description = "NPCs present at the new or current location after this round")
    val newNpcs: List<String> = emptyList(),
    @field:AiSchema(description = "Full NPC profiles for any NPCs introduced this round")
    val newNpcProfiles: List<NpcProfile> = emptyList(),
    @field:AiSchema(description = "New quest or objective to add, or empty if none")
    val questUpdate: String = "",
    @field:AiSchema(description = "Effects applied to party members this round (ambient damage, status changes, healing)")
    val partyEffects: List<PartyEffect> = emptyList()
)

@Serializable
@AiSchema(
    title = "RoundOutcomeCandidate",
    description = "One possible round outcome with engagement score and category"
)
data class RoundOutcomeCandidate(
    @field:AiSchema(description = "An atmospheric summary of what happened this round")
    val narrative: String = "",
    @field:AiSchema(description = "Suggested actions the party could take next round")
    val availableActions: List<String> = emptyList(),
    @field:AiSchema(description = "A short world event the DM injects. Empty if none.")
    val worldEvent: String = "",
    @field:AiSchema(description = "If the DM moves the party, the new location name. Empty if they stay.")
    val newLocationName: String = "",
    @field:AiSchema(description = "If the DM moves the party, describe the new location.")
    val newLocationDescription: String = "",
    @field:AiSchema(description = "If the party moved, exits at the new location")
    val newExits: List<String> = emptyList(),
    @field:AiSchema(description = "NPCs present at the new or current location after this round")
    val newNpcs: List<String> = emptyList(),
    @field:AiSchema(description = "Full NPC profiles for any NPCs introduced this round")
    val newNpcProfiles: List<NpcProfile> = emptyList(),
    @field:AiSchema(description = "New quest or objective to add, or empty if none")
    val questUpdate: String = "",
    @field:AiSchema(description = "Effects applied to party members this round")
    val partyEffects: List<PartyEffect> = emptyList(),
    @field:AiSchema(description = "Engagement score 1-10 (10 = most engaging)")
    val engagementScore: Int = 5,
    @field:AiSchema(description = "Category: combat, social, exploration, or dramatic_twist")
    val category: String = ""
)

@Serializable
@AiSchema(
    title = "RoundOutcomeProposals",
    description = "Four diverse round outcome candidates proposed by the DM"
)
data class RoundOutcomeProposals(
    @field:AiSchema(description = "Exactly 4 diverse outcome candidates")
    val candidates: List<RoundOutcomeCandidate> = emptyList()
)

@Serializable
@AiSchema(
    title = "ActionOutcomeCandidate",
    description = "One possible action outcome with engagement score and category"
)
data class ActionOutcomeCandidate(
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
    @field:AiSchema(description = "Optional full NPC profiles introduced by this action")
    val newNpcProfiles: List<NpcProfile> = emptyList(),
    @field:AiSchema(description = "New quest or objective to add, or empty if none")
    val questUpdate: String = "",
    @field:AiSchema(description = "New status effect for the character, or empty to keep current")
    val statusChange: String = "",
    @field:AiSchema(
        description = "If a dice roll is required before this action resolves, " +
            "provide the request here. Null/default if no roll needed."
    )
    val diceRollRequest: DiceRollRequest? = null,
    @field:AiSchema(description = "Engagement score 1-10 (10 = most engaging)")
    val engagementScore: Int = 5,
    @field:AiSchema(description = "Category: combat, social, exploration, or dramatic_twist")
    val category: String = ""
)

@Serializable
@AiSchema(
    title = "ActionOutcomeProposals",
    description = "Four diverse action outcome candidates proposed by the DM"
)
data class ActionOutcomeProposals(
    @field:AiSchema(description = "Exactly 4 diverse outcome candidates")
    val candidates: List<ActionOutcomeCandidate> = emptyList()
)

@Serializable
@AiSchema(title = "SceneDescription", description = "A vivid description of the current scene")
data class SceneDescription(
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
    @field:AiSchema(description = "Optional full NPC profiles introduced by this action")
    val newNpcProfiles: List<NpcProfile> = emptyList(),
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
    @field:AiSchema(description = "A physical description of the character's appearance and style")
    val look: String = "",
    @field:AiSchema(
        description = "The character's race " +
            "(e.g. Human, Elf, Dwarf, Halfling, Gnome, Tiefling, Dragonborn, Half-Orc)"
    )
    val race: String = "",
    @field:AiSchema(description = "The character's class (e.g. Fighter, Wizard, Rogue, Cleric, Ranger, Bard, Paladin, Warlock)")
    val characterClass: String = ""
)

@Serializable
@AiSchema(title = "BackstoryResult", description = "An AI-generated backstory with suggested stats for a character")
data class BackstoryResult(
    @field:AiSchema(description = "A rich 2-3 paragraph backstory for the character")
    val backstory: String = "",
    @field:AiSchema(description = "A physical description of the character's appearance and style")
    val look: String = "",
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
    val reasoning: String = "",
    @field:AiSchema(description = "Optional journal entry to persist this turn")
    val journalEntry: String = "",
    @field:AiSchema(description = "Optional updated emotional state for the character")
    val emotionalUpdate: String = "",
    @field:AiSchema(description = "Optional new or revised personal goal")
    val goalUpdate: String = "",
    @field:AiSchema(description = "Optional character to whisper to this turn")
    val whisperTarget: String = "",
    @field:AiSchema(description = "Optional whisper message sent to whisperTarget")
    val whisperMessage: String = ""
)
