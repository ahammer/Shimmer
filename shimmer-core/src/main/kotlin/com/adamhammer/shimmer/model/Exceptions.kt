package com.adamhammer.shimmer.model

open class ShimmerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class ShimmerTimeoutException(message: String, cause: Throwable? = null) : ShimmerException(message, cause)
class ResultValidationException(message: String) : ShimmerException(message)
class ShimmerConfigurationException(message: String, cause: Throwable? = null) : ShimmerException(message, cause)
class ShimmerDeserializationException(message: String, cause: Throwable? = null) : ShimmerException(message, cause)
