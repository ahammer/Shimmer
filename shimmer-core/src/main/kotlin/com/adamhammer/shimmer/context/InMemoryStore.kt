package com.adamhammer.shimmer.context

import com.adamhammer.shimmer.interfaces.MemoryStore
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [MemoryStore].
 * Backed by a [ConcurrentHashMap] for concurrent access across coroutines.
 */
class InMemoryStore(initial: Map<String, String> = emptyMap()) : MemoryStore {
    private val map = ConcurrentHashMap<String, String>(initial)

    override suspend fun get(key: String): String? = map[key]

    override suspend fun put(key: String, value: String) {
        map[key] = value
    }

    override suspend fun getAll(): Map<String, String> = map.toMap()
}
