package com.adamhammer.shimmer.samples.dnd.api

import com.adamhammer.shimmer.model.ImageResult

/**
 * Lightweight timeline entries captured during a game session,
 * used to generate a markdown report that mirrors the in-game timeline.
 */
sealed class MarkdownEntry {
    data class RoundHeader(val round: Int, val locationName: String) : MarkdownEntry()
    data class DmNarration(val title: String, val narrative: String, val category: String) : MarkdownEntry()
    data class SceneImage(val image: ImageResult, val caption: String = "") : MarkdownEntry()
    data class CharacterAction(
        val characterName: String,
        val action: String,
        val outcome: String = "",
        val success: Boolean? = null
    ) : MarkdownEntry()
    data class DiceRoll(
        val characterName: String,
        val rollType: String,
        val difficulty: Int
    ) : MarkdownEntry()
    data class Whisper(val from: String, val to: String, val message: String) : MarkdownEntry()
    data class SystemMessage(val title: String, val details: String) : MarkdownEntry()
    data class WorldBuildStep(val step: String, val details: String) : MarkdownEntry()
}
