package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.annotations.*
import com.adamhammer.shimmer.samples.dnd.model.PlayerAction
import java.util.concurrent.Future

/**
 * AI interface for an autonomous party member. Each method represents a phase
 * of the AI character's decision-making process. Driven by [AutonomousAgent]
 * with a [DecidingAgentAPI] that picks which phase to run next.
 */
interface PlayerAgentAPI {

    @AiOperation(
        summary = "Observe Situation",
        description = "Observe the current scene and situation. Consider what is happening, " +
                "who is present, what dangers or opportunities exist, and how your character " +
                "would perceive this scene given their backstory, personality, and skills."
    )
    @AiResponse(
        description = "Your character's observations and thoughts about the current situation",
        responseClass = String::class
    )
    @Memorize(label = "Current observations")
    fun observeSituation(
        @AiParameter(description = "The DM's description of the current scene and recent events")
        sceneDescription: String
    ): Future<String>

    @AiOperation(
        summary = "Decide Action",
        description = "Based on your observations, decide what action your character takes. " +
                "Stay in character — consider your backstory, personality, class abilities, " +
                "and the needs of the party. Be decisive and specific. " +
                "Describe the action in first person from your character's perspective."
    )
    @AiResponse(
        description = "The action your character wants to take, with reasoning",
        responseClass = PlayerAction::class
    )
    @Memorize(label = "Chosen action")
    fun decideAction(): Future<PlayerAction>
}
