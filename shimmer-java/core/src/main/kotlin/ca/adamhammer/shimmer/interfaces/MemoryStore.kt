package ca.adamhammer.shimmer.interfaces

/**
 * A store for persisting results across AI calls.
 * Used by the `@Memorize` annotation to store and retrieve context.
 */
interface MemoryStore {
    /** Retrieves a value by key. */
    suspend fun get(key: String): String?
    
    /** Stores a value by key. */
    suspend fun put(key: String, value: String)
    
    /** Retrieves all stored key-value pairs. */
    suspend fun getAll(): Map<String, String>
}
