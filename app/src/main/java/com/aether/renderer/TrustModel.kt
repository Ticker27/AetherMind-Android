package com.aether.renderer

import kotlin.math.abs
import kotlin.math.max

 enum class TrustBand {
    LOW,
    WATCH,
    TRUSTED,
    VOLATILE
}

data class TrustModelState(
    val packageName: String = "",
    val trustBand: TrustBand = TrustBand.LOW,
    val trustScore: Float = 0.0f,
    val uncertainty: Float = 1.0f,
    val volatility: Float = 0.0f,
    val label: String = "TRUST_LOW",
    val reason: String = "trust_cold_start"
)

object TrustModel {
    private val latest = LinkedHashMap<String, TrustModelState>()

    fun enrich(scene: SceneSnapshot, decision: RuntimeDecision): RuntimeDecision {
        val memory = LearningMemory.current(scene.packageName) ?: LearningMemoryState(packageName = scene.packageName.ifBlank { "unknown" })
        val state = evaluate(decision, memory)
        val adjustedRisk = when (state.trustBand) {
            TrustBand.VOLATILE -> (decision.risk + 0.07f).coerceAtMost(0.60f)
            TrustBand.LOW -> (decision.risk + 0.03f).coerceAtMost(0.60f)
            else -> decision.risk
        }
        val adjustedConfidence = when (state.trustBand) {
            TrustBand.TRUSTED -> (decision.confidence + 0.02f).coerceAtMost(0.98f)
            TrustBand.VOLATILE -> (decision.confidence - 0.05f).coerceAtLeast(0.05f)
            TrustBand.LOW -> (decision.confidence - 0.02f).coerceAtLeast(0.05f)
            TrustBand.WATCH -> decision.confidence
        }
        return decision.copy(
            confidence = adjustedConfidence,
            risk = adjustedRisk,
            trustBand = state.trustBand,
            trustScore = state.trustScore,
            uncertainty = state.uncertainty,
            volatility = state.volatility,
            trustLabel = state.label,
            trustReason = state.reason
        )
    }

    fun evaluate(decision: RuntimeDecision, memory: LearningMemoryState): TrustModelState {
        val confidenceBase = averageNonZero(
            decision.contextConfidence,
            decision.behaviorStability,
            decision.anchorStability,
            decision.learningTrust,
            memory.avgTrust
        )
        val uncertainty = (1.0f - confidenceBase).coerceIn(0.0f, 1.0f)
        val volatility = max(
            memory.volatility,
            max(
                abs(decision.sessionAvgRisk - decision.risk),
                abs(decision.contextRisk - decision.risk)
            )
        ).coerceIn(0.0f, 1.0f)
        val riskPressure = max(decision.risk, max(decision.contextRisk, memory.avgRisk)).coerceIn(0.02f, 0.60f)
        val sampleScore = (memory.samples.toFloat() / 32.0f).coerceIn(0.0f, 1.0f)
        val trust = (
            confidenceBase * 0.38f +
                (1.0f - riskPressure) * 0.24f +
                (1.0f - uncertainty) * 0.18f +
                (1.0f - volatility) * 0.12f +
                sampleScore * 0.08f
            ).coerceIn(0.0f, 0.95f)
        val band = when {
            volatility >= 0.24f -> TrustBand.VOLATILE
            trust >= 0.72f && uncertainty < 0.32f && riskPressure < 0.28f -> TrustBand.TRUSTED
            trust >= 0.46f -> TrustBand.WATCH
            else -> TrustBand.LOW
        }
        val label = when (band) {
            TrustBand.TRUSTED -> "TRUSTED"
            TrustBand.WATCH -> "TRUST_WATCH"
            TrustBand.LOW -> "TRUST_LOW"
            TrustBand.VOLATILE -> "TRUST_VOLATILE"
        }
        val packageName = memory.packageName.ifBlank { "unknown" }
        val reason = "$label:trust=${trust.format2()}:unc=${uncertainty.format2()}:vol=${volatility.format2()}:risk=${riskPressure.format2()}"
        val state = TrustModelState(
            packageName = packageName,
            trustBand = band,
            trustScore = trust,
            uncertainty = uncertainty,
            volatility = volatility,
            label = label,
            reason = reason
        )
        latest[packageName] = state
        trim()
        return state
    }

    fun current(packageName: String): TrustModelState? = latest[packageName.ifBlank { "unknown" }]

    private fun averageNonZero(vararg values: Float): Float {
        val valid = values.filter { it > 0.0f }
        if (valid.isEmpty()) return 0.0f
        return valid.sum() / valid.size.toFloat()
    }

    private fun trim() {
        while (latest.size > 48) {
            val first = latest.keys.firstOrNull() ?: return
            latest.remove(first)
        }
    }
}

private fun Float.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
