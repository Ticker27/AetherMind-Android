package com.aether.renderer

data class StrategicReasoningState(
    val physicsState: PhysicsExperienceState = PhysicsExperienceState(),
    val intent: StrategicIntentState = StrategicIntentState(),
    val evaluation: PlanEvaluationState = PlanEvaluationState(),
    val explanation: PlanExplanationState = PlanExplanationState(),
    val skill: SkillCard = SkillBoundary.observeScreen,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val apiStatus: String = "FINAL_STABILITY_LOCK"
) {
    val badge: String
        get() = when {
            !telemetryOnly || autoControlEnabled -> "STRATEGIC_BLOCKED"
            evaluation.badge == "PLAN_STRONG" -> "STRATEGIC_READY"
            evaluation.badge == "PLAN_OK" -> "STRATEGIC_OK"
            else -> "STRATEGIC_OBSERVE"
        }

    val compactLine: String
        get() = "$badge ${intent.badge} ${evaluation.best.candidate.label} s=${evaluation.best.score.fmt2()}"
}

object StrategicReasoningCore {
    fun evaluate(
        scene: SceneSnapshot,
        observer: ObserverSnapshot,
        decision: RuntimeDecision,
        trust: TrustModelState,
        privacy: PrivacyGuardState,
        security: SecurityLockState,
        budget: RuntimeBudgetState
    ): StrategicReasoningState {
        val physicsState = StrategicStateModel.evaluate(scene, observer, decision, trust, privacy, security, budget)
        val intent = StrategicIntentEngine.decide(physicsState)
        val candidates = PlannerCandidateGenerator.generate(physicsState, intent)
        val evaluation = StrategicCostModel.evaluate(physicsState, intent, candidates)
        val explanation = PlanExplanation.build(physicsState, intent, evaluation)
        return StrategicReasoningState(
            physicsState = physicsState,
            intent = intent,
            evaluation = evaluation,
            explanation = explanation,
            skill = evaluation.best.candidate.skill,
            telemetryOnly = security.telemetryOnly,
            autoControlEnabled = security.autoControlEnabled,
            apiStatus = "FINAL_STABILITY_LOCK"
        )
    }
}
