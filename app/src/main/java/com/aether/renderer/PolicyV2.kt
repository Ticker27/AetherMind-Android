package com.aether.renderer

enum class PolicyV2State {
    OBSERVE,
    LEARN,
    READY,
    CAUTION,
    LOCKED
}

object PolicyV2 {
    fun apply(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): RuntimeDecision {
        val state = when {
            decision.safetyLevel == SafetyLevel.LOCKED -> PolicyV2State.LOCKED
            decision.contextLabel == "CTX_ERROR_RECOVERY" -> PolicyV2State.LOCKED
            decision.contextLabel == "CTX_CONFIRMATION" -> PolicyV2State.CAUTION
            decision.risk >= 0.44f -> PolicyV2State.LOCKED
            decision.risk >= 0.30f -> PolicyV2State.CAUTION
            decision.behaviorLabel == "BEHAVIOR_VOLATILE" -> PolicyV2State.CAUTION
            decision.contextConfidence < 0.42f || decision.behaviorSamples < 4 -> PolicyV2State.LEARN
            decision.confidence >= 0.72f && decision.behaviorStability >= 0.70f && profile.telemetryOnly -> PolicyV2State.READY
            else -> PolicyV2State.OBSERVE
        }
        val reason = when (state) {
            PolicyV2State.LOCKED -> when {
                decision.contextLabel == "CTX_ERROR_RECOVERY" -> "v2_error_locked"
                decision.risk >= 0.44f -> "v2_risk_locked"
                else -> "v2_safety_locked"
            }
            PolicyV2State.CAUTION -> when {
                decision.contextLabel == "CTX_CONFIRMATION" -> "v2_confirm_caution"
                decision.behaviorLabel == "BEHAVIOR_VOLATILE" -> "v2_behavior_caution"
                else -> "v2_risk_caution"
            }
            PolicyV2State.LEARN -> "v2_learning_context"
            PolicyV2State.READY -> "v2_ready_stable_context"
            PolicyV2State.OBSERVE -> "v2_observe_context"
        }
        val safety = when (state) {
            PolicyV2State.LOCKED -> SafetyLevel.LOCKED
            PolicyV2State.CAUTION, PolicyV2State.LEARN -> SafetyLevel.CAUTION
            PolicyV2State.READY, PolicyV2State.OBSERVE -> decision.safetyLevel
        }
        val badge = when (state) {
            PolicyV2State.LOCKED -> "LOCKED"
            PolicyV2State.CAUTION -> "CAUTION"
            PolicyV2State.LEARN -> "LEARN"
            PolicyV2State.READY -> "READY"
            PolicyV2State.OBSERVE -> if (safety == SafetyLevel.SAFE) "SAFE" else "OBSERVE"
        }
        val legacyMode = when (state) {
            PolicyV2State.LOCKED, PolicyV2State.CAUTION -> PolicyMode.SOFT_LOCK
            PolicyV2State.READY -> PolicyMode.ACTIVE_READY
            else -> PolicyMode.OBSERVE
        }
        return decision.copy(
            safetyLevel = safety,
            safetyBadge = badge,
            gateReason = reason,
            policyMode = legacyMode,
            policyReason = reason,
            policyV2State = state,
            policyV2Reason = reason,
            telemetryOnly = true
        )
    }
}
