package com.aether.renderer

enum class FeedbackPolicyState {
    OBSERVE,
    LEARNING_HOLD,
    TRUST_READY,
    CAUTION_HOLD,
    LOCKED_HOLD
}

data class FeedbackPolicyStatus(
    val packageName: String = "",
    val state: FeedbackPolicyState = FeedbackPolicyState.OBSERVE,
    val label: String = "FEEDBACK_OBSERVE",
    val reason: String = "feedback_observe",
    val allowSafetyReduction: Boolean = false,
    val samples: Int = 0
)

object FeedbackPolicy {
    private val latest = LinkedHashMap<String, FeedbackPolicyStatus>()

    fun apply(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): RuntimeDecision {
        val state = decide(scene, profile, decision)
        val elevated = when (state.state) {
            FeedbackPolicyState.LOCKED_HOLD -> decision.copy(
                safetyLevel = SafetyLevel.LOCKED,
                safetyBadge = "LOCKED",
                gateReason = state.reason,
                policyMode = PolicyMode.SOFT_LOCK,
                policyReason = state.reason,
                policyV2State = PolicyV2State.LOCKED,
                policyV2Reason = state.reason
            )
            FeedbackPolicyState.CAUTION_HOLD -> decision.copy(
                safetyLevel = SafetyLevel.CAUTION,
                safetyBadge = if (decision.safetyBadge == "LOCKED") "LOCKED" else "CAUTION",
                gateReason = state.reason,
                policyMode = PolicyMode.SOFT_LOCK,
                policyReason = state.reason,
                policyV2State = if (decision.policyV2State == PolicyV2State.LOCKED) PolicyV2State.LOCKED else PolicyV2State.CAUTION,
                policyV2Reason = state.reason
            )
            FeedbackPolicyState.LEARNING_HOLD -> decision.copy(
                safetyLevel = if (decision.safetyLevel == SafetyLevel.LOCKED) SafetyLevel.LOCKED else SafetyLevel.CAUTION,
                safetyBadge = if (decision.safetyBadge == "LOCKED") "LOCKED" else "LEARN",
                gateReason = state.reason,
                policyV2State = if (decision.policyV2State == PolicyV2State.LOCKED) PolicyV2State.LOCKED else PolicyV2State.LEARN,
                policyV2Reason = state.reason
            )
            FeedbackPolicyState.TRUST_READY -> decision.copy(
                safetyBadge = if (decision.safetyBadge == "READY" || decision.safetyBadge == "SAFE") "READY" else decision.safetyBadge,
                gateReason = state.reason,
                policyV2Reason = state.reason
            )
            FeedbackPolicyState.OBSERVE -> decision.copy(
                gateReason = if (decision.gateReason.isBlank()) state.reason else decision.gateReason
            )
        }
        return elevated.copy(
            feedbackPolicyState = state.state,
            feedbackPolicyLabel = state.label,
            feedbackPolicyReason = state.reason,
            feedbackSamples = state.samples,
            telemetryOnly = true
        )
    }

    fun current(packageName: String): FeedbackPolicyStatus? = latest[packageName.ifBlank { "unknown" }]

    private fun decide(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): FeedbackPolicyStatus {
        val key = scene.packageName.ifBlank { "unknown" }
        val samples = maxOf(decision.learningMemorySamples, decision.learningSamples)
        val state = when {
            decision.policyV2State == PolicyV2State.LOCKED -> FeedbackPolicyState.LOCKED_HOLD
            decision.risk >= 0.44f || decision.trustBand == TrustBand.VOLATILE -> FeedbackPolicyState.LOCKED_HOLD
            decision.uncertainty >= 0.72f || decision.trustBand == TrustBand.LOW -> FeedbackPolicyState.LEARNING_HOLD
            decision.risk >= 0.30f || decision.learningMemoryLabel == "MEMORY_RISK" -> FeedbackPolicyState.CAUTION_HOLD
            decision.trustBand == TrustBand.TRUSTED && samples >= 16 && profile.telemetryOnly -> FeedbackPolicyState.TRUST_READY
            else -> FeedbackPolicyState.OBSERVE
        }
        val label = when (state) {
            FeedbackPolicyState.OBSERVE -> "FEEDBACK_OBSERVE"
            FeedbackPolicyState.LEARNING_HOLD -> "FEEDBACK_LEARN"
            FeedbackPolicyState.TRUST_READY -> "FEEDBACK_READY"
            FeedbackPolicyState.CAUTION_HOLD -> "FEEDBACK_CAUTION"
            FeedbackPolicyState.LOCKED_HOLD -> "FEEDBACK_LOCKED"
        }
        val reason = when (state) {
            FeedbackPolicyState.LOCKED_HOLD -> when {
                decision.policyV2State == PolicyV2State.LOCKED -> "feedback_policy_locked"
                decision.trustBand == TrustBand.VOLATILE -> "feedback_trust_volatile"
                else -> "feedback_risk_locked"
            }
            FeedbackPolicyState.CAUTION_HOLD -> "feedback_conservative_caution"
            FeedbackPolicyState.LEARNING_HOLD -> "feedback_learning_hold"
            FeedbackPolicyState.TRUST_READY -> "feedback_trust_ready_observe_only"
            FeedbackPolicyState.OBSERVE -> "feedback_observe"
        }
        val status = FeedbackPolicyStatus(
            packageName = key,
            state = state,
            label = label,
            reason = reason,
            allowSafetyReduction = false,
            samples = samples
        )
        latest[key] = status
        trim()
        return status
    }

    private fun trim() {
        while (latest.size > 48) {
            val first = latest.keys.firstOrNull() ?: return
            latest.remove(first)
        }
    }
}
