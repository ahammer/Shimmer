@file:Suppress("TooManyFunctions")
package ca.adamhammer.shimmer.samples.dnd.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ca.adamhammer.shimmer.UsageTracker
import ca.adamhammer.shimmer.adapters.ClaudeAdapter
import ca.adamhammer.shimmer.adapters.GeminiAdapter
import ca.adamhammer.shimmer.adapters.OpenAiAdapter
import ca.adamhammer.shimmer.adapters.RoutingAdapter
import ca.adamhammer.shimmer.interfaces.ApiAdapter
import ca.adamhammer.shimmer.model.ImageResult
import ca.adamhammer.shimmer.samples.dnd.CharacterUtils
import ca.adamhammer.shimmer.samples.dnd.DungeonMasterAPI
import ca.adamhammer.shimmer.samples.dnd.api.GameEventListener
import ca.adamhammer.shimmer.samples.dnd.api.GameSession
import ca.adamhammer.shimmer.samples.dnd.model.*
import ca.adamhammer.shimmer.shimmer
import com.openai.models.ChatModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ComposeGameControllerV2(private val uiScope: CoroutineScope) : GameEventListener {

    var screen by mutableStateOf(AppScreen.SETUP)
        private set

    var setupState by mutableStateOf(SetupState())

    var world by mutableStateOf(World())
        private set

    var currentRound by mutableStateOf(0)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var activeTurnCharacterName by mutableStateOf<String?>(null)
        private set

    val timelineEntries = mutableStateListOf<TimelineEntry>()

    val usageTracker = UsageTracker()

    var showUsagePane by mutableStateOf(false)

    // Track pending action per character so we can merge action + outcome
    private val pendingActions = mutableMapOf<String, TimelineEntry.CharacterAction>()
    // Monotonic counter for stable timeline ordering
    private var timeCounter = 0L
    private fun nextTimestamp(): Long = timeCounter++

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

    fun updateMaxRounds(rounds: Int) {
        setupState = setupState.copy(maxRounds = rounds.coerceIn(3, 100))
    }

    fun randomizePrimer() {
        val genre = BakedGameData.genres.random()
        val selectedCharacters = genre.characters.shuffled().take(setupState.partySize)
        val newDrafts = selectedCharacters.map { baked ->
            CharacterDraft(
                name = baked.name,
                race = baked.race,
                characterClass = baked.characterClass,
                manualBackstory = true,
                backstory = baked.backstory
            )
        }
        setupState = setupState.copy(
            genre = genre.name,
            premise = genre.premise,
            drafts = newDrafts
        )
    }

    fun startGame() {
        if (isBusy) return
        isBusy = true
        errorMessage = null
        timelineEntries.clear()
        pendingActions.clear()
        AvatarColors.reset()
        timeCounter = 0L

        uiScope.launch(Dispatchers.IO) {
            try {
                val textAdapter = createAdapter(setupState.textVendor, setupState.textModel)
                val imageAdapter = createAdapter(setupState.imageVendor, setupState.imageVendor.defaultModel)
                val sessionAdapter = RoutingAdapter { context ->
                    if (context.methodName == "generateImage") imageAdapter else textAdapter
                }
                val initialWorld = buildInitialWorld(setupState, sessionAdapter)
                val session = GameSession(
                    apiAdapter = sessionAdapter,
                    listener = this@ComposeGameControllerV2,
                    maxTurns = setupState.maxRounds,
                    enableImages = setupState.enableImages,
                    artStyle = setupState.artStyle,
                    requestListeners = listOf(usageTracker)
                )
                uiScope.launch {
                    world = initialWorld
                    currentRound = 0
                    screen = AppScreen.PLAYING
                    append(
                        TimelineEntry.SystemMessage(
                            title = "The party gathers",
                            details = "${initialWorld.party.size} adventurers set out from ${initialWorld.location.name}.",
                            timestamp = nextTimestamp()
                        )
                    )
                    // Pre-register avatar colors for all party members
                    initialWorld.party.forEach { AvatarColors.colorFor(it.name) }
                }
                session.startGame(initialWorld)
            } catch (e: Exception) {
                uiScope.launch {
                    errorMessage = normalizeError(e.message)
                    append(
                        TimelineEntry.SystemMessage(
                            title = "Unable to start adventure",
                            details = errorMessage ?: "Unknown startup error",
                            isError = true,
                            timestamp = nextTimestamp()
                        )
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

    // ── GameEventListener ───────────────────────────────────────────────────

    override fun onRoundStarted(round: Int, world: World) {
        uiScope.launch {
            currentRound = round
            this@ComposeGameControllerV2.world = world
            append(
                TimelineEntry.RoundHeader(
                    round = round,
                    locationName = world.location.name,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    override fun onWorldBuildingStep(step: String, details: String) {
        uiScope.launch {
            append(
                TimelineEntry.DmNarration(
                    title = "World: $step",
                    narrative = details,
                    category = TimelineEntry.DmCategory.WORLD_BUILD,
                    timestamp = nextTimestamp()
                )
            )
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
            append(
                TimelineEntry.CharacterThinking(
                    characterName = characterName,
                    stepNumber = stepNumber,
                    methodName = methodName,
                    details = details,
                    isTerminal = terminal,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    override fun onSceneDescription(scene: SceneDescription) {
        uiScope.launch {
            append(
                TimelineEntry.DmNarration(
                    title = world.location.name,
                    narrative = scene.narrative,
                    category = TimelineEntry.DmCategory.SCENE,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    override fun onImageGenerated(image: ImageResult) {
        uiScope.launch {
            if (image.base64.isNotBlank()) {
                append(
                    TimelineEntry.SceneImage(
                        base64 = image.base64,
                        caption = "",
                        timestamp = nextTimestamp()
                    )
                )
            }
        }
    }

    override fun onCharacterThinking(characterName: String) {
        uiScope.launch {
            activeTurnCharacterName = characterName
            isBusy = true
        }
    }

    override fun onCharacterAction(characterName: String, action: String) {
        uiScope.launch {
            // Store as pending — we'll merge outcome when onActionResult fires
            pendingActions[characterName] = TimelineEntry.CharacterAction(
                characterName = characterName,
                action = action,
                timestamp = nextTimestamp()
            )
            activeTurnCharacterName = null
            isBusy = false
        }
    }

    override fun onActionResult(result: ActionResult, world: World) {
        uiScope.launch {
            this@ComposeGameControllerV2.world = world
            // Find and merge with pending character action
            val targetName = result.targetCharacterName.ifBlank {
                // find most recent pending
                pendingActions.keys.lastOrNull() ?: "Unknown"
            }
            val pending = pendingActions.remove(targetName)
                ?: pendingActions.values.lastOrNull()?.also { pendingActions.clear() }

            if (pending != null) {
                append(
                    pending.copy(
                        outcome = result.narrative,
                        success = result.success,
                        timestamp = pending.timestamp
                    )
                )
            } else {
                append(
                    TimelineEntry.CharacterAction(
                        characterName = targetName,
                        action = "(action)",
                        outcome = result.narrative,
                        success = result.success,
                        timestamp = nextTimestamp()
                    )
                )
            }
        }
    }

    override fun onDiceRollRequested(character: Character, request: DiceRollRequest) {
        uiScope.launch {
            append(
                TimelineEntry.DiceRoll(
                    characterName = character.name,
                    rollType = request.rollType,
                    difficulty = request.difficulty,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    override fun onWhisper(fromCharacter: String, toCharacter: String, message: String) {
        uiScope.launch {
            append(
                TimelineEntry.Whisper(
                    from = fromCharacter,
                    to = toCharacter,
                    message = message,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    override fun onRoundSummary(summary: SceneDescription, world: World) {
        uiScope.launch {
            this@ComposeGameControllerV2.world = world
            append(
                TimelineEntry.DmNarration(
                    title = "Round $currentRound Summary",
                    narrative = summary.narrative,
                    category = TimelineEntry.DmCategory.SUMMARY,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    override fun onGameOver(world: World) {
        uiScope.launch {
            this@ComposeGameControllerV2.world = world
            isBusy = false
            activeTurnCharacterName = null
            append(
                TimelineEntry.SystemMessage(
                    title = "Adventure Ended",
                    details = "The chapter closes for this party.",
                    timestamp = nextTimestamp()
                )
            )
            screen = AppScreen.GAME_OVER
        }
    }

    override fun onEndGameSummaryGenerated(reportPath: String) {
        uiScope.launch {
            append(
                TimelineEntry.SystemMessage(
                    title = "Report saved",
                    details = reportPath,
                    timestamp = nextTimestamp()
                )
            )
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun append(entry: TimelineEntry) {
        timelineEntries.add(entry)
        if (timelineEntries.size > 500) {
            timelineEntries.removeFirst()
        }
    }

    private fun createAdapter(vendor: Vendor, model: String): ApiAdapter = when (vendor) {
        Vendor.OPENAI -> OpenAiAdapter(model = ChatModel.of(model))
        Vendor.ANTHROPIC -> ClaudeAdapter(model = model)
        Vendor.GEMINI -> GeminiAdapter(model = model)
    }

    private fun normalizeError(message: String?): String {
        val text = message?.trim().orEmpty()
        return when {
            text.contains("OPENAI_API_KEY", ignoreCase = true) ->
                "OPENAI_API_KEY is missing. Set it before starting."
            text.contains("ANTHROPIC_API_KEY", ignoreCase = true) ->
                "ANTHROPIC_API_KEY is missing. Set it before starting."
            text.contains("GEMINI_API_KEY", ignoreCase = true) ||
                text.contains("GOOGLE_API_KEY", ignoreCase = true) ->
                "GEMINI_API_KEY (or GOOGLE_API_KEY) is missing. Set it before starting."
            text.isBlank() -> "Failed to start game"
            else -> text
        }
    }

    private fun buildInitialWorld(state: SetupState, apiAdapter: ApiAdapter): World {
        val backstoryDm = shimmer<DungeonMasterAPI> {
            adapter(apiAdapter)
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
            round = 0,
            lore = WorldLore(campaignPremise = state.premise)
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
            look = concept.look.ifBlank { "A typical adventurer." },
            race = race,
            characterClass = clazz,
            abilityScores = scores,
            backstory = backstoryResult.backstory,
            aiSuggestedItems = backstoryResult.startingItems
        )
    }
}
