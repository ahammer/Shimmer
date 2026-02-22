package com.adamhammer.shimmer.samples.dnd.compose

enum class AppScreen {
    SETUP,
    PLAYING,
    GAME_OVER
}

data class ModelOption(val id: String, val displayName: String)

enum class Vendor(
    val displayName: String,
    val envVarName: String,
    val supportsImages: Boolean,
    val models: List<ModelOption>,
    val defaultModel: String
) {
    OPENAI(
        displayName = "OpenAI",
        envVarName = "OPENAI_API_KEY",
        supportsImages = true,
        models = listOf(
            ModelOption("gpt-5-nano", "GPT-5 Nano"),
            ModelOption("gpt-5-mini", "GPT-5 Mini"),
            ModelOption("gpt-5.2", "GPT-5.2"),
            ModelOption("gpt-4.1", "GPT-4.1")
        ),
        defaultModel = "gpt-5-mini"
    ),
    ANTHROPIC(
        displayName = "Anthropic",
        envVarName = "ANTHROPIC_API_KEY",
        supportsImages = false,
        models = listOf(
            ModelOption("claude-haiku-4-5", "Claude Haiku 4.5"),
            ModelOption("claude-sonnet-4-6", "Claude Sonnet 4.6"),
            ModelOption("claude-opus-4-6", "Claude Opus 4.6")
        ),
        defaultModel = "claude-sonnet-4-6"
    ),
    GEMINI(
        displayName = "Google Gemini",
        envVarName = "GEMINI_API_KEY",
        supportsImages = true,
        models = listOf(
            ModelOption("gemini-2.5-flash", "Gemini 2.5 Flash"),
            ModelOption("gemini-2.5-pro", "Gemini 2.5 Pro"),
            ModelOption("gemini-3-flash-preview", "Gemini 3 Flash (Preview)")
        ),
        defaultModel = "gemini-2.5-flash"
    );

    fun isAvailable(): Boolean =
        System.getenv(envVarName) != null ||
            (this == GEMINI && System.getenv("GOOGLE_API_KEY") != null)
}

data class CharacterDraft(
    val name: String = "",
    val race: String = "",
    val characterClass: String = "",
    val manualBackstory: Boolean = false,
    val backstory: String = ""
)

data class SetupState(
    val genre: String = "",
    val premise: String = "",
    val partySize: Int = 2,
    val maxRounds: Int = 3,
    val enableImages: Boolean = true,
    val artStyle: String = "Anime",
    val textVendor: Vendor = Vendor.OPENAI,
    val textModel: String = Vendor.OPENAI.defaultModel,
    val imageVendor: Vendor = Vendor.OPENAI,
    val drafts: List<CharacterDraft> = List(2) { CharacterDraft() }
)
