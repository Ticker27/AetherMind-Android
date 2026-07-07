package com.aether.renderer

enum class IntentReadinessBand {
    READY,
    CAUTION,
    SAFETY
}

data class IntentTransitionRule(
    val id: String,
    val band: IntentReadinessBand,
    val expectedIntent: StrategicIntentType,
    val reason: String
)

object IntentTransitionRules {
    fun classify(state: PhysicsExperienceState): IntentTransitionRule {
        return when {
            state.securityMode == SecurityLockMode.HARD_LOCK ||
                state.privacyMode == PrivacyMode.LOCKED ||
                state.risk >= 0.42f ||
                state.budgetMode == RuntimeBudgetMode.CRITICAL -> IntentTransitionRule(
                    id = "SAFETY_GATE",
                    band = IntentReadinessBand.SAFETY,
                    expectedIntent = if (state.privacyMode == PrivacyMode.LOCKED || state.securityMode == SecurityLockMode.HARD_LOCK) {
                        StrategicIntentType.REVIEW_PRIVACY
                    } else {
                        StrategicIntentType.REDUCE_RISK
                    },
                    reason = "risk_privacy_or_security_requires_safety_intent"
                )
            state.confidence < 0.45f ||
                state.uncertainty >= 0.62f ||
                state.volatility >= 0.30f ||
                state.stateStability < 0.42f ||
                state.budgetMode == RuntimeBudgetMode.WATCH ||
                state.budgetMode == RuntimeBudgetMode.THROTTLE -> IntentTransitionRule(
                    id = "CAUTION_GATE",
                    band = IntentReadinessBand.CAUTION,
                    expectedIntent = if (state.confidence < 0.45f || state.uncertainty >= 0.62f) {
                        StrategicIntentType.SEEK_CLARITY
                    } else {
                        StrategicIntentType.STABILIZE_CONTEXT
                    },
                    reason = "uncertain_or_unstable_context_requires_caution"
                )
            else -> IntentTransitionRule(
                id = "READY_GATE",
                band = IntentReadinessBand.READY,
                expectedIntent = StrategicIntentType.MAINTAIN_READY,
                reason = "stable_low_risk_context_can_remain_ready"
            )
        }
    }
}
