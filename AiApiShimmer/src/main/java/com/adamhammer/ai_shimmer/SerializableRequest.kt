package com.adamhammer.ai_shimmer

import kotlinx.serialization.Serializable

@Serializable
data class SerializableRequest(
    val method: String,
    val parameters: Collection<Map<String, String>>,
    val memory: Map<String, String>,
    val resultSchema: String,
)