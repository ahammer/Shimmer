package com.adamhammer.shimmer.interfaces

import kotlin.reflect.KClass

/**
 * Bridges an external POJO type [P] to a Shimmer-native mirror type [M].
 *
 * The mirror type must be a `@Serializable` data class annotated with `@AiSchema`
 * so that Shimmer's schema generation and serialization pipeline works unchanged.
 *
 * Register adapters via `ShimmerBuilder.typeAdapter(...)`. The Shimmer proxy will
 * transparently swap POJO types for their mirrors before the AI call and convert
 * results back after.
 *
 * @param P the external POJO type that cannot be annotated directly
 * @param M the mirror `@Serializable` + `@AiSchema` data class
 */
interface TypeAdapter<P : Any, M : Any> {
    /** The external POJO class. */
    val pojoClass: KClass<P>

    /** The mirror class — must be `@Serializable` and annotated with `@AiSchema`. */
    val mirrorClass: KClass<M>

    /** Convert a POJO instance to its mirror representation. */
    fun toMirror(pojo: P): M

    /** Convert a mirror instance back to the POJO. */
    fun fromMirror(mirror: M): P
}
