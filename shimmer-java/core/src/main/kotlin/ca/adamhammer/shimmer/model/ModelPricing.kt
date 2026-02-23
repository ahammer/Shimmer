package ca.adamhammer.shimmer.model

/**
 * Per-token pricing for a model, used to estimate costs in [UsageInfo].
 *
 * Pass to vendor adapter constructors to enable automatic cost estimation.
 * When not configured, both values default to zero.
 */
data class ModelPricing(
    val inputCostPerToken: Double = 0.0,
    val outputCostPerToken: Double = 0.0
)
