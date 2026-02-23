package ca.adamhammer.shimmer.samples.dnd

import ca.adamhammer.shimmer.interfaces.Interceptor
import ca.adamhammer.shimmer.model.PromptContext

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
        val recentActionsSection = if (state.previousRoundActions.isNotEmpty()) {
            """|
                |## Your Recent Actions
                |Here is what you did in recent rounds:
                |${state.previousRoundActions.joinToString("\n") { "- $it" }}
                |The world has changed since then. Consider: What's different now?
                |Look for new opportunities, react to new events, or build on what you learned.
                |Repeating the same action will have diminishing returns — the DM will escalate consequences."""
        } else ""
        return context.copy(
            systemInstructions = context.systemInstructions + """
                |
                |## Turn Progress
                |- Phase: ${state.phase}
                |- Steps used: ${state.stepsUsed}/${state.stepsBudget}
                |- Steps remaining: $stepsRemaining
                |- Observation has already been collected for this turn.
                $recentActionsSection
                |
                |## Steps So Far
                |${state.recentSteps.takeLast(8).joinToString("\n") { "- $it" }.ifBlank { "- No steps yet." }}
            """.trimMargin()
        )
    }
}
