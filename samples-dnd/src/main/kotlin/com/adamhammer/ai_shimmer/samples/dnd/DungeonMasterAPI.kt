package com.adamhammer.ai_shimmer.samples.dnd

import com.adamhammer.ai_shimmer.annotations.*
import com.adamhammer.ai_shimmer.samples.dnd.model.ActionResult
import com.adamhammer.ai_shimmer.samples.dnd.model.SceneDescription
import java.util.concurrent.Future

/**
 * The Dungeon Master AI interface. Each method represents a DM action
 * that the AI will narrate and resolve.
 */
interface DungeonMasterAPI {

    @AiOperation(
        summary = "Describe Scene",
        description = "Describe the current scene to the player. Be vivid and atmospheric. " +
                "Include sensory details — sights, sounds, smells. Suggest possible actions."
    )
    @AiResponse(
        description = "A vivid scene description with suggested actions",
        responseClass = SceneDescription::class
    )
    @Memorize(label = "Last scene description")
    fun describeScene(): Future<SceneDescription>

    @AiOperation(
        summary = "Process Player Action",
        description = "The player has taken an action. Narrate what happens. " +
                "Determine if the action succeeds based on the world state and common sense. " +
                "Apply reasonable consequences: HP changes (keep them small, -1 to -5 for minor injuries, " +
                "-5 to -10 for serious ones), item gains/losses, location changes, quest updates. " +
                "Be creative but fair. The world should feel alive and reactive."
    )
    @AiResponse(
        description = "The result of the player's action including narrative and state changes",
        responseClass = ActionResult::class
    )
    @Memorize(label = "Last action result")
    fun processAction(
        @AiParameter(description = "What the player wants to do")
        action: String
    ): Future<ActionResult>
}
