package com.aether.renderer

enum class StrategicIntentType {
    OBSERVE,
    SEEK_CLARITY,
    REDUCE_RISK,
    MAINTAIN_READY,
    REVIEW_PRIVACY,
    STABILIZE_CONTEXT
}

data class StrategicIntentState(
    val type: StrategicIntentType = StrategicIntentType.OBSERVE,
    val goal: String = "observe_safely",
    val confidence: Float = 0.0f,
    val riskTolerance: Float = 0.0f,
    val requiresUserApproval: Boolean = false,
    val reason: String = "intent_waiting"
) {
    val badge: String
        get() = when (type) {
            StrategicIntentType.OBSERVE -> "INTENT_OBSERVE"
            StrategicIntentType.SEEK_CLARITY -> "INTENT_SEEK_CLARITY"
            StrategicIntentType.REDUCE_RISK -> "INTENT_REDUCE_RISK"
            StrategicIntentType.MAINTAIN_READY -> "INTENT_READY"
            StrategicIntentType.REVIEW_PRIVACY -> "INTENT_PRIVACY_REVIEW"
            StrategicIntentType.STABILIZE_CONTEXT -> "INTENT_STABILIZE"
        }

    val compactLine: String
        get() = "$badge goal=$goal c=${confidence.fmt2()}"
}

object StrategicIntentEngine {
    fun decide(state: PhysicsExperienceState): StrategicIntentState {
        val type = when {
            !state.observerActive -> StrategicIntentType.OBSERVE
            state.securityMode == SecurityLockMode.HARD_LOCK || state.privacyMode == PrivacyMode.LOCKED -> StrategicIntentType.REVIEW_PRIVACY
            state.risk >= 0.42f || state.budgetMode == RuntimeBudgetMode.CRITICAL -> StrategicIntentType.REDUCE_RISK
            state.confidence < 0.45f || state.uncertainty >= 0.62f -> StrategicIntentType.SEEK_CLARITY
            state.stateStability < 0.42f || state.volatility >= 0.30f -> StrategicIntentType.STABILIZE_CONTEXT
            else -> StrategicIntentType.MAINTAIN_READY
        }
        return StrategicIntentState(
            type = type,
            goal = when (type) {
                StrategicIntentType.OBSERVE -> "observe_safely"
                StrategicIntentType.SEEK_CLARITY -> "collect_more_context"
                StrategicIntentType.REDUCE_RISK -> "lower_risk_before_action"
                StrategicIntentType.MAINTAIN_READY -> "keep_ready_state"
                StrategicIntentType.REVIEW_PRIVACY -> "protect_private_context"
                StrategicIntentType.STABILIZE_CONTEXT -> "wait_for_stable_screen"
            },
            confidence = when (type) {
                StrategicIntentType.OBSERVE -> 0.30f
                StrategicIntentType.SEEK_CLARITY -> (1.0f - state.uncertainty).coerceIn(0.20f, 0.72f)
                StrategicIntentType.REDUCE_RISK -> (0.50f + state.risk * 0.35f).coerceIn(0.0f, 0.92f)
                StrategicIntentType.MAINTAIN_READY -> state.confidence.coerceIn(0.0f, 0.96f)
                StrategicIntentType.REVIEW_PRIVACY -> 0.92f
                StrategicIntentType.STABILIZE_CONTEXT -> state.stateStability.coerceIn(0.20f, 0.74f)
            },
            riskTolerance = when (type) {
                StrategicIntentType.REDUCE_RISK, StrategicIntentType.REVIEW_PRIVACY -> 0.10f
                StrategicIntentType.SEEK_CLARITY, StrategicIntentType.STABILIZE_CONTEXT -> 0.18f
                StrategicIntentType.MAINTAIN_READY -> 0.28f
                StrategicIntentType.OBSERVE -> 0.12f
            },
            requiresUserApproval = type != StrategicIntentType.OBSERVE && type != StrategicIntentType.MAINTAIN_READY,
            reason = "intent_from_${state.badge.lowercase(java.util.Locale.US)}"
        )
    }
}
