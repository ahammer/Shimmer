package com.adamhammer.shimmer.model

/** Base exception for all Shimmer errors. */
open class ShimmerException(
    message: String,
    cause: Throwable? = null,
    val context: PromptContext? = null
) : RuntimeException(message, cause)

/** Thrown when a request times out. */
class ShimmerTimeoutException(
    message: String,
    cause: Throwable? = null,
    context: PromptContext? = null
) : ShimmerException(message, cause, context)

/** Thrown when a result is rejected by the configured validator. */
class ResultValidationException(
    message: String,
    context: PromptContext? = null
) : ShimmerException(message, null, context)

/** Thrown when the Shimmer builder is configured incorrectly. */
class ShimmerConfigurationException(
    message: String,
    cause: Throwable? = null
) : ShimmerException(message, cause)

/** Thrown when the AI response cannot be deserialized into the expected type. */
class ShimmerDeserializationException(
    message: String,
    cause: Throwable? = null,
    context: PromptContext? = null
) : ShimmerException(message, cause, context)

/** Thrown when a tool invocation fails. */
class ShimmerToolException(
    message: String,
    cause: Throwable? = null,
    context: PromptContext? = null
) : ShimmerException(message, cause, context)

/** Thrown when the underlying AI adapter encounters an error (e.g., HTTP 500). */
class ShimmerAdapterException(
    message: String,
    cause: Throwable? = null,
    context: PromptContext? = null
) : ShimmerException(message, cause, context)
