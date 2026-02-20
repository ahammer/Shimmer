package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.interfaces.Interceptor
import com.adamhammer.shimmer.model.PromptContext

data class TurnState(
    val phase: String = "PLAN",
    val stepsUsed: Int = 0,
    val stepsBudget: Int = 0,
    val recentSteps: List<String> = emptyList(),
    val previousRoundActions: List<String> = emptyList()
)

class TurnStateInterceptor(
    private val turnStateProvider: () -> TurnState
) : Interceptor {

    override fun intercept(context: PromptContext): PromptContext {
        val state = turnStateProvider()
        val stepsRemaining = (state.stepsBudget - state.stepsUsed).coerceAtLeast(0)
        val bannedSection = if (state.previousRoundActions.isNotEmpty()) {
            """|
                |## BANNED — Do NOT Repeat These Actions
                |You already did these in recent rounds. Pick something DIFFERENT:
                |${state.previousRoundActions.joinToString("\n") { "- $it" }}"""
        } else ""
        return context.copy(
            systemInstructions = context.systemInstructions + """
                |
                |## Turn Progress
                |- Phase: ${state.phase}
                |- Steps used: ${state.stepsUsed}/${state.stepsBudget}
                |- Steps remaining: $stepsRemaining
                |- Observation has already been collected for this turn.
                $bannedSection
                |
                |## Steps So Far
                |${state.recentSteps.takeLast(8).joinToString("\n") { "- $it" }.ifBlank { "- No steps yet." }}
            """.trimMargin()
        )
    }
}
