@file:Suppress("MaxLineLength")
package ca.adamhammer.shimmer.samples.dnd

import ca.adamhammer.shimmer.annotations.*
import ca.adamhammer.shimmer.samples.dnd.model.ActionOutcomeProposals
import ca.adamhammer.shimmer.samples.dnd.model.ActionResult
import ca.adamhammer.shimmer.samples.dnd.model.BackstoryResult
import ca.adamhammer.shimmer.samples.dnd.model.CharacterConcept
import ca.adamhammer.shimmer.samples.dnd.model.RoundOutcomeProposals
import ca.adamhammer.shimmer.samples.dnd.model.SceneDescription
import ca.adamhammer.shimmer.model.ImageResult
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
                "NEVER repeat a previous scene description — always advance the narrative."
    )
    @AiResponse(
        description = "A vivid scene description with suggested actions",
        responseClass = SceneDescription::class
    )
    @Memorize(label = "Last scene description")
    fun describeScene(): Future<SceneDescription>

    @AiOperation(
        summary = "Propose Action Outcomes",
        description = "A party member has taken an action. Generate EXACTLY 4 diverse outcome candidates. " +
                "Each candidate MUST be a different variation of the attempted action (e.g., 'Standard Success', 'Success with Complication', 'Failure with Consequence', and 'Critical Twist'). " +
                "Rate each candidate's engagementScore from 1-10 (10 = most exciting/engaging). " +
                "Consider the character's ability scores, class, skills, and equipment when determining outcomes. " +
                "If the action requires a skill check, ability check, attack roll, or saving throw, " +
                "set diceRollRequest with the appropriate rollType, difficulty (DC), and modifier " +
                "(calculated from the character's stats + proficiency if applicable). " +
                "DO NOT resolve the action fully if a dice roll is needed — instead describe the attempt " +
                "and request the roll. When you include diceRollRequest, describe only the attempt " +
                "and immediate setup, not the final success/failure outcome. " +
                "For actions that don't need a roll, resolve them directly with appropriate consequences: " +
                "HP changes (keep them small, -1 to -5 for minor, -5 to -10 for serious), " +
                "item gains/losses, location changes, quest updates. " +
                "Vary outcomes across candidates — at least one should include a complication or twist. " +
                "The world is ALIVE. Include environmental changes, NPC reactions, or emerging threats. " +
                "Use newNpcs, newNpcProfiles, questUpdate, and location fields ACTIVELY."
    )
    @AiResponse(
        description = "Four diverse action outcome candidates with engagement scores",
        responseClass = ActionOutcomeProposals::class
    )
    @Memorize(label = "Last action proposals")
    fun proposeActionOutcomes(
        @AiParameter(description = "Name of the character performing the action")
        characterName: String,
        @AiParameter(description = "What the character wants to do")
        action: String
    ): Future<ActionOutcomeProposals>

    @AiOperation(
        summary = "Resolve Dice Roll",
        description = "A dice roll has been made. The 'success' parameter tells you definitively " +
                "whether the roll succeeded — you MUST narrate a success if success=true and " +
                "a failure if success=false. Do NOT contradict the success/failure outcome. " +
                "Be dramatic — describe near-misses, critical successes (natural 20), " +
                "and critical failures (natural 1) with extra flair. " +
                "Apply appropriate consequences based on the outcome. " +
                "Explicitly populate the hpChange (use negative numbers for damage), statusChange, itemsGained, and itemsLost fields to reflect the narrative consequences of the roll."
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
                "Choose an evocative name, an interesting race, a fitting class, and a physical description (look). " +
                "Be creative — go beyond the standard choices. Make each character unique and memorable. " +
                "IMPORTANT: The concept MUST be different from existing party members. " +
                "Avoid duplicate names, races, and classes to create a balanced, diverse party. " +
                "If a partial concept is provided (e.g. just a name, or just a race), " +
                "fill in only the missing fields while keeping the provided ones."
    )
    @AiResponse(
        description = "A character concept with name, look, race, and class — different from existing party members",
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
        description = "Generate a rich, compelling backstory and physical description (look) for a new character joining the adventure. " +
                "The backstory should be 2-3 paragraphs, fitting the character's race and class. " +
                "Also suggest appropriate ability scores (using values from the standard array: " +
                "15, 14, 13, 12, 10, 8 distributed across STR/DEX/CON/INT/WIS/CHA based on class needs) " +
                "and starting equipment appropriate for a level 1 character of this class."
    )
    @AiResponse(
        description = "A backstory with suggested ability scores, starting items, and physical description",
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
        summary = "Propose Round Outcomes",
        description = "All party members have acted this round. Generate EXACTLY 4 diverse outcome candidates. " +
                "Each candidate MUST be a different category: combat, social, exploration, dramatic_twist. " +
                "Rate each candidate's engagementScore from 1-10 (10 = most exciting/engaging). " +
                "At least ONE candidate MUST include combat with partyEffects dealing HP damage. " +
                "Mention each character by name. " +
                "CRITICAL: You are a great DM. You MUST advance the PLOT. " +
                "You have FULL POWER to change the world state: " +
                "move the party to a new location (newLocationName), introduce NPCs (newNpcs/newNpcProfiles), " +
                "add quests (questUpdate), apply effects to characters (partyEffects), " +
                "and inject world events (worldEvent). " +
                "USE THESE FIELDS in your candidates. Do not just narrate — MAKE things happen. " +
                "Each candidate should tell a DIFFERENT version of what could happen next. " +
                "The combat candidate should include actual combat with damage via partyEffects. " +
                "The dramatic_twist candidate should be surprising and change the situation fundamentally. " +
                "If stagnation is detected, ALL candidates should include at least one mutation field. " +
                "NEVER repeat previous scene descriptions verbatim."
    )
    @AiResponse(
        description = "Four diverse round outcome candidates with engagement scores and categories",
        responseClass = RoundOutcomeProposals::class
    )
    fun proposeRoundOutcomes(): Future<RoundOutcomeProposals>

    @AiOperation(
        summary = "Generate Scene Image Prompt",
        description = "Generate a cinematic image prompt for the current D&D scene using the latest world state, " +
                "recent actions, and round summary. Focus on environment, mood, lighting, and key characters. " +
                "Keep it consistent with the current setting and tone. " +
                "CRITICAL: The prompt MUST be PG-13 and safe for work. Do NOT include any gore, extreme violence, blood, or sexual content. " +
                "Focus on action poses, magical effects, and atmosphere instead of violence. " +
                "Incorporate the requested art style into the prompt."
    )
    @AiResponse(
        description = "A generated image prompt for the current scene",
        responseClass = String::class
    )
    fun generateSceneImagePrompt(
        @AiParameter(description = "The desired art style for the image (e.g., Anime, Cinematic, Dark Fantasy)")
        artStyle: String
    ): Future<String>

    @AiOperation(
        summary = "Generate Image",
        description = "Generate an image using the provided prompt."
    )
    @AiResponse(
        description = "A generated image",
        responseClass = ImageResult::class
    )
    fun generateImage(
        @AiParameter(description = "The prompt to use for image generation")
        prompt: String
    ): Future<ImageResult>
}
