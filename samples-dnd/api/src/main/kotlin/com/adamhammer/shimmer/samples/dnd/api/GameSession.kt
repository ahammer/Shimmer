package com.adamhammer.shimmer.samples.dnd.api

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.agents.AutonomousAgent
import com.adamhammer.shimmer.agents.DecidingAgentAPI
import com.adamhammer.shimmer.agents.AgentDispatcher
import com.adamhammer.shimmer.agents.AiDecision
import com.adamhammer.shimmer.model.ImageResult
import com.adamhammer.shimmer.samples.dnd.*
import com.adamhammer.shimmer.samples.dnd.model.*
import com.adamhammer.shimmer.shimmer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.random.Random

class GameSession(
    private val apiAdapter: ApiAdapter,
    private val listener: GameEventListener,
    maxTurns: Int = 3,
    private val enableImages: Boolean = true,
    private val artStyle: String = "Anime"
) {
    private data class StorySection(
        val title: String,
        val narrative: String,
        var image: ImageResult? = null
    )

    private data class TurnBreakdown(
        val round: Int,
        val characterName: String,
        val action: String,
        val success: Boolean,
        val narrative: String,
        val diceDetail: String?
    )

    companion object {
        private const val DEFAULT_MAX_TURNS = 3
        private const val MAX_ALLOWED_TURNS = 100
        private const val AGENT_TURN_BUDGET = 5
        private const val IMAGE_THREAD_JOIN_TIMEOUT_MS = 3000L
        private val SELECTION_WEIGHTS = intArrayOf(4, 3, 2, 1)
    }

    private val maxTurnsConfigured = maxTurns.coerceIn(DEFAULT_MAX_TURNS, MAX_ALLOWED_TURNS)

    private lateinit var world: World
    private lateinit var dm: DungeonMasterAPI
    private lateinit var aiAgents: Map<String, AutonomousAgent<PlayerAgentAPI>>
    private val turnStates: MutableMap<String, TurnState> = mutableMapOf()
    private val turnHistories: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val characterPreviousActions: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val storySections: MutableList<StorySection> = Collections.synchronizedList(mutableListOf())
    private val turnBreakdown: MutableList<TurnBreakdown> = Collections.synchronizedList(mutableListOf())
    private val imageWorkers: MutableList<Thread> = Collections.synchronizedList(mutableListOf())
    private var openingScene: SceneDescription = SceneDescription()

    suspend fun startGame(initialWorld: World) {
        world = initialWorld.copy(maxRounds = maxTurnsConfigured)

        dm = shimmer<DungeonMasterAPI> {
            adapter(apiAdapter)
            addInterceptor(WorldStateInterceptor { world })
            resilience {
                maxRetries = 2
                retryDelayMs = 500
                resultValidator = { r ->
                    when (r) {
                        is ActionResult -> r.narrative.isNotBlank() && r.hpChange in -15..15
                        is SceneDescription -> r.narrative.isNotBlank()
                        is RoundSummaryResult -> r.narrative.isNotBlank()
                        is RoundOutcomeProposals -> r.candidates.count { it.narrative.isNotBlank() } >= 2
                        is ActionOutcomeProposals -> r.candidates.count { it.narrative.isNotBlank() } >= 2
                        else -> true
                    }
                }
            }
        }.api

        openingScene = buildWorldSetup()
        aiAgents = buildAiAgents(world)

        val openingSectionIndex = appendStorySection("Opening Scene", openingScene.narrative)
        listener.onSceneDescription(openingScene)
        requestSceneImage(openingSectionIndex)

        runGameLoop()
    }

    private fun buildWorldSetup(): SceneDescription {
        listener.onWorldBuildingStep("start", "The DM is constructing campaign lore and an opening situation.")

        val worldBuilder = shimmer<DungeonMasterWorldBuilderAPI> {
            adapter(apiAdapter)
            addInterceptor(WorldStateInterceptor { world })
            resilience {
                maxRetries = 2
                retryDelayMs = 500
            }
        }.api

        val steps = listOf(
            "buildCampaignPremise" to { worldBuilder.buildCampaignPremise().get() as Any },
            "buildLocationGraph" to { worldBuilder.buildLocationGraph().get() as Any },
            "buildNpcRegistry" to { worldBuilder.buildNpcRegistry().get() as Any },
            "buildPlotHooks" to { worldBuilder.buildPlotHooks().get() as Any }
        )

        for ((stepName, stepFn) in steps) {
            try {
                val result = stepFn()
                val detail = result.toString().take(200).ifBlank { "No details" }
                listener.onWorldBuildingStep(stepName, detail)
            } catch (e: Exception) {
                listener.onWorldBuildingStep(
                    stepName,
                    "Step failed (${e.message?.take(120)}); continuing with remaining steps."
                )
            }
        }

        val worldBuildResult = try {
            worldBuilder.commitWorldSetup().get()
        } catch (e: Exception) {
            listener.onWorldBuildingStep(
                "fallback",
                "World setup commit failed (${e.message?.take(80)}); using default opening scene."
            )
            return dm.describeScene().get()
        }

        val detail = worldBuildResult.toString().take(200).ifBlank { "No details" }
        listener.onWorldBuildingStep("commitWorldSetup", detail)

        val mergedLore = world.lore.copy(
            campaignPremise = worldBuildResult.campaignPremise.ifBlank { world.lore.campaignPremise },
            locations = worldBuildResult.lore.locations.ifEmpty { world.lore.locations },
            npcs = worldBuildResult.lore.npcs.ifEmpty { world.lore.npcs },
            plotHooks = worldBuildResult.lore.plotHooks.ifEmpty { world.lore.plotHooks },
            factions = worldBuildResult.lore.factions.ifEmpty { world.lore.factions }
        )

        val startLocation = worldBuildResult.startingLocation
        val location = if (startLocation.name.isNotBlank()) {
            val seededNpcs = mergedLore.npcs
                .filter { it.currentLocation.equals(startLocation.name, ignoreCase = true) }
                .map { it.name }
                .ifEmpty { startLocation.npcs }
            startLocation.copy(npcs = seededNpcs)
        } else world.location
        val newQuests = (world.questLog + worldBuildResult.initialQuests).distinct()

        world = world.copy(
            lore = mergedLore,
            location = location,
            questLog = newQuests
        )

        listener.onWorldBuildingStep(
            "commit",
            "World setup committed with ${mergedLore.npcs.size} NPC(s) and ${mergedLore.locations.size} location node(s)."
        )

        return worldBuildResult.openingScene.takeIf { it.narrative.isNotBlank() }
            ?: dm.describeScene().get()
    }

    private suspend fun runGameLoop() {
        while (world.party.any { it.hp > 0 } && world.round < maxTurnsConfigured) {
            world = world.copy(
                round = world.round + 1,
                turnsAtCurrentLocation = world.turnsAtCurrentLocation + 1
            )
            listener.onRoundStarted(world.round, world)

            processRound()

            if (world.party.all { it.hp <= 0 }) {
                finishGame()
                return
            }

            val proposals = dm.proposeRoundOutcomes().get()
            val selected = weightedRandomSelect(proposals.candidates) { it.engagementScore }
            val summary = selected.toRoundSummaryResult()
            val selectionLog = "Round ${world.round}: \uD83C\uDFAF DM chose '${selected.category}' " +
                "(score: ${selected.engagementScore}/10)"
            world = world.copy(actionLog = (world.actionLog + selectionLog).takeLast(40))
            world = applyRoundSummary(world, summary)
            val sceneDescription = SceneDescription(
                narrative = summary.narrative,
                availableActions = summary.availableActions
            )
            val summarySectionIndex = appendStorySection("Round ${world.round} Summary", summary.narrative)
            listener.onRoundSummary(sceneDescription, world)
            requestSceneImage(summarySectionIndex)
        }

        finishGame()
    }

    private suspend fun processRound() {
        val currentRoundCharacters = world.party.toList()
        
        for (character in currentRoundCharacters) {
            val currentStats = world.party.find { it.name == character.name } ?: continue
            
            if (currentStats.hp <= 0) {
                continue
            }

            val playerAction = resolveCharacterAction(currentStats)
            val action = playerAction.action.ifBlank { "I hold my position and reassess." }

            val actionProposals = dm.proposeActionOutcomes(currentStats.name, action).get()
            val selectedAction = weightedRandomSelect(actionProposals.candidates) { it.engagementScore }
            val result = selectedAction.toActionResult()
            val actionSelLog = "Round ${world.round}: \uD83C\uDFAF ${currentStats.name} outcome '${selectedAction.category}' " +
                "(score: ${selectedAction.engagementScore}/10)"
            world = world.copy(actionLog = (world.actionLog + actionSelLog).takeLast(40))
            val finalResult = handleDiceRoll(currentStats, result)

            world = applyResult(world, finalResult, currentStats.name)
            world = applyPlayerStateUpdate(world, currentStats.name, playerAction)
            
            val logEntry = "Round ${world.round}: ${currentStats.name} — $action → " +
                (if (finalResult.success) "success" else "failure") + ". DM: ${finalResult.narrative}"
            
            characterPreviousActions.getOrPut(currentStats.name) { mutableListOf() }
                .apply { add(logEntry); if (size > 5) removeFirst() }
                
            world = world.copy(
                actionLog = (world.actionLog + logEntry).takeLast(40)
            )
            val roundPrefix = "Round ${world.round}:"
            val diceDetail = world.actionLog.lastOrNull {
                it.startsWith(roundPrefix) && it.contains("🎲") && it.contains(currentStats.name)
            }
            turnBreakdown += TurnBreakdown(
                round = world.round,
                characterName = currentStats.name,
                action = action,
                success = finalResult.success,
                narrative = finalResult.narrative,
                diceDetail = diceDetail
            )
            listener.onActionResult(finalResult, world)
        }
    }

    private suspend fun resolveCharacterAction(character: Character): PlayerAction {
        return resolveAiAction(character)
    }

    private suspend fun resolveAiAction(character: Character): PlayerAction {
        listener.onCharacterThinking(character.name)
        val agent = aiAgents[character.name]
        val turnHistory = mutableListOf<String>()
        turnHistories[character.name] = turnHistory
        val prevActions = characterPreviousActions[character.name] ?: emptyList()
        turnStates[character.name] = TurnState(
            phase = "OBSERVE",
            stepsUsed = 0,
            stepsBudget = AGENT_TURN_BUDGET,
            recentSteps = turnHistory,
            previousRoundActions = prevActions
        )
        if (agent != null) {
            try {
                val observedStep = try {
                    agent.invoke("observeSituation")
                } catch (e: Exception) {
                    AgentDispatcher.DispatchResult(
                        methodName = "observeSituation",
                        value = "Observation unavailable: ${e.message?.take(120) ?: "unknown error"}",
                        isTerminal = false
                    )
                }
                turnHistory += "${observedStep.methodName}: ${summarizeStepValue(observedStep.value)}"
                turnStates[character.name] = TurnState(
                    phase = "PLAN",
                    stepsUsed = 0,
                    stepsBudget = AGENT_TURN_BUDGET,
                    recentSteps = turnHistory,
                    previousRoundActions = prevActions
                )
                listener.onAgentStep(
                    characterName = character.name,
                    stepNumber = 0,
                    methodName = observedStep.methodName,
                    details = summarizeStepValue(observedStep.value),
                    terminal = false,
                    pointsSpent = 0,
                    pointsRemaining = AGENT_TURN_BUDGET
                )

                val currentExcludedMethods = mutableSetOf("observeSituation")

                for (stepIndex in 1..AGENT_TURN_BUDGET) {
                    val stepResult = try {
                        agent.stepDetailed(excludedMethods = currentExcludedMethods)
                    } catch (e: Exception) {
                        listener.onAgentStep(
                            characterName = character.name,
                            stepNumber = stepIndex,
                            methodName = "recover",
                            details = "Recovered from invalid step (${e.message?.take(120)}); forcing commitAction.",
                            terminal = false,
                            pointsSpent = stepIndex,
                            pointsRemaining = (AGENT_TURN_BUDGET - stepIndex).coerceAtLeast(0)
                        )
                        agent.invoke("commitAction", mapOf("recentActions" to prevActions.joinToString("\n")))
                    }
                    turnHistory += "${stepResult.methodName}: ${summarizeStepValue(stepResult.value)}"
                    currentExcludedMethods.add(stepResult.methodName)
                    turnStates[character.name] = TurnState(
                        phase = if (stepResult.isTerminal) "DONE" else "PLAN",
                        stepsUsed = stepIndex,
                        stepsBudget = AGENT_TURN_BUDGET,
                        recentSteps = turnHistory,
                        previousRoundActions = prevActions
                    )
                    listener.onAgentStep(
                        characterName = character.name,
                        stepNumber = stepIndex,
                        methodName = stepResult.methodName,
                        details = summarizeStepValue(stepResult.value),
                        terminal = stepResult.isTerminal,
                        pointsSpent = stepIndex,
                        pointsRemaining = (AGENT_TURN_BUDGET - stepIndex).coerceAtLeast(0)
                    )

                    if (!stepResult.isTerminal) {
                        continue
                    }

                    val playerAction = when (val value = stepResult.value) {
                        is PlayerAction -> value
                        else -> PlayerAction(
                            action = parseAiAction(value?.toString() ?: "", character.name),
                            reasoning = "Parsed from non-structured terminal output."
                        )
                    }
                    val actionText = playerAction.action.ifBlank { "I hold my position and reassess." }
                    listener.onCharacterAction(character.name, actionText)
                    if (playerAction.whisperTarget.isNotBlank() && playerAction.whisperMessage.isNotBlank()) {
                        listener.onWhisper(character.name, playerAction.whisperTarget, playerAction.whisperMessage)
                    }
                    return playerAction
                }

                val fallbackTerminal = agent.invoke("commitAction", mapOf("recentActions" to prevActions.joinToString("\n")))
                turnHistory += "${fallbackTerminal.methodName}: ${summarizeStepValue(fallbackTerminal.value)}"
                turnStates[character.name] = TurnState(
                    phase = if (fallbackTerminal.isTerminal) "DONE" else "PLAN",
                    stepsUsed = AGENT_TURN_BUDGET,
                    stepsBudget = AGENT_TURN_BUDGET,
                    recentSteps = turnHistory,
                    previousRoundActions = prevActions
                )
                listener.onAgentStep(
                    characterName = character.name,
                    stepNumber = AGENT_TURN_BUDGET + 1,
                    methodName = fallbackTerminal.methodName.ifBlank { "fallback" },
                    details = summarizeStepValue(fallbackTerminal.value),
                    terminal = fallbackTerminal.isTerminal,
                    pointsSpent = AGENT_TURN_BUDGET,
                    pointsRemaining = 0
                )

                val playerAction = when (val value = fallbackTerminal.value) {
                    is PlayerAction -> value
                    else -> PlayerAction(
                        action = parseAiAction(value?.toString() ?: "", character.name),
                        reasoning = "Fallback after budget exhaustion."
                    )
                }
                val actionText = playerAction.action.ifBlank { "I hold my position and reassess." }
                listener.onCharacterAction(character.name, actionText)
                if (playerAction.whisperTarget.isNotBlank() && playerAction.whisperMessage.isNotBlank()) {
                    listener.onWhisper(character.name, playerAction.whisperTarget, playerAction.whisperMessage)
                }
                return playerAction
            } catch (e: Exception) {
                // fall through to default action
            }
        }
        turnStates[character.name] = TurnState(
            phase = "DONE",
            stepsUsed = AGENT_TURN_BUDGET,
            stepsBudget = AGENT_TURN_BUDGET,
            recentSteps = turnHistory,
            previousRoundActions = prevActions
        )
        val fallback = PlayerAction(
            action = "I look around cautiously.",
            reasoning = "Fallback action after agent failure."
        )
        listener.onCharacterAction(character.name, fallback.action)
        return fallback
    }

    private fun summarizeStepValue(value: Any?): String {
        val raw = when (value) {
            is PlayerAction -> {
                val actionText = value.action.ifBlank { "(no action)" }
                val reason = value.reasoning.ifBlank { "(no reasoning)" }
                "action=$actionText | reasoning=$reason"
            }
            null -> "(no output)"
            else -> value.toString()
        }
        return raw.replace("\n", " ").take(220)
    }

    private suspend fun handleDiceRoll(character: Character, result: ActionResult): ActionResult {
        val roll = result.diceRollRequest ?: return result
        listener.onDiceRollRequested(character, roll)
        val diceValue = Random.nextInt(1, 21)

        val normalizedModifier = normalizeRollModifier(character, roll)

        val total = diceValue + normalizedModifier
        val success = total >= roll.difficulty

        val modSign = if (normalizedModifier >= 0) "+" else ""
        val rollLog = "Round ${world.round}: \uD83C\uDFB2 ${character.name} ${roll.rollType}: " +
            "d20($diceValue) $modSign$normalizedModifier = $total vs DC ${roll.difficulty} \u2192 " +
            if (success) "SUCCESS" else "FAIL"
        world = world.copy(actionLog = (world.actionLog + rollLog).takeLast(40))

        return dm.resolveRoll(
            roll.characterName,
            roll.rollType,
            diceValue,
            normalizedModifier,
            roll.difficulty,
            total,
            success
        ).get()
    }

    private fun normalizeRollModifier(character: Character, roll: DiceRollRequest): Int {
        val expected = expectedRollModifier(character, roll.rollType)
        if (expected == null) {
            return roll.modifier
        }
        return expected
    }

    private fun expectedRollModifier(character: Character, rollType: String): Int? {
        val text = rollType.lowercase()
        val scores = character.abilityScores
        val abilityMod = when {
            "strength" in text || "athletics" in text -> CharacterUtils.abilityModifier(scores.str)
            "dexterity" in text || "stealth" in text || "acrobatics" in text || "sleight" in text -> CharacterUtils.abilityModifier(scores.dex)
            "constitution" in text -> CharacterUtils.abilityModifier(scores.con)
            "intelligence" in text || "arcana" in text || "investigation" in text || "history" in text || "nature" in text || "religion" in text -> CharacterUtils.abilityModifier(scores.int)
            "wisdom" in text || "perception" in text || "insight" in text || "animal handling" in text || "medicine" in text || "survival" in text -> CharacterUtils.abilityModifier(scores.wis)
            "charisma" in text || "deception" in text || "intimidation" in text || "performance" in text || "persuasion" in text -> CharacterUtils.abilityModifier(scores.cha)
            else -> return null
        }

        val proficiency = if (character.skills.any { text.contains(it.lowercase()) }) {
            character.proficiencyBonus
        } else {
            0
        }

        return abilityMod + proficiency
    }

    private fun buildAiAgents(world: World): Map<String, AutonomousAgent<PlayerAgentAPI>> {
        val agents = mutableMapOf<String, AutonomousAgent<PlayerAgentAPI>>()

        for (character in world.party) {
            val characterProvider = {
                this@GameSession.world.party.first { it.name == character.name }
            }

            val toolProvider = PlayerToolProvider(characterProvider)
            val playerInstance = shimmer<PlayerAgentAPI> {
                adapter(apiAdapter)
                addInterceptor(WorldStateInterceptor(isDm = false) { this@GameSession.world })
                addInterceptor(CharacterInterceptor(characterProvider))
                addInterceptor(
                    TurnStateInterceptor {
                        turnStates[character.name] ?: TurnState(
                            phase = "PLAN",
                            stepsUsed = 0,
                            stepsBudget = AGENT_TURN_BUDGET,
                            recentSteps = turnHistories[character.name] ?: emptyList()
                        )
                    }
                )
                toolProvider(toolProvider)
                resilience { maxRetries = 1 }
            }

            val decider = shimmer<DecidingAgentAPI> {
                adapter(apiAdapter)
                addInterceptor(WorldStateInterceptor(isDm = false) { this@GameSession.world })
                addInterceptor(CharacterInterceptor(characterProvider))
                addInterceptor(
                    TurnStateInterceptor {
                        turnStates[character.name] ?: TurnState(
                            phase = "PLAN",
                            stepsUsed = 0,
                            stepsBudget = AGENT_TURN_BUDGET,
                            recentSteps = turnHistories[character.name] ?: emptyList()
                        )
                    }
                )
                toolProvider(toolProvider)
                resilience {
                    maxRetries = 2
                    resultValidator = { r ->
                        if (r is AiDecision) r.method != "decideNextAction" else true
                    }
                }
            }.api

            agents[character.name] = AutonomousAgent(playerInstance, decider)
        }
        return agents
    }

    private fun applyPlayerStateUpdate(world: World, characterName: String, action: PlayerAction): World {
        val normalizedTarget = action.whisperTarget.trim()
        val whisperMessage = action.whisperMessage.trim()
        val whisper = if (normalizedTarget.isNotBlank() && whisperMessage.isNotBlank()) {
            WhisperMessage(
                from = characterName,
                to = normalizedTarget,
                message = whisperMessage.take(220),
                round = world.round
            )
        } else {
            null
        }

        val updatedParty = world.party.map { character ->
            if (!character.name.equals(characterName, ignoreCase = true)) {
                character
            } else {
                val updatedGoals = action.goalUpdate.trim().takeIf { it.isNotBlank() }
                    ?.let { (character.goals + it).distinct().takeLast(6) }
                    ?: character.goals
                val updatedEmotion = action.emotionalUpdate.trim().ifBlank { character.emotionalState }
                val updatedJournal = action.journalEntry.trim().takeIf { it.isNotBlank() }
                    ?.let { (character.journal + "R${world.round}: $it").takeLast(12) }
                    ?: character.journal
                val updatedRelationships = if (whisper != null && whisper.to.isNotBlank()) {
                    character.relationships + (whisper.to to "coordinating via whispers")
                } else {
                    character.relationships
                }

                character.copy(
                    goals = updatedGoals,
                    emotionalState = updatedEmotion,
                    journal = updatedJournal,
                    relationships = updatedRelationships
                )
            }
        }

        val updatedWhisperLog = if (whisper != null) {
            (world.whisperLog + whisper).takeLast(30)
        } else {
            world.whisperLog
        }

        return world.copy(
            party = updatedParty,
            whisperLog = updatedWhisperLog
        )
    }

    private fun applyResult(world: World, result: ActionResult, actingCharacterName: String): World {
         val targetName = result.targetCharacterName.ifBlank {
            actingCharacterName
        }

        val newParty = world.party.map { c ->
            if (c.name.equals(targetName, ignoreCase = true)) {
                val newHp = (c.hp + result.hpChange)
                    .coerceIn(0, c.maxHp)
                val newInv = (c.inventory + result.itemsGained) -
                    result.itemsLost.toSet()
                val newStatus = result.statusChange.ifBlank { c.status }
                c.copy(hp = newHp, inventory = newInv, status = newStatus)
            } else {
                c
            }
        }

        val updatedLoreNpcs = if (result.newNpcProfiles.isNotEmpty()) {
            val merged = (world.lore.npcs + result.newNpcProfiles)
            merged.associateBy { it.name.lowercase() }.values.toList()
        } else {
            world.lore.npcs
        }

        val locationChanged = result.newLocationName.isNotBlank() && !result.newLocationName.equals(world.location.name, ignoreCase = true)
        val newLocation = if (result.newLocationName.isNotBlank()) {
            Location(
                name = result.newLocationName,
                description = result.newLocationDescription,
                exits = result.newExits,
                npcs = result.newNpcs
            )
        } else if (result.newNpcs.isNotEmpty()) {
            world.location.copy(
                npcs = (world.location.npcs + result.newNpcs).distinct()
            )
        } else {
            world.location
        }

        val newQuestLog = if (result.questUpdate.isNotBlank()) {
            world.questLog + result.questUpdate
        } else {
            world.questLog
        }

        return world.copy(
            party = newParty,
            location = newLocation,
            lore = world.lore.copy(npcs = updatedLoreNpcs),
            questLog = newQuestLog,
            turnsAtCurrentLocation = if (locationChanged) 0 else world.turnsAtCurrentLocation
        )
    }

    private fun applyRoundSummary(world: World, summary: RoundSummaryResult): World {
        var w = world

        // Apply location change
        if (summary.newLocationName.isNotBlank() && !summary.newLocationName.equals(w.location.name, ignoreCase = true)) {
            w = w.copy(
                location = Location(
                    name = summary.newLocationName,
                    description = summary.newLocationDescription,
                    exits = summary.newExits,
                    npcs = summary.newNpcs
                ),
                turnsAtCurrentLocation = 0
            )
        } else if (summary.newLocationName.isNotBlank()) {
            w = w.copy(
                location = Location(
                    name = summary.newLocationName,
                    description = summary.newLocationDescription,
                    exits = summary.newExits,
                    npcs = summary.newNpcs
                )
            )
        } else if (summary.newNpcs.isNotEmpty()) {
            w = w.copy(
                location = w.location.copy(
                    npcs = (w.location.npcs + summary.newNpcs).distinct()
                )
            )
        }

        // Apply NPC profiles to lore
        if (summary.newNpcProfiles.isNotEmpty()) {
            val merged = (w.lore.npcs + summary.newNpcProfiles)
                .associateBy { it.name.lowercase() }.values.toList()
            w = w.copy(lore = w.lore.copy(npcs = merged))
        }

        // Apply quest update
        if (summary.questUpdate.isNotBlank()) {
            w = w.copy(questLog = w.questLog + summary.questUpdate)
        }

        // Apply party effects (HP changes, status changes)
        if (summary.partyEffects.isNotEmpty()) {
            val updatedParty = w.party.map { c ->
                val effect = summary.partyEffects.find { it.characterName.equals(c.name, ignoreCase = true) }
                if (effect != null) {
                    val newHp = (c.hp + effect.hpChange).coerceIn(0, c.maxHp)
                    val newStatus = effect.statusChange.ifBlank { c.status }
                    c.copy(hp = newHp, status = newStatus)
                } else c
            }
            w = w.copy(party = updatedParty)
        }

        // Append world event to action log
        if (summary.worldEvent.isNotBlank()) {
            val eventLog = "Round ${w.round}: \uD83C\uDF0D WORLD EVENT — ${summary.worldEvent}"
            w = w.copy(actionLog = (w.actionLog + eventLog).takeLast(40))
        }

        // Append round summary narrative to action log
        if (summary.narrative.isNotBlank()) {
            val narrativeLog = "Round ${w.round} Summary: ${summary.narrative}"
            w = w.copy(actionLog = (w.actionLog + narrativeLog).takeLast(40))
        }

        return w
    }

    private fun <T> weightedRandomSelect(candidates: List<T>, scoreExtractor: (T) -> Int): T {
        if (candidates.size == 1) return candidates.first()
        val sorted = candidates.sortedByDescending(scoreExtractor)
        val weights = SELECTION_WEIGHTS
        val totalWeight = sorted.indices.sumOf { i -> weights.getOrElse(i) { 1 } }
        var roll = Random.nextInt(totalWeight)
        for ((i, candidate) in sorted.withIndex()) {
            roll -= weights.getOrElse(i) { 1 }
            if (roll < 0) return candidate
        }
        return sorted.first()
    }

    private fun RoundOutcomeCandidate.toRoundSummaryResult() = RoundSummaryResult(
        narrative = narrative,
        availableActions = availableActions,
        worldEvent = worldEvent,
        newLocationName = newLocationName,
        newLocationDescription = newLocationDescription,
        newExits = newExits,
        newNpcs = newNpcs,
        newNpcProfiles = newNpcProfiles,
        questUpdate = questUpdate,
        partyEffects = partyEffects
    )

    private fun ActionOutcomeCandidate.toActionResult() = ActionResult(
        narrative = narrative,
        success = success,
        targetCharacterName = targetCharacterName,
        hpChange = hpChange,
        itemsGained = itemsGained,
        itemsLost = itemsLost,
        newLocationName = newLocationName,
        newLocationDescription = newLocationDescription,
        newExits = newExits,
        newNpcs = newNpcs,
        newNpcProfiles = newNpcProfiles,
        questUpdate = questUpdate,
        statusChange = statusChange,
        diceRollRequest = diceRollRequest
    )

    private fun parseAiAction(stepResult: String, characterName: String): String {
        // Try JSON format: "action": "..."
        val jsonPattern = Regex(""""action"\s*:\s*"([^"]+)"""")
        jsonPattern.find(stepResult)?.groupValues?.get(1)?.let { return it }

        // Try Kotlin toString format: PlayerAction(action=..., reasoning=...)
        val toStringPattern = Regex("""action=([^,)]+)""")
        toStringPattern.find(stepResult)?.groupValues?.get(1)
            ?.trim()?.let { return it }

        return stepResult.take(200).ifBlank {
            "$characterName looks around cautiously."
        }
    }

    private fun requestSceneImage(storySectionIndex: Int) {
        if (!enableImages) return
        
        val worker = thread(isDaemon = true, name = "scene-image-generator") {
            try {
                val prompt = dm.generateSceneImagePrompt(artStyle).get()
                val image = dm.generateImage(prompt).get()
                synchronized(storySections) {
                    storySections.getOrNull(storySectionIndex)?.image = image
                }
                listener.onImageGenerated(image)
            } catch (_: Exception) {
            }
        }
        imageWorkers += worker
    }

    private fun appendStorySection(title: String, narrative: String): Int {
        synchronized(storySections) {
            storySections.add(
                StorySection(
                    title = title,
                    narrative = narrative.ifBlank { "(No narrative provided)" }
                )
            )
            return storySections.lastIndex
        }
    }

    private fun finishGame() {
        val reportPath = runCatching {
            waitForImageWorkers()
            writeEndGameSummaryMarkdown()
        }.getOrNull()

        listener.onGameOver(world)
        if (!reportPath.isNullOrBlank()) {
            listener.onEndGameSummaryGenerated(reportPath)
        }
    }

    private fun waitForImageWorkers() {
        val workersSnapshot = synchronized(imageWorkers) { imageWorkers.toList() }
        for (worker in workersSnapshot) {
            if (worker.isAlive) {
                runCatching { worker.join(IMAGE_THREAD_JOIN_TIMEOUT_MS) }
            }
        }
    }

    private fun writeEndGameSummaryMarkdown(): String {
        val generatedAt = LocalDateTime.now()
        val fileTimestamp = generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val displayTimestamp = generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val reportsDir = File("build/reports/dnd")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val reportFile = File(reportsDir, "dnd-endgame-summary-$fileTimestamp.md")
        val imageDir = File(reportsDir, "images/$fileTimestamp")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val storySnapshot = synchronized(storySections) { storySections.toList() }
        val turnSnapshot = synchronized(turnBreakdown) { turnBreakdown.toList() }

        val markdown = StringBuilder()
        markdown.append("# DND End-Game Summary\n\n")
        markdown.append("- Generated: $displayTimestamp\n")
        markdown.append("- Final Round: ${world.round}\n")
        markdown.append("- Final Location: ${world.location.name}\n\n")

        markdown.append("## Story + Images\n\n")
        if (storySnapshot.isEmpty()) {
            markdown.append("No story sections were captured.\n\n")
        } else {
            storySnapshot.forEachIndexed { index, section ->
                markdown.append("### ${index + 1}. ${section.title}\n\n")
                markdown.append(section.narrative.trim()).append("\n\n")
                section.image?.let { image ->
                    if (image.base64.isNotBlank()) {
                        val imageFileName = "section-${(index + 1).toString().padStart(2, '0')}-${slugify(section.title)}.png"
                        val imageFile = File(imageDir, imageFileName)
                        runCatching {
                            imageFile.writeBytes(Base64.getDecoder().decode(image.base64))
                        }.onSuccess {
                            val relativePath = "images/$fileTimestamp/$imageFileName"
                            markdown.append("![${section.title}](${relativePath.replace("\\", "/")})\n\n")
                        }
                    }
                }
            }
        }

        markdown.append("## Turn by Turn Breakdown\n\n")
        if (turnSnapshot.isEmpty()) {
            markdown.append("No turn events were captured.\n")
        } else {
            val turnsByRound = turnSnapshot.groupBy { it.round }.toSortedMap()
            for ((round, turns) in turnsByRound) {
                markdown.append("### Round $round\n\n")
                turns.forEachIndexed { index, turn ->
                    markdown.append("${index + 1}. **${turn.characterName}**\n")
                    markdown.append("   - Action: ${toInline(turn.action)}\n")
                    markdown.append("   - Outcome: ${if (turn.success) "Success" else "Failure"}\n")
                    markdown.append("   - Narrative: ${toInline(turn.narrative)}\n")
                    turn.diceDetail?.takeIf { it.isNotBlank() }?.let {
                        markdown.append("   - Dice: ${toInline(it)}\n")
                    }
                    markdown.append("\n")
                }
            }
        }

        reportFile.writeText(markdown.toString())
        return reportFile.absolutePath
    }

    private fun toInline(text: String): String = text.replace("\n", " ").trim()

    private fun slugify(input: String): String {
        val normalized = input
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return normalized.ifBlank { "scene" }
    }
}
