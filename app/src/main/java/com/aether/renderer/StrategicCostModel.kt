package com.aether.renderer

data class PlanScore(
    val candidate: PlanCandidate = PlanCandidate(
        id = "observe",
        label = "observe_more",
        skill = SkillBoundary.observeScreen,
        expectedBenefit = 0.0f,
        expectedRisk = 0.0f,
        requiresApproval = false
    ),
    val score: Float = 0.0f,
    val safetyPenalty: Float = 0.0f,
    val approvalPenalty: Float = 0.0f,
    val reason: String = "score_waiting"
) {
    val compactLine: String
        get() = "${candidate.label} score=${score.fmt2()} risk=${candidate.expectedRisk.fmt2()}"
}

data class PlanEvaluationState(
    val candidateCount: Int = 0,
    val best: PlanScore = PlanScore(),
    val ranked: List<PlanScore> = emptyList(),
    val reason: String = "evaluation_waiting"
) {
    val badge: String
        get() = when {
            candidateCount <= 0 -> "PLAN_EMPTY"
            best.score >= 0.72f -> "PLAN_STRONG"
            best.score >= 0.48f -> "PLAN_OK"
            else -> "PLAN_WEAK"
        }

    val compactLine: String
        get() = "$badge n=$candidateCount ${best.compactLine}"
}

object StrategicCostModel {
    fun evaluate(state: PhysicsExperienceState, intent: StrategicIntentState, candidates: List<PlanCandidate>): PlanEvaluationState {
        if (candidates.isEmpty()) return PlanEvaluationState()
        val scored = candidates.map { candidate ->
            val safetyPenalty = safetyPenalty(state, candidate)
            val approvalPenalty = if (candidate.requiresApproval) 0.04f else 0.0f
            val intentBonus = when {
                intent.type == StrategicIntentType.REDUCE_RISK && candidate.id in setOf("suggest", "privacy_review", "no_action") -> 0.10f
                intent.type == StrategicIntentType.MAINTAIN_READY && candidate.id in setOf("explain", "suggest") -> 0.08f
                intent.type == StrategicIntentType.SEEK_CLARITY && candidate.id in setOf("observe", "suggest") -> 0.09f
                intent.type == StrategicIntentType.REVIEW_PRIVACY && candidate.id == "privacy_review" -> 0.18f
                intent.type == StrategicIntentType.STABILIZE_CONTEXT && candidate.id in setOf("observe", "no_action") -> 0.08f
                else -> 0.0f
            }
            val score = (
                candidate.expectedBenefit * 0.48f +
                    (1.0f - candidate.expectedRisk).coerceIn(0.0f, 1.0f) * 0.18f +
                    state.confidence * 0.10f +
                    state.stateStability * 0.10f +
                    (1.0f - state.risk).coerceIn(0.0f, 1.0f) * 0.10f +
                    intentBonus - safetyPenalty - approvalPenalty
                ).coerceIn(0.0f, 0.98f)
            PlanScore(
                candidate = candidate,
                score = score,
                safetyPenalty = safetyPenalty,
                approvalPenalty = approvalPenalty,
                reason = "benefit_risk_safety_intent"
            )
        }.sortedByDescending { it.score }
        return PlanEvaluationState(
            candidateCount = candidates.size,
            best = scored.first(),
            ranked = scored.take(4),
            reason = "ranked_by_conservative_cost_model"
        )
    }

    private fun safetyPenalty(state: PhysicsExperienceState, candidate: PlanCandidate): Float {
        var penalty = 0.0f
        if (state.securityMode == SecurityLockMode.HARD_LOCK && candidate.id != "no_action") penalty += 0.40f
        if (state.privacyMode == PrivacyMode.LOCKED && candidate.id !in setOf("privacy_review", "no_action", "observe")) penalty += 0.30f
        if (candidate.skill.permission == SkillPermission.FORBIDDEN && candidate.id != "no_action") penalty += 0.50f
        if (candidate.skill.permission == SkillPermission.USER_APPROVAL_REQUIRED) penalty += 0.08f
        if (!state.telemetryOnly || state.autoControlEnabled) penalty += 0.40f
        return penalty.coerceIn(0.0f, 0.80f)
    }
}
