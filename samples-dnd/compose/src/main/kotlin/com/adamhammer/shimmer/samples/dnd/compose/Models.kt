package com.adamhammer.shimmer.samples.dnd.compose

import com.openai.models.ImageGenerateParams
import com.openai.models.ImageModel

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
    val genre: String = "",
    val premise: String = "",
    val partySize: Int = 2,
    val maxRounds: Int = 3,
    val enableImages: Boolean = true,
    val artStyle: String = "Anime",
    val imageModel: ImageModel = ImageModel.DALL_E_3,
    val imageSize: ImageGenerateParams.Size = ImageGenerateParams.Size._1024X1024,
    val drafts: List<CharacterDraft> = List(2) { CharacterDraft() }
)
