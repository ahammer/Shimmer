package com.adamhammer.ai_shimmer.interfaces

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a request that will go to AI for processing
 */
@Serializable
data class SerializableRequest(
    val method: String,
    val parameters: Collection<Map<String, JsonElement>>,
    val memory: Map<String, String>,
)