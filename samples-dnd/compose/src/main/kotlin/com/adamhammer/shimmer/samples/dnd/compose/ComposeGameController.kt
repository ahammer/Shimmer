package com.adamhammer.shimmer.samples.dnd.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adamhammer.shimmer.adapters.OpenAiAdapter
import com.adamhammer.shimmer.model.ImageResult
import com.adamhammer.shimmer.samples.dnd.CharacterUtils
import com.adamhammer.shimmer.samples.dnd.DungeonMasterAPI
import com.adamhammer.shimmer.samples.dnd.api.GameEventListener
import com.adamhammer.shimmer.samples.dnd.api.GameSession
import com.adamhammer.shimmer.samples.dnd.model.AbilityScores
import com.adamhammer.shimmer.samples.dnd.model.ActionResult
import com.adamhammer.shimmer.samples.dnd.model.BackstoryResult
import com.adamhammer.shimmer.samples.dnd.model.Character
import com.adamhammer.shimmer.samples.dnd.model.CharacterConcept
import com.adamhammer.shimmer.samples.dnd.model.DiceRollRequest
import com.adamhammer.shimmer.samples.dnd.model.Location
import com.adamhammer.shimmer.samples.dnd.model.SceneDescription
import com.adamhammer.shimmer.samples.dnd.model.World
import com.adamhammer.shimmer.shimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppScreen {
    SETUP,
    PLAYING,
    GAME_OVER
}

data class CharacterDraft(
    val name: String = "",
    val race: String = "",
    val characterClass: String = "",
    val manualBackstory: Boolean = false,
    val backstory: String = ""
)

data class SetupState(
    val partySize: Int = 2,
    val drafts: List<CharacterDraft> = List(2) { CharacterDraft() }
)

enum class StoryEventType {
    SYSTEM,
    ROUND,
    SCENE,
    TURN,
    AGENT_STEP,
    ACTION,
    ROLL,
    SUMMARY,
    ERROR
}

data class StoryEvent(
    val type: StoryEventType,
    val title: String,
    val details: String
)

class ComposeGameController(private val uiScope: CoroutineScope) : GameEventListener {

    private val adapter = OpenAiAdapter()

    var screen by mutableStateOf(AppScreen.SETUP)
        private set

    var setupState by mutableStateOf(SetupState())
        private set

    var world by mutableStateOf(World())
        private set

    var currentScene by mutableStateOf(SceneDescription())
        private set

    var currentImage by mutableStateOf<ImageResult?>(null)
        private set

    var currentRound by mutableStateOf(0)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var activeTurnCharacterName by mutableStateOf<String?>(null)
        private set

    var latestSummary by mutableStateOf<String?>(null)
        private set

    var chapterObjective by mutableStateOf("Form the party and take the first meaningful risk.")
        private set

    var dramaticQuestion by mutableStateOf("What threat is really shaping this place?")
        private set

    var momentumLabel by mutableStateOf("Opening")
        private set

    val timeline = mutableStateListOf<StoryEvent>()

    fun updatePartySize(size: Int) {
        val clamped = size.coerceIn(1, 4)
        val existing = setupState.drafts
        val resized = buildList {
            repeat(clamped) { index ->
                add(existing.getOrNull(index) ?: CharacterDraft())
            }
        }
        setupState = setupState.copy(partySize = clamped, drafts = resized)
    }

    fun updateDraft(index: Int, transform: (CharacterDraft) -> CharacterDraft) {
        val updated = setupState.drafts.toMutableList()
        updated[index] = transform(updated[index])
        setupState = setupState.copy(drafts = updated)
    }

    fun startGame() {
        if (isBusy) return
        isBusy = true
        errorMessage = null
        timeline.clear()
        latestSummary = null

        uiScope.launch(Dispatchers.IO) {
            try {
                val initialWorld = buildInitialWorld(setupState)
                val newSession = GameSession(adapter, this@ComposeGameController)
                uiScope.launch {
                    world = initialWorld
                    currentRound = 0
                    screen = AppScreen.PLAYING
                    refreshStorySignals(initialWorld)
                    appendEvent(
                        StoryEventType.SYSTEM,
                        "The party gathers",
                        "${initialWorld.party.size} adventurers set out from ${initialWorld.location.name}."
                    )
                }
                newSession.startGame(initialWorld)
            } catch (e: Exception) {
                uiScope.launch {
                    errorMessage = normalizeError(e.message)
                    appendEvent(
                        StoryEventType.ERROR,
                        "Unable to start adventure",
                        errorMessage ?: "Unknown startup error"
                    )
                    isBusy = false
                    screen = AppScreen.SETUP
                }
            }
        }
    }

    fun backToSetup() {
        activeTurnCharacterName = null
        isBusy = false
        screen = AppScreen.SETUP
    }

    override fun onRoundStarted(round: Int, world: World) {
        uiScope.launch {
            currentRound = round
            this@ComposeGameController.world = world
            refreshStorySignals(world)
            appendEvent(StoryEventType.ROUND, "Round $round", "A new beat begins in ${world.location.name}.")
        }
    }

    override fun onWorldBuildingStep(step: String, details: String) {
        uiScope.launch {
            appendEvent(StoryEventType.SYSTEM, "World build: $step", details)
            if (step == "start") {
                chapterObjective = "Establish a coherent world, then launch the opening scene."
                momentumLabel = "Setup"
            }
        }
    }

    override fun onAgentStep(
        characterName: String,
        stepNumber: Int,
        methodName: String,
        details: String,
        terminal: Boolean,
        pointsSpent: Int,
        pointsRemaining: Int
    ) {
        uiScope.launch {
            val terminalTag = if (terminal) "terminal" else "in-progress"
            appendEvent(
                StoryEventType.AGENT_STEP,
                "$characterName step $stepNumber · $methodName",
                "MP $pointsSpent/5 (left $pointsRemaining) · $details ($terminalTag)"
            )
        }
    }

    override fun onSceneDescription(scene: SceneDescription) {
        uiScope.launch {
            currentScene = scene
            currentImage = null
            appendEvent(StoryEventType.SCENE, world.location.name, scene.narrative)
        }
    }

    override fun onImageGenerated(image: ImageResult) {
        uiScope.launch {
            currentImage = image
        }
    }

    override fun onActionResult(result: ActionResult, world: World) {
        uiScope.launch {
            this@ComposeGameController.world = world
            refreshStorySignals(world)
            appendEvent(
                StoryEventType.ACTION,
                if (result.success) "Action succeeds" else "Action falters",
                result.narrative
            )
        }
    }

    override fun onDiceRollRequested(character: Character, request: DiceRollRequest) {
        uiScope.launch {
            appendEvent(
                StoryEventType.ROLL,
                "${character.name} prepares a roll",
                "${request.rollType} vs DC ${request.difficulty}"
            )
        }
    }

    override fun onRoundSummary(summary: SceneDescription, world: World) {
        uiScope.launch {
            this@ComposeGameController.world = world
            currentScene = summary
            currentImage = null
            latestSummary = summary.narrative
            refreshStorySignals(world)
            appendEvent(
                StoryEventType.SUMMARY,
                "Round ${world.round} summary",
                summary.narrative
            )
        }
    }

    override fun onGameOver(world: World) {
        uiScope.launch {
            this@ComposeGameController.world = world
            isBusy = false
            screen = AppScreen.GAME_OVER
            activeTurnCharacterName = null
            appendEvent(StoryEventType.SYSTEM, "Adventure ended", "The chapter closes for this party.")
        }
    }

    override fun onCharacterThinking(characterName: String) {
        uiScope.launch {
            activeTurnCharacterName = characterName
            appendEvent(StoryEventType.TURN, "$characterName is thinking", "Plotting the next move.")
            isBusy = true
        }
    }

    override fun onCharacterAction(characterName: String, action: String) {
        uiScope.launch {
            appendEvent(StoryEventType.ACTION, "$characterName acts", action)
            activeTurnCharacterName = null
            isBusy = false
        }
    }

    private fun appendEvent(type: StoryEventType, title: String, details: String) {
        timeline.add(StoryEvent(type, title, details))
        if (timeline.size > 300) {
            timeline.removeFirst()
        }
    }

    private fun refreshStorySignals(world: World) {
        val latestQuest = world.questLog.lastOrNull()
        val latestAction = world.actionLog.lastOrNull()

        chapterObjective = when {
            !latestQuest.isNullOrBlank() -> latestQuest
            world.round <= 1 -> "Establish intent at ${world.location.name}."
            else -> "Shift the balance in ${world.location.name} this round."
        }

        dramaticQuestion = when {
            !latestAction.isNullOrBlank() && latestAction.contains("failure", ignoreCase = true) ->
                "Can the party recover from setbacks before the world closes in?"
            world.party.any { it.hp <= (it.maxHp / 3).coerceAtLeast(1) } ->
                "Can the wounded survive long enough to turn the scene around?"
            world.questLog.isNotEmpty() ->
                "What cost will this objective demand from the party?"
            else ->
                "Who or what is about to interrupt this plan?"
        }

        val lowHpCount = world.party.count { it.hp <= (it.maxHp / 2).coerceAtLeast(1) }
        val recentFailures = world.actionLog.takeLast(6).count { it.contains("failure", ignoreCase = true) }

        momentumLabel = when {
            world.round <= 1 -> "Opening"
            lowHpCount >= 2 || recentFailures >= 3 -> "Dire"
            recentFailures >= 1 -> "Unsteady"
            world.questLog.isNotEmpty() -> "Driven"
            else -> "Rising"
        }
    }

    private fun normalizeError(message: String?): String {
        val text = message?.trim().orEmpty()
        return when {
            text.contains("OPENAI_API_KEY", ignoreCase = true) ->
                "OPENAI_API_KEY is missing. Set it before starting the Compose adventure."
            text.isBlank() -> "Failed to start game"
            else -> text
        }
    }

    private fun buildInitialWorld(state: SetupState): World {
        val backstoryDm = shimmer<DungeonMasterAPI> {
            adapter(adapter)
            resilience { maxRetries = 1 }
        }.api

        val party = mutableListOf<Character>()
        state.drafts.forEachIndexed { index, draft ->
            val character = createCharacterFromDraft(backstoryDm, draft, index + 1, party)
            party.add(character)
        }

        return World(
            party = party,
            location = Location(),
            round = 0
        )
    }

    private fun createCharacterFromDraft(
        backstoryDm: DungeonMasterAPI,
        draft: CharacterDraft,
        index: Int,
        existingParty: List<Character>
    ): Character {
        val concept = if (
            draft.name.isBlank() || draft.race.isBlank() || draft.characterClass.isBlank()
        ) {
            val existingMembers = existingParty.joinToString(", ") {
                "${it.name} the ${it.race} ${it.characterClass}"
            }.ifEmpty { "none yet" }
            backstoryDm.generateCharacterConcept(
                "EXISTING PARTY: [$existingMembers]. GENERATE: name, race, class."
            ).get()
        } else {
            CharacterConcept(draft.name, draft.race, draft.characterClass)
        }

        val name = draft.name.ifBlank { concept.name.ifBlank { "Hero$index" } }
        val race = draft.race.ifBlank { concept.race.ifBlank { "Human" } }
        val clazz = draft.characterClass.ifBlank { concept.characterClass.ifBlank { "Fighter" } }

        val backstoryResult = if (draft.manualBackstory && draft.backstory.isNotBlank()) {
            BackstoryResult(
                backstory = draft.backstory,
                suggestedAbilityScores = CharacterUtils.autoAssignScores(clazz),
                startingItems = emptyList()
            )
        } else {
            backstoryDm.generateBackstory(name, race, clazz).get()
        }

        val scores = backstoryResult.suggestedAbilityScores.takeIf { it != AbilityScores() }
            ?: CharacterUtils.autoAssignScores(clazz)

        return CharacterUtils.buildCharacter(
            name = name,
            race = race,
            characterClass = clazz,
            abilityScores = scores,
            backstory = backstoryResult.backstory,
            aiSuggestedItems = backstoryResult.startingItems
        )
    }
}