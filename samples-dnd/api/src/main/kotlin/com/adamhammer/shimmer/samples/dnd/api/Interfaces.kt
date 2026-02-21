package com.adamhammer.shimmer.samples.dnd.api

import com.adamhammer.shimmer.model.ImageResult
import com.adamhammer.shimmer.samples.dnd.model.*

interface GameEventListener {
    fun onWorldBuildingStep(step: String, details: String) {}
    fun onAgentStep(
        characterName: String,
        stepNumber: Int,
        methodName: String,
        details: String,
        terminal: Boolean,
        pointsSpent: Int,
        pointsRemaining: Int
    ) {
    }
    fun onRoundStarted(round: Int, world: World) {}
    fun onSceneDescription(scene: SceneDescription)
    fun onImageGenerated(image: ImageResult) {}
    fun onActionResult(result: ActionResult, world: World)
    fun onDiceRollRequested(character: Character, request: DiceRollRequest) {}
    fun onRoundSummary(summary: SceneDescription, world: World)
    fun onGameOver(world: World)
    fun onEndGameSummaryGenerated(reportPath: String) {}
    fun onCharacterThinking(characterName: String)
    fun onCharacterAction(characterName: String, action: String)
    fun onWhisper(fromCharacter: String, toCharacter: String, message: String) {}
}
