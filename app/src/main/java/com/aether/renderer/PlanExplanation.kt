package com.aether.renderer

data class PlanExplanationState(
    val summary: String = "Waiting for a stable state.",
    val why: String = "No strategic state yet.",
    val safety: String = "Telemetry-only mode is active.",
    val next: String = "Observe safely.",
    val readableLine: String = "plan=observe score=0.00"
) {
    val compactLine: String
        get() = UiReadability.short(readableLine, 72)
}

object PlanExplanation {
    fun build(state: PhysicsExperienceState, intent: StrategicIntentState, evaluation: PlanEvaluationState): PlanExplanationState {
        val best = evaluation.best
        val plan = best.candidate
        val summary = "Best plan: ${plan.label}"
        val why = "intent=${intent.goal}, ctx=${UiReadability.shortContext(state.contextLabel)}, confidence=${state.confidence.fmt2()}, risk=${state.risk.fmt2()}"
        val safety = when {
            !state.telemetryOnly -> "Blocked: telemetry-only is not true."
            state.autoControlEnabled -> "Blocked: auto-control must stay false."
            plan.skill.permission == SkillPermission.FORBIDDEN -> "Plan is held because execution is forbidden."
            plan.requiresApproval -> "Plan is proposal-only and requires user approval."
            else -> "Plan is telemetry/proposal only; no action is executed."
        }
        val next = when (plan.id) {
            "observe" -> "Keep observing until context changes."
            "explain" -> "Show reason and keep state readable."
            "suggest" -> "Surface recommendation without acting."
            "privacy_review" -> "Review privacy boundary before any future skill."
            "safe_tap_proposal" -> "Draft only; wait for approval in future skill layer."
            "no_action" -> "Hold position and do nothing."
            else -> "Keep plan in proposal mode."
        }
        return PlanExplanationState(
            summary = summary,
            why = why,
            safety = safety,
            next = next,
            readableLine = "plan=${plan.label} score=${best.score.fmt2()} safety=${plan.skill.permission.name}"
        )
    }
}
