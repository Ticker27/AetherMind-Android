package com.aether.renderer

data class PlanCandidate(
    val id: String,
    val label: String,
    val skill: SkillCard,
    val expectedBenefit: Float,
    val expectedRisk: Float,
    val requiresApproval: Boolean,
    val telemetryOnly: Boolean = true,
    val reason: String = "candidate"
) {
    val compactLine: String
        get() = "$label b=${expectedBenefit.fmt2()} r=${expectedRisk.fmt2()} ${skill.permission.name}"
}

object PlannerCandidateGenerator {
    fun generate(state: PhysicsExperienceState, intent: StrategicIntentState): List<PlanCandidate> {
        val candidates = ArrayList<PlanCandidate>(8)
        candidates += PlanCandidate(
            id = "observe",
            label = "observe_more",
            skill = SkillBoundary.observeScreen,
            expectedBenefit = if (state.observerActive) 0.45f else 0.28f,
            expectedRisk = 0.02f,
            requiresApproval = false,
            reason = "baseline_safe_observe"
        )
        candidates += PlanCandidate(
            id = "explain",
            label = "explain_current_state",
            skill = SkillBoundary.explainState,
            expectedBenefit = (0.36f + state.confidence * 0.32f).coerceIn(0.0f, 0.82f),
            expectedRisk = 0.02f,
            requiresApproval = false,
            reason = "make_reason_visible"
        )
        candidates += PlanCandidate(
            id = "suggest",
            label = when (intent.type) {
                StrategicIntentType.REDUCE_RISK -> "suggest_risk_reduction"
                StrategicIntentType.REVIEW_PRIVACY -> "suggest_privacy_review"
                StrategicIntentType.SEEK_CLARITY -> "suggest_more_context"
                StrategicIntentType.STABILIZE_CONTEXT -> "suggest_wait_stable"
                StrategicIntentType.MAINTAIN_READY -> "suggest_ready_next_step"
                StrategicIntentType.OBSERVE -> "suggest_observation_only"
            },
            skill = SkillBoundary.suggestPlan,
            expectedBenefit = (0.42f + intent.confidence * 0.28f + state.stateStability * 0.18f).coerceIn(0.0f, 0.90f),
            expectedRisk = (state.risk * 0.28f + state.privacyMode.ordinal * 0.04f).coerceIn(0.02f, 0.36f),
            requiresApproval = intent.requiresUserApproval,
            reason = "intent_aligned_proposal"
        )
        if (state.privacyMode != PrivacyMode.STANDARD || state.securityMode != SecurityLockMode.OPEN_OBSERVE) {
            candidates += PlanCandidate(
                id = "privacy_review",
                label = "review_privacy_boundary",
                skill = SkillBoundary.suggestPlan.copy(id = "PrivacyReview"),
                expectedBenefit = 0.76f,
                expectedRisk = 0.04f,
                requiresApproval = true,
                reason = "privacy_or_security_guard_active"
            )
        }
        if (state.confidence >= 0.70f && state.risk < 0.20f && state.privacyMode == PrivacyMode.STANDARD) {
            candidates += PlanCandidate(
                id = "safe_tap_proposal",
                label = "draft_safe_tap_proposal",
                skill = SkillBoundary.proposeSafeTap,
                expectedBenefit = 0.58f,
                expectedRisk = 0.18f,
                requiresApproval = true,
                reason = "future_skill_proposal_only"
            )
        }
        candidates += PlanCandidate(
            id = "no_action",
            label = "hold_no_action",
            skill = SkillBoundary.externalAction,
            expectedBenefit = if (state.risk >= 0.40f) 0.70f else 0.22f,
            expectedRisk = 0.01f,
            requiresApproval = false,
            reason = "safe_default"
        )
        return candidates.take(8)
    }
}
