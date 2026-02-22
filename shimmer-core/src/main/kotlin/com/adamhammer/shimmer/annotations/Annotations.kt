package com.adamhammer.shimmer.annotations

import kotlin.reflect.KClass

/**
 * Marks a method to have its result stored in the shared memory map.
 * The stored result will be passed to subsequent AI calls.
 *
 * @param label The key under which the result will be stored in memory.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Memorize(val label: String)

/**
 * Describes an AI operation. Used to provide context to the AI about what the method does.
 *
 * @param summary A short summary of the operation.
 * @param description A detailed description of the operation.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiOperation(
    val summary: String = "",
    val description: String = ""
)

/**
 * Describes the expected response from an AI operation.
 *
 * @param description A description of the expected response.
 * @param responseClass The expected class of the response. If not provided, the method's return type is used.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiResponse(
    val description: String = "",
    val responseClass: KClass<*> = Unit::class
)

/**
 * Describes a parameter of an AI operation.
 *
 * @param description A description of the parameter.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiParameter(
    val description: String = ""
)

/**
 * Provides metadata for data structure schemas.
 *
 * @param title The title of the schema.
 * @param description A description of the schema.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class AiSchema(
    val title: String = "",
    val description: String = ""
)

/**
 * Marks a method as a terminal operation in an agent workflow.
 * When an agent invokes a method with this annotation, it will stop its execution loop.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Terminal
