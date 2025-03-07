package com.adamhammer.ai_shimmer.annotations

import kotlin.reflect.KClass

/**
 * Extended annotations for a unified AI-driven API metadata interface.
 *
 * These annotations replace Swagger and offer a consistent way to:
 * - Mark methods for caching/memorization.
 * - Indicate subscription and publication channels.
 * - Describe operations, responses, parameters, and data schemas.
 */

/**
 * Indicates that the result of a method should be cached or memorized.
 *
 * @param label A label to identify the cached result.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Memorize(val label: String)

/**
 * Indicates that a method, field, or parameter subscribes to a given channel.
 *
 * @param channel The channel to subscribe to.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(val channel: String)

/**
 * Indicates that a method, field, or parameter publishes to a given channel.
 *
 * @param channel The channel to publish to.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Publish(val channel: String)

/**
 * Describes an API operation in a unified manner.
 *
 * @param summary A short summary of what the operation does.
 * @param description A more detailed description of the operation.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiOperation(
    val summary: String = "",
    val description: String = ""
)

/**
 * Describes the expected response from an API operation.
 *
 * @param description A description of the response.
 * @param responseClass The class representing the response type.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiResponse(
    val description: String = "",
    val responseClass: KClass<*> = Unit::class
)

/**
 * Describes an API parameter in a unified manner.
 *
 * @param description A description of the parameter.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiParameter(
    val description: String = ""
)

/**
 * Provides metadata for a data structure or schema.
 *
 * @param title A title for the schema.
 * @param description A detailed description of the schema.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiSchema(
    val title: String = "",
    val description: String = ""
)
