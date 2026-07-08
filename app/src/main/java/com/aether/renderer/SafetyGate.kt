package com.aether.renderer

enum class SafetyLevel {
    SAFE,
    CAUTION,
    LOCKED
}

object SafetyGate {
    fun apply(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): RuntimeDecision {
        val level = when {
            scene.nodeCount <= 0 -> SafetyLevel.CAUTION
            decision.risk >= 0.45f -> SafetyLevel.LOCKED
            decision.risk >= 0.28f -> SafetyLevel.CAUTION
            decision.confidence < 0.30f -> SafetyLevel.CAUTION
            profile.telemetryOnly -> SafetyLevel.SAFE
            else -> SafetyLevel.CAUTION
        }
        val badge = when (level) {
            SafetyLevel.SAFE -> "SAFE"
            SafetyLevel.CAUTION -> "CAUTION"
            SafetyLevel.LOCKED -> "LOCKED"
        }
        val gateReason = when (level) {
            SafetyLevel.SAFE -> "telemetry_ok"
            SafetyLevel.CAUTION -> when {
                scene.nodeCount <= 0 -> "waiting_scene"
                decision.confidence < 0.30f -> "low_confidence"
                decision.risk >= 0.28f -> "elevated_risk"
                else -> "observe_only"
            }
            SafetyLevel.LOCKED -> "risk_gate"
        }
        return decision.copy(
            telemetryOnly = true,
            safetyLevel = level,
            safetyBadge = badge,
            gateReason = gateReason
        )
    }
}
