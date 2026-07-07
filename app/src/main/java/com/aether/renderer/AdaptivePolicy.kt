package com.aether.renderer

enum class PolicyMode {
    OBSERVE,
    SOFT_LOCK,
    ACTIVE_READY
}

object AdaptivePolicy {
    fun apply(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): RuntimeDecision {
        val mode = when {
            decision.safetyLevel == SafetyLevel.LOCKED -> PolicyMode.SOFT_LOCK
            decision.screenType == ScreenType.SCREEN_CONFIRM -> PolicyMode.SOFT_LOCK
            decision.screenType == ScreenType.SCREEN_ERROR -> PolicyMode.SOFT_LOCK
            decision.risk >= 0.36f -> PolicyMode.SOFT_LOCK
            decision.stabilityLabel == "UNSTABLE" -> PolicyMode.SOFT_LOCK
            decision.stabilityLabel == "LEARNING" -> PolicyMode.OBSERVE
            decision.confidence >= 0.68f && decision.stabilityScore >= 0.68f && profile.telemetryOnly -> PolicyMode.ACTIVE_READY
            else -> PolicyMode.OBSERVE
        }
        val reason = when (mode) {
            PolicyMode.SOFT_LOCK -> when {
                decision.safetyLevel == SafetyLevel.LOCKED -> "locked_by_safety"
                decision.screenType == ScreenType.SCREEN_CONFIRM -> "confirm_screen_soft_lock"
                decision.screenType == ScreenType.SCREEN_ERROR -> "error_screen_soft_lock"
                decision.risk >= 0.36f -> "risk_soft_lock"
                decision.stabilityLabel == "UNSTABLE" -> "unstable_soft_lock"
                else -> "soft_lock"
            }
            PolicyMode.ACTIVE_READY -> "stable_high_confidence"
            PolicyMode.OBSERVE -> when {
                scene.nodeCount <= 0 -> "waiting_scene"
                decision.stabilityLabel == "LEARNING" -> "learning_policy"
                else -> "observe_policy"
            }
        }
        val adjustedSafety = when {
            mode == PolicyMode.SOFT_LOCK -> SafetyLevel.CAUTION
            decision.safetyLevel == SafetyLevel.SAFE -> SafetyLevel.SAFE
            else -> decision.safetyLevel
        }
        val badge = when (adjustedSafety) {
            SafetyLevel.SAFE -> if (mode == PolicyMode.ACTIVE_READY) "READY" else "SAFE"
            SafetyLevel.CAUTION -> if (mode == PolicyMode.SOFT_LOCK) "SOFT_LOCK" else "CAUTION"
            SafetyLevel.LOCKED -> "LOCKED"
        }
        return decision.copy(
            policyMode = mode,
            policyReason = reason,
            safetyLevel = adjustedSafety,
            safetyBadge = badge,
            gateReason = reason,
            telemetryOnly = true
        )
    }
}
