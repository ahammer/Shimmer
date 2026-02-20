package com.adamhammer.shimmer.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageResult(
    val base64: String = "",
    val prompt: String = "",
    val revisedPrompt: String = ""
)