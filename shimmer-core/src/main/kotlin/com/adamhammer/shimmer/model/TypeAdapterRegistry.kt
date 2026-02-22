package com.adamhammer.shimmer.model

import com.adamhammer.shimmer.interfaces.TypeAdapter
import kotlin.reflect.KClass

/**
 * Registry of [TypeAdapter]s keyed by their POJO class.
 *
 * Used internally by the Shimmer proxy to transparently bridge external POJO
 * types to their `@Serializable` mirror counterparts before schema generation,
 * serialization, and deserialization.
 */
class TypeAdapterRegistry {
    private val adapters = mutableMapOf<KClass<*>, TypeAdapter<*, *>>()

    /** Register a type adapter. Replaces any previously registered adapter for the same POJO class. */
    fun register(adapter: TypeAdapter<*, *>) {
        adapters[adapter.pojoClass] = adapter
    }

    /** Look up the adapter for a given class, or null if none is registered. */
    fun resolve(klass: KClass<*>): TypeAdapter<*, *>? = adapters[klass]

    /** Returns true if an adapter is registered for [klass]. */
    fun hasAdapter(klass: KClass<*>): Boolean = klass in adapters

    /**
     * Returns the mirror class if an adapter is registered for [klass],
     * otherwise returns [klass] unchanged.
     */
    fun mirrorClassFor(klass: KClass<*>): KClass<*> =
        adapters[klass]?.mirrorClass ?: klass

    /**
     * If [value]'s class has a registered adapter, converts it to the mirror type.
     * Otherwise returns [value] unchanged.
     */
    fun convertArg(value: Any): Any {
        val adapter = adapters[value::class] ?: return value
        @Suppress("UNCHECKED_CAST")
        return (adapter as TypeAdapter<Any, Any>).toMirror(value)
    }

    /**
     * Converts a mirror result back to the original POJO type.
     *
     * @param mirror the mirror instance returned by the AI pipeline
     * @param pojoClass the original POJO class the caller expects
     * @return the converted POJO, or [mirror] unchanged if no adapter is registered
     */
    fun <R : Any> convertResult(mirror: Any, pojoClass: KClass<R>): R {
        val adapter = adapters[pojoClass]
        if (adapter != null) {
            @Suppress("UNCHECKED_CAST")
            return (adapter as TypeAdapter<R, Any>).fromMirror(mirror)
        }
        @Suppress("UNCHECKED_CAST")
        return mirror as R
    }

    /** Returns true if no adapters are registered. */
    fun isEmpty(): Boolean = adapters.isEmpty()
}
