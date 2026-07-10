package com.aether.renderer

import kotlin.math.abs
import kotlin.math.max

 data class LearningMemoryState(
    val packageName: String = "",
    val contextLabel: String = "CTX_UNKNOWN",
    val samples: Int = 0,
    val avgTrust: Float = 0.0f,
    val avgConfidence: Float = 0.0f,
    val avgRisk: Float = 0.0f,
    val volatility: Float = 0.0f,
    val trend: String = "MEMORY_NEW",
    val label: String = "MEMORY_COLD",
    val reason: String = "memory_cold_start"
)

object LearningMemory {
    private const val MAX_KEYS = 96
    private val memory = LinkedHashMap<String, LearningMemoryState>()
    private val latestByPackage = LinkedHashMap<String, LearningMemoryState>()

    fun enrich(scene: SceneSnapshot, decision: RuntimeDecision): RuntimeDecision {
        val state = update(scene, decision)
        val conservativeRisk = when (state.label) {
            "MEMORY_VOLATILE" -> (decision.risk + 0.05f).coerceAtMost(0.60f)
            "MEMORY_RISK" -> (decision.risk + 0.04f).coerceAtMost(0.60f)
            else -> decision.risk
        }
        val confidence = when (state.label) {
            "MEMORY_STABLE" -> (decision.confidence + 0.02f).coerceAtMost(0.98f)
            "MEMORY_VOLATILE" -> (decision.confidence - 0.04f).coerceAtLeast(0.05f)
            else -> decision.confidence
        }
        val badge = when {
            state.label == "MEMORY_VOLATILE" && decision.safetyBadge == "READY" -> "CAUTION"
            else -> decision.safetyBadge
        }
        return decision.copy(
            confidence = confidence,
            risk = conservativeRisk,
            safetyBadge = badge,
            learningMemoryLabel = state.label,
            learningMemoryTrend = state.trend,
            learningMemoryReason = state.reason,
            learningMemorySamples = state.samples,
            learningMemoryTrustAvg = state.avgTrust,
            learningMemoryVolatility = state.volatility
        )
    }

    fun current(packageName: String): LearningMemoryState? {
        return latestByPackage[packageName.ifBlank { "unknown" }]
    }

    private fun update(scene: SceneSnapshot, decision: RuntimeDecision): LearningMemoryState {
        val key = keyOf(scene.packageName, decision.contextLabel, decision.screenType)
        val old = memory[key]
        val samples = ((old?.samples ?: 0) + 1).coerceAtMost(999)
        val alpha = if (old == null) 1.0f else 0.16f
        val avgTrust = blend(old?.avgTrust, decision.learningTrust, alpha).coerceIn(0.0f, 0.95f)
        val avgConfidence = blend(old?.avgConfidence, decision.confidence, alpha).coerceIn(0.0f, 0.98f)
        val avgRisk = blend(old?.avgRisk, decision.risk, alpha).coerceIn(0.02f, 0.60f)
        val shock = max(
            abs(decision.risk - (old?.avgRisk ?: decision.risk)),
            abs(decision.confidence - (old?.avgConfidence ?: decision.confidence))
        ).coerceIn(0.0f, 1.0f)
        val volatility = blend(old?.volatility, shock, 0.24f).coerceIn(0.0f, 1.0f)
        val trend = when {
            old == null || samples < 4 -> "MEMORY_NEW"
            avgRisk - old.avgRisk > 0.05f -> "MEMORY_RISK_UP"
            old.avgRisk - avgRisk > 0.05f -> "MEMORY_RISK_DOWN"
            avgTrust - old.avgTrust > 0.08f -> "MEMORY_TRUST_UP"
            volatility >= 0.18f -> "MEMORY_SHIFTING"
            else -> "MEMORY_STEADY"
        }
        val label = when {
            samples < 6 -> "MEMORY_COLD"
            avgRisk >= 0.36f -> "MEMORY_RISK"
            volatility >= 0.20f -> "MEMORY_VOLATILE"
            avgTrust >= 0.68f && avgConfidence >= 0.64f && avgRisk < 0.28f -> "MEMORY_STABLE"
            else -> "MEMORY_WATCH"
        }
        val reason = "$label:$trend:trust=${avgTrust.format2()}:risk=${avgRisk.format2()}:vol=${volatility.format2()}:n=$samples"
        val state = LearningMemoryState(
            packageName = scene.packageName.ifBlank { "unknown" },
            contextLabel = decision.contextLabel,
            samples = samples,
            avgTrust = avgTrust,
            avgConfidence = avgConfidence,
            avgRisk = avgRisk,
            volatility = volatility,
            trend = trend,
            label = label,
            reason = reason
        )
        memory[key] = state
        latestByPackage[state.packageName] = state
        trim()
        return state
    }

    private fun keyOf(packageName: String, contextLabel: String, screenType: ScreenType): String {
        return "${packageName.ifBlank { "unknown" }}|$contextLabel|${screenType.name}"
    }

    private fun blend(old: Float?, next: Float, alpha: Float): Float {
        val base = old ?: next
        return base * (1.0f - alpha) + next * alpha
    }

    private fun trim() {
        while (memory.size > MAX_KEYS) {
            val first = memory.keys.firstOrNull() ?: return
            memory.remove(first)
        }
        while (latestByPackage.size > 48) {
            val first = latestByPackage.keys.firstOrNull() ?: return
            latestByPackage.remove(first)
        }
    }
}

private fun Float.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
