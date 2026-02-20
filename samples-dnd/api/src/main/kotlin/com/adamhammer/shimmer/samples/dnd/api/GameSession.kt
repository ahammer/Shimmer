package com.adamhammer.shimmer.samples.dnd.api

import com.adamhammer.shimmer.interfaces.ApiAdapter
import com.adamhammer.shimmer.agents.AutonomousAgent
import com.adamhammer.shimmer.agents.DecidingAgentAPI
import com.adamhammer.shimmer.agents.AgentDispatcher
import com.adamhammer.shimmer.agents.AiDecision
import com.adamhammer.shimmer.samples.dnd.*
import com.adamhammer.shimmer.samples.dnd.model.*
import com.adamhammer.shimmer.shimmer
import kotlin.random.Random

class GameSession(
    private val apiAdapter: ApiAdapter,
    private val listener: GameEventListener
) {
    companion object {
        private const val MAX_TURNS = 3
        private const val AGENT_TURN_BUDGET = 5
    }

    private lateinit var world: World
    private lateinit var dm: DungeonMasterAPI
    private lateinit var aiAgents: Map<String, AutonomousAgent<PlayerAgentAPI>>
    private val turnStates: MutableMap<String, TurnState> = mutableMapOf()
    private val turnHistories: MutableMap<String, MutableList<String>> = mutableMapOf()
    private var openingScene: SceneDescription = SceneDescription()

    suspend fun startGame(initialWorld: World) {
        world = initialWorld

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
                        else -> true
                    }
                }
            }
        }.api

        openingScene = buildWorldSetup()
        aiAgents = buildAiAgents(world)

        listener.onSceneDescription(openingScene)

        runGameLoop()
    }

    private fun buildWorldSetup(): SceneDescription {
        listener.onWorldBuildingStep("start", "The DM is constructing campaign lore and an opening situation.")

        val worldBuilderInstance = shimmer<DungeonMasterWorldBuilderAPI> {
            adapter(apiAdapter)
            addInterceptor(WorldStateInterceptor { world })
            resilience {
                maxRetries = 2
                retryDelayMs = 500
            }
        }

        val worldBuilderDecider = shimmer<DecidingAgentAPI> {
            adapter(apiAdapter)
            addInterceptor(WorldStateInterceptor { world })
            resilience { maxRetries = 1 }
        }.api

        val autonomous = AutonomousAgent(worldBuilderInstance, worldBuilderDecider)
        var finalStep: AgentDispatcher.DispatchResult? = null
        val completedWorldSteps = mutableSetOf<String>()
        val fallbackWorldSteps = ArrayDeque(
            listOf(
                "buildCampaignPremise",
                "buildLocationGraph",
                "buildNpcRegistry",
                "buildPlotHooks",
                "commitWorldSetup"
            )
        )

        for (index in 0 until AGENT_TURN_BUDGET) {
            var step = try {
                autonomous.stepDetailed()
            } catch (e: Exception) {
                val forcedMethod = fallbackWorldSteps.removeFirstOrNull() ?: "commitWorldSetup"
                listener.onWorldBuildingStep(
                    "recover",
                    "Recovered from invalid DM world step (${e.message?.take(120)}); forcing '$forcedMethod'."
                )
                autonomous.invoke(forcedMethod)
            }

            if (!step.isTerminal && completedWorldSteps.contains(step.methodName)) {
                val forcedMethod = fallbackWorldSteps.firstOrNull { it !in completedWorldSteps } ?: "commitWorldSetup"
                listener.onWorldBuildingStep(
                    "recover",
                    "Detected repeated world-build step '${step.methodName}'; forcing '$forcedMethod'."
                )
                step = autonomous.invoke(forcedMethod)
            }

            completedWorldSteps.add(step.methodName)
            val detail = step.value?.toString()?.take(200)?.ifBlank { "No details" } ?: "No details"
            listener.onWorldBuildingStep(step.methodName, detail)
            finalStep = step
            if (step.isTerminal) {
                break
            }
        }

        val resolved = finalStep?.takeIf { it.isTerminal }
            ?: run {
                listener.onWorldBuildingStep(
                    "recover",
                    "Budget exhausted without terminal setup; forcing 'commitWorldSetup'."
                )
                autonomous.invoke("commitWorldSetup")
            }

        val worldBuildResult = resolved.value as? WorldBuildResult
        if (worldBuildResult == null) {
            listener.onWorldBuildingStep("fallback", "World setup returned non-structured output; using default opening scene.")
            return dm.describeScene().get()
        }

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

        listener.onWorldBuildingStep("commit", "World setup committed with ${mergedLore.npcs.size} NPC(s) and ${mergedLore.locations.size} location node(s).")

        return worldBuildResult.openingScene.takeIf { it.narrative.isNotBlank() }
            ?: dm.describeScene().get()
    }

    private suspend fun runGameLoop() {
        while (world.party.any { it.hp > 0 } && world.round < MAX_TURNS) {
            world = world.copy(round = world.round + 1)
            listener.onRoundStarted(world.round, world)

            processRound()

            if (world.party.all { it.hp <= 0 }) {
                listener.onGameOver(world)
                return
            }

            val summary = dm.narrateRoundSummary().get()
            listener.onRoundSummary(summary, world)
        }

        listener.onGameOver(world)
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

            val result = dm.processAction(currentStats.name, action).get()
            val finalResult = handleDiceRoll(currentStats, result)

            world = applyResult(world, finalResult, currentStats.name)
            world = applyPlayerStateUpdate(world, currentStats.name, playerAction)
            val logEntry = "Round ${world.round}: ${currentStats.name} — $action → " +
                if (finalResult.success) "success" else "failure"
            world = world.copy(
                actionLog = (world.actionLog + logEntry).takeLast(20)
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
        turnStates[character.name] = TurnState(
            phase = "OBSERVE",
            stepsUsed = 0,
            stepsBudget = AGENT_TURN_BUDGET,
            recentSteps = turnHistory
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
                    recentSteps = turnHistory
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
                        agent.invoke("commitAction")
                    }
                    turnHistory += "${stepResult.methodName}: ${summarizeStepValue(stepResult.value)}"
                    currentExcludedMethods.add(stepResult.methodName)
                    turnStates[character.name] = TurnState(
                        phase = if (stepResult.isTerminal) "DONE" else "PLAN",
                        stepsUsed = stepIndex,
                        stepsBudget = AGENT_TURN_BUDGET,
                        recentSteps = turnHistory
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

                val fallbackTerminal = agent.invoke("commitAction")
                turnHistory += "${fallbackTerminal.methodName}: ${summarizeStepValue(fallbackTerminal.value)}"
                turnStates[character.name] = TurnState(
                    phase = if (fallbackTerminal.isTerminal) "DONE" else "PLAN",
                    stepsUsed = AGENT_TURN_BUDGET,
                    stepsBudget = AGENT_TURN_BUDGET,
                    recentSteps = turnHistory
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
            recentSteps = turnHistory
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
                addInterceptor(WorldStateInterceptor { this@GameSession.world })
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
                addInterceptor(WorldStateInterceptor { this@GameSession.world })
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
            questLog = newQuestLog
        )
    }

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
}
