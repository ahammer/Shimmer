package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.annotations.*
import com.adamhammer.shimmer.samples.dnd.model.ActionResult
import com.adamhammer.shimmer.samples.dnd.model.BackstoryResult
import com.adamhammer.shimmer.samples.dnd.model.CharacterConcept
import com.adamhammer.shimmer.samples.dnd.model.SceneDescription
import java.util.concurrent.Future

/**
 * The Dungeon Master AI interface. Each method represents a DM action
 * that the AI will narrate and resolve for a multi-player party.
 */
interface DungeonMasterAPI {

    @AiOperation(
        summary = "Describe Scene",
        description = "Describe the current scene to the adventuring party. Style: Dark Fantasy / BBS RPG. " +
                "Be vivid and atmospheric. Include sensory details — sights, sounds, smells. Address the party as a group. " +
                "Suggest possible actions for the party members in the style of a text adventure game. " +
                "Include a cool BBS-style ASCII art illustration (5-10 lines) of the scene using block characters (█ ▓ ▒ ░). " +
                "NEVER repeat a previous scene description — always advance the narrative."
    )
    @AiResponse(
        description = "A vivid scene description with BBS-style ASCII art and suggested actions",
        responseClass = SceneDescription::class
    )
    @Memorize(label = "Last scene description")
    fun describeScene(): Future<SceneDescription>

    @AiOperation(
        summary = "Process Character Action",
        description = "A party member has taken an action. Narrate what happens. " +
                "Consider the character's ability scores, class, skills, and equipment when determining outcomes. " +
                "If the action requires a skill check, ability check, attack roll, or saving throw, " +
                "set diceRollRequest with the appropriate rollType, difficulty (DC), and modifier " +
                "(calculated from the character's stats + proficiency if applicable). " +
                "DO NOT resolve the action fully if a dice roll is needed — instead describe the attempt " +
                "and request the roll. The action will be resolved after the roll. " +
                "When you include diceRollRequest, describe only the attempt and immediate setup, not the final success/failure outcome. " +
                "Do not apply HP, inventory, quest, or status consequences until resolveRoll. " +
                "For actions that don't need a roll, resolve them directly with appropriate consequences: " +
                "HP changes (keep them small, -1 to -5 for minor, -5 to -10 for serious), " +
                "item gains/losses, location changes, quest updates. " +
                "You may introduce NPCs dynamically using newNpcs and newNpcProfiles when the story naturally warrants it. " +
                "Be creative but fair. The world should feel alive and reactive."
    )
    @AiResponse(
        description = "The result of the action, possibly including a dice roll request",
        responseClass = ActionResult::class
    )
    @Memorize(label = "Last action result")
    fun processAction(
        @AiParameter(description = "Name of the character performing the action")
        characterName: String,
        @AiParameter(description = "What the character wants to do")
        action: String
    ): Future<ActionResult>

    @AiOperation(
        summary = "Resolve Dice Roll",
        description = "A dice roll has been made. The 'success' parameter tells you definitively " +
                "whether the roll succeeded — you MUST narrate a success if success=true and " +
                "a failure if success=false. Do NOT contradict the success/failure outcome. " +
                "Be dramatic — describe near-misses, critical successes (natural 20), " +
                "and critical failures (natural 1) with extra flair. " +
                "Apply appropriate consequences based on the outcome."
    )
    @AiResponse(
        description = "The narrative outcome of the dice roll with state changes",
        responseClass = ActionResult::class
    )
    @Memorize(label = "Last roll result")
    fun resolveRoll(
        @AiParameter(description = "Name of the character who rolled")
        characterName: String,
        @AiParameter(description = "Type of roll (e.g. 'Strength check', 'Attack roll')")
        rollType: String,
        @AiParameter(description = "The raw d20 value rolled (1-20)")
        rollValue: Int,
        @AiParameter(description = "The ability modifier added to the roll")
        modifier: Int,
        @AiParameter(description = "The Difficulty Class to beat")
        difficulty: Int,
        @AiParameter(description = "The total of rollValue + modifier")
        total: Int,
        @AiParameter(description = "Whether the roll succeeded (total >= difficulty)")
        success: Boolean
    ): Future<ActionResult>

    @AiOperation(
        summary = "Generate Character Concept",
        description = "Generate a creative and interesting character concept for a D&D adventure. " +
                "Choose an evocative name, an interesting race, and a fitting class. " +
                "Be creative — go beyond the standard choices. Make each character unique and memorable. " +
                "IMPORTANT: The concept MUST be different from existing party members. " +
                "Avoid duplicate names, races, and classes to create a balanced, diverse party. " +
                "If a partial concept is provided (e.g. just a name, or just a race), " +
                "fill in only the missing fields while keeping the provided ones."
    )
    @AiResponse(
        description = "A character concept with name, race, and class — different from existing party members",
        responseClass = CharacterConcept::class
    )
    fun generateCharacterConcept(
        @AiParameter(
            description = "Partial concept hint and party context. " +
                "Format: 'EXISTING PARTY: [list]. GENERATE: [fields needed]. HINT: [user input]'"
        )
        hint: String
    ): Future<CharacterConcept>

    @AiOperation(
        summary = "Generate Character Backstory",
        description = "Generate a rich, compelling backstory for a new character joining the adventure. " +
                "The backstory should be 2-3 paragraphs, fitting the character's race and class. " +
                "Also suggest appropriate ability scores (using values from the standard array: " +
                "15, 14, 13, 12, 10, 8 distributed across STR/DEX/CON/INT/WIS/CHA based on class needs) " +
                "and starting equipment appropriate for a level 1 character of this class."
    )
    @AiResponse(
        description = "A backstory with suggested ability scores and starting items",
        responseClass = BackstoryResult::class
    )
    fun generateBackstory(
        @AiParameter(description = "The character's name")
        name: String,
        @AiParameter(description = "The character's race")
        race: String,
        @AiParameter(description = "The character's class")
        characterClass: String
    ): Future<BackstoryResult>

    @AiOperation(
        summary = "Narrate Round Summary",
        description = "All party members have acted this round. Summarize what happened, " +
                "describe how the environment reacts to the party's collective actions, " +
                "and set the stage for the next round. Mention each character by name. " +
                "Build tension, advance the story, and hint at what lies ahead. " +
                "Include a cool BBS-style ASCII art illustration (5-10 lines) of the evolving scene using block characters (█ ▓ ▒ ░). " +
                "CRITICAL: You are a great DM. Advance the PLOT every round. " +
                "Introduce new NPCs, threats, discoveries, or twists. " +
                "If players are repeating the same actions, the world should react — " +
                "interrupt with events, encounters, or consequences. " +
                "A good DM never lets the story stagnate. " +
                "NEVER repeat previous scene descriptions verbatim."
    )
    @AiResponse(
        description = "A summary advancing the plot, with ASCII art and new suggested actions",
        responseClass = SceneDescription::class
    )
    @Memorize(label = "Last round summary")
    fun narrateRoundSummary(): Future<SceneDescription>
}
