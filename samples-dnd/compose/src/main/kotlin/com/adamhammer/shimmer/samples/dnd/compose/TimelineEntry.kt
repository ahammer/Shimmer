package com.adamhammer.shimmer.samples.dnd.compose

import androidx.compose.ui.graphics.Color

/**
 * A single entry in the unified game timeline.
 * Every game event — DM narration, player actions, dice rolls, images, whispers — becomes one of these.
 */
sealed class TimelineEntry {
    abstract val timestamp: Long

    /** Round divider / header */
    data class RoundHeader(
        val round: Int,
        val locationName: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    /** DM narration — scene descriptions, summaries, world events */
    data class DmNarration(
        val title: String,
        val narrative: String,
        val category: DmCategory = DmCategory.SCENE,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    enum class DmCategory { SCENE, SUMMARY, WORLD_BUILD, WORLD_EVENT }

    /** A generated image — appears inline in the timeline */
    data class SceneImage(
        val base64: String,
        val caption: String = "",
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    /** Character action — shown chat-style with avatar and name */
    data class CharacterAction(
        val characterName: String,
        val action: String,
        val outcome: String = "",
        val success: Boolean? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    /** Character internal thinking / agent steps — collapsible, muted */
    data class CharacterThinking(
        val characterName: String,
        val stepNumber: Int,
        val methodName: String,
        val details: String,
        val isTerminal: Boolean = false,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    /** Dice roll events */
    data class DiceRoll(
        val characterName: String,
        val rollType: String,
        val difficulty: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    /** Private whisper between characters */
    data class Whisper(
        val from: String,
        val to: String,
        val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()

    /** System messages — game start, game end, errors */
    data class SystemMessage(
        val title: String,
        val details: String,
        val isError: Boolean = false,
        override val timestamp: Long = System.currentTimeMillis()
    ) : TimelineEntry()
}

/** Map character names to consistent avatar colors */
object AvatarColors {
    private val palette = listOf(
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF26A69A), // Teal
        Color(0xFFEF5350), // Red
        Color(0xFFAB47BC), // Purple
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF42A5F5), // Blue
        Color(0xFF66BB6A), // Green
        Color(0xFFFFA726), // Orange
    )

    private val assignments = mutableMapOf<String, Color>()

    fun colorFor(name: String): Color {
        return assignments.getOrPut(name.lowercase()) {
            palette[assignments.size % palette.size]
        }
    }

    fun reset() = assignments.clear()
}
