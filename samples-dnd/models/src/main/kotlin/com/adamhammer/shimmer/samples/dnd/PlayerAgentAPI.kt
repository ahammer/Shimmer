package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.annotations.*
import com.adamhammer.shimmer.annotations.Terminal
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
        description = "Observe the current scene and situation from the world state. " +
                "Consider what is happening, who is present, what dangers or opportunities exist, " +
                "and how your character would perceive this scene given their backstory and skills. " +
                "Review the action log to remember what you and others have done recently."
    )
    @AiResponse(
        description = "Your character's observations and thoughts about the current situation",
        responseClass = String::class
    )
    @Memorize(label = "Current observations")
    fun observeSituation(): Future<String>

    @AiOperation(
        summary = "Check Abilities",
        description = "Review your current inventory, health, skills, class abilities, and known tactical options. " +
            "Decide what actions are realistically available this turn. Call this at most ONCE per turn."
    )
    @AiResponse(
        description = "A concise capability assessment for this turn",
        responseClass = String::class
    )
    @Memorize(label = "Capability assessment")
    fun checkAbilities(): Future<String>

    @AiOperation(
        summary = "Whisper Planning",
        description = "Think about party coordination and decide whether to send a short whisper this turn. " +
            "This step should only plan coordination; the actual whisper is sent via commitAction whisperTarget/whisperMessage. Call this at most ONCE per turn."
    )
    @AiResponse(
        description = "A short tactical-social reflection for this turn",
        responseClass = String::class
    )
    @Memorize(label = "Whisper planning")
    fun whisper(): Future<String>

    @AiOperation(
        summary = "Commit Action",
        description = "Based on your observations, capabilities, and team dynamics, commit to a final action for this turn. " +
                "Stay in character — consider your backstory, personality, class abilities, " +
                "and the needs of the party. Be decisive and specific. " +
                "State your action in ONE short sentence as a player command " +
                "(e.g. 'I attack the goblin with my longsword' or 'I search the chest for traps'). " +
                "Do NOT narrate what happens or describe the outcome — that is the DM's job. " +
                "IMPORTANT: Do NOT repeat an action you already took in a previous round. " +
                "Check the action log — if you did something last round, try something DIFFERENT. " +
                "React to what has changed in the world. Be creative and advance the story. " +
                "You may optionally provide journalEntry, emotionalUpdate, goalUpdate, whisperTarget, and whisperMessage " +
                "to persist your own internal state and coordinate with teammates."
    )
    @AiResponse(
        description = "A short, specific player action (1-2 sentences max), not narration",
        responseClass = PlayerAction::class
    )
    @Memorize(label = "Chosen action")
    @Terminal
    fun commitAction(): Future<PlayerAction>
}
