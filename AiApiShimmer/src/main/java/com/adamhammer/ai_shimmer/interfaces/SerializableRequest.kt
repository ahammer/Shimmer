package com.adamhammer.ai_shimmer.interfaces

import kotlinx.serialization.Serializable

/**
 * Represents a request that will go to AI for processing
 */
@Serializable
data class SerializableRequest(
    val method: String,
    val parameters: Collection<Map<String, String>>,
    val memory: Map<String, String>,
)