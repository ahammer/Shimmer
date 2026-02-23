package ca.adamhammer.shimmer.model

/**
 * Wraps an adapter result together with optional token-usage metadata.
 *
 * Returned by [ca.adamhammer.shimmer.interfaces.ApiAdapter.handleRequestWithUsage].
 */
data class AdapterResponse<R>(
    val result: R,
    val usage: UsageInfo? = null
)
