package com.aether.renderer

import kotlin.math.abs

data class SessionMemoryState(
    val packageName: String = "",
    val samples: Int = 0,
    val avgConfidence: Float = 0.0f,
    val avgRisk: Float = 0.0f,
    val trend: String = "NEW",
    val lastPolicy: PolicyMode = PolicyMode.OBSERVE
)

object SessionMemory {
    private const val MAX_PACKAGES = 24
    private val sessions = LinkedHashMap<String, SessionMemoryState>()

    fun enrich(scene: SceneSnapshot, decision: RuntimeDecision): RuntimeDecision {
        val key = scene.packageName.ifBlank { "unknown" }
        val previous = sessions[key]
        val samples = ((previous?.samples ?: 0) + 1).coerceAtMost(999)
        val avgConfidence = smooth(previous?.avgConfidence, decision.confidence)
        val avgRisk = smooth(previous?.avgRisk, decision.risk)
        val trend = when {
            previous == null || samples < 3 -> "NEW"
            avgRisk - previous.avgRisk > 0.08f -> "RISK_UP"
            previous.avgRisk - avgRisk > 0.08f -> "RISK_DOWN"
            avgConfidence - previous.avgConfidence > 0.10f -> "CONF_UP"
            abs(avgConfidence - previous.avgConfidence) < 0.04f && abs(avgRisk - previous.avgRisk) < 0.04f -> "STEADY"
            else -> "SHIFTING"
        }
        val state = SessionMemoryState(
            packageName = key,
            samples = samples,
            avgConfidence = avgConfidence,
            avgRisk = avgRisk,
            trend = trend,
            lastPolicy = decision.policyMode
        )
        sessions[key] = state
        trim()
        val finalBadge = when {
            trend == "RISK_UP" && decision.safetyBadge == "READY" -> "CAUTION"
            else -> decision.safetyBadge
        }
        return decision.copy(
            sessionTrend = trend,
            sessionSamples = samples,
            sessionAvgConfidence = avgConfidence,
            sessionAvgRisk = avgRisk,
            safetyBadge = finalBadge
        )
    }

    fun current(packageName: String): SessionMemoryState? = sessions[packageName.ifBlank { "unknown" }]

    private fun smooth(old: Float?, next: Float): Float {
        val base = old ?: next
        return (base * 0.70f + next * 0.30f).coerceIn(0.0f, 1.0f)
    }

    private fun trim() {
        while (sessions.size > MAX_PACKAGES) {
            val first = sessions.keys.firstOrNull() ?: return
            sessions.remove(first)
        }
    }
}
