package com.aether.renderer

import kotlin.math.max
import kotlin.math.min

data class LearningState(
    val packageName: String = "",
    val contextLabel: String = "CTX_UNKNOWN",
    val samples: Int = 0,
    val trustScore: Float = 0.0f,
    val confidenceDelta: Float = 0.0f,
    val riskDelta: Float = 0.0f,
    val label: String = "LEARNING_COLD",
    val reason: String = "cold_start",
    val lastSignal: LearningSignalKind = LearningSignalKind.COLD_START,
    val avgConfidence: Float = 0.0f,
    val avgRisk: Float = 0.0f,
    val avgStability: Float = 0.0f
)

object LearningLoop {
    private const val MAX_SAMPLES = 240
    private val states = linkedMapOf<String, LearningState>()

    fun enrich(scene: SceneSnapshot, decision: RuntimeDecision): RuntimeDecision {
        val state = update(scene, decision)
        val learnedConfidence = (decision.confidence + state.confidenceDelta).coerceIn(0.05f, 0.95f)
        val learnedRisk = (decision.risk + state.riskDelta).coerceIn(0.02f, 0.60f)
        val elevated = elevateIfNeeded(decision, learnedRisk, state)
        return elevated.copy(
            confidence = learnedConfidence,
            risk = learnedRisk,
            learningLabel = state.label,
            learningReason = state.reason,
            learningTrust = state.trustScore,
            learningSamples = state.samples,
            learningConfidenceDelta = state.confidenceDelta,
            learningRiskDelta = state.riskDelta
        )
    }

    fun current(packageName: String): LearningState? {
        return states.values.lastOrNull { it.packageName == packageName }
    }

    private fun update(scene: SceneSnapshot, decision: RuntimeDecision): LearningState {
        val signal = signalFrom(scene, decision)
        val key = keyOf(signal.packageName, signal.contextLabel, signal.screenType)
        val old = states[key] ?: LearningState(
            packageName = signal.packageName,
            contextLabel = signal.contextLabel
        )
        val nextSamples = min(MAX_SAMPLES, old.samples + 1)
        val alpha = if (old.samples == 0) 1.0f else 0.18f
        val avgConfidence = blend(old.avgConfidence, signal.confidence, alpha)
        val avgRisk = blend(old.avgRisk, signal.risk, alpha)
        val avgStability = blend(old.avgStability, signal.stability, alpha)
        val trust = computeTrust(nextSamples, avgConfidence, avgRisk, avgStability, signal.kind)
        val confidenceDelta = computeConfidenceDelta(trust, avgRisk, avgStability, signal.kind)
        val riskDelta = computeRiskDelta(trust, avgRisk, avgStability, signal.kind)
        val label = when {
            signal.kind == LearningSignalKind.RISK_NEGATIVE -> "LEARNING_RISK"
            nextSamples < 6 -> "LEARNING_COLD"
            trust >= 0.72f && avgRisk < 0.24f -> "LEARNING_STABLE"
            trust >= 0.48f -> "LEARNING_WATCH"
            else -> "LEARNING_CAUTION"
        }
        val reason = "${signal.reason}:trust=${trust.format2()}:n=$nextSamples"
        val state = LearningState(
            packageName = signal.packageName,
            contextLabel = signal.contextLabel,
            samples = nextSamples,
            trustScore = trust,
            confidenceDelta = confidenceDelta,
            riskDelta = riskDelta,
            label = label,
            reason = reason,
            lastSignal = signal.kind,
            avgConfidence = avgConfidence,
            avgRisk = avgRisk,
            avgStability = avgStability
        )
        states[key] = state
        trim()
        return state
    }

    private fun signalFrom(scene: SceneSnapshot, decision: RuntimeDecision): LearningSignal {
        val stability = averageNonZero(
            decision.stabilityScore,
            decision.anchorStability,
            decision.behaviorStability,
            decision.contextConfidence
        )
        val riskPressure = max(
            decision.risk,
            max(decision.contextRisk, decision.behaviorRisk)
        ).coerceIn(0.02f, 0.60f)
        val kind = when {
            scene.nodeCount <= 0 || decision.contextLabel == "CTX_UNKNOWN" -> LearningSignalKind.COLD_START
            decision.policyV2State == PolicyV2State.LOCKED || riskPressure >= 0.42f -> LearningSignalKind.RISK_NEGATIVE
            stability >= 0.70f && decision.confidence >= 0.68f && riskPressure < 0.26f -> LearningSignalKind.STABLE_POSITIVE
            else -> LearningSignalKind.WATCH
        }
        val reason = when (kind) {
            LearningSignalKind.STABLE_POSITIVE -> "stable_context"
            LearningSignalKind.WATCH -> "watch_context"
            LearningSignalKind.RISK_NEGATIVE -> "risk_context"
            LearningSignalKind.COLD_START -> "cold_context"
        }
        return LearningSignal(
            packageName = scene.packageName,
            contextLabel = decision.contextLabel,
            screenType = decision.screenType,
            confidence = decision.confidence,
            risk = riskPressure,
            stability = stability,
            behaviorStability = decision.behaviorStability,
            sessionSamples = decision.sessionSamples,
            kind = kind,
            reason = reason,
            timestampNanos = scene.timestampNanos
        )
    }

    private fun elevateIfNeeded(decision: RuntimeDecision, learnedRisk: Float, state: LearningState): RuntimeDecision {
        return when {
            learnedRisk >= 0.44f -> decision.copy(
                safetyLevel = SafetyLevel.LOCKED,
                safetyBadge = "LOCKED",
                gateReason = "learning_risk_gate",
                policyMode = PolicyMode.SOFT_LOCK,
                policyReason = "learning_risk_gate",
                policyV2State = PolicyV2State.LOCKED,
                policyV2Reason = "learning_risk_gate"
            )
            learnedRisk >= 0.30f && decision.policyV2State != PolicyV2State.LOCKED -> decision.copy(
                safetyLevel = SafetyLevel.CAUTION,
                safetyBadge = if (state.label == "LEARNING_COLD") "LEARN" else "CAUTION",
                gateReason = "learning_caution_gate",
                policyMode = PolicyMode.SOFT_LOCK,
                policyReason = "learning_caution_gate",
                policyV2State = PolicyV2State.CAUTION,
                policyV2Reason = "learning_caution_gate"
            )
            state.label == "LEARNING_COLD" && decision.policyV2State == PolicyV2State.OBSERVE -> decision.copy(
                safetyBadge = "LEARN",
                gateReason = "learning_cold_start",
                policyV2State = PolicyV2State.LEARN,
                policyV2Reason = "learning_cold_start"
            )
            else -> decision
        }
    }

    private fun computeTrust(samples: Int, confidence: Float, risk: Float, stability: Float, kind: LearningSignalKind): Float {
        val sampleScore = (samples.toFloat() / 24.0f).coerceIn(0.0f, 1.0f)
        val base = sampleScore * 0.26f + confidence * 0.28f + stability * 0.34f + (1.0f - risk).coerceIn(0.0f, 1.0f) * 0.12f
        val penalty = when (kind) {
            LearningSignalKind.RISK_NEGATIVE -> 0.24f
            LearningSignalKind.COLD_START -> 0.18f
            else -> 0.0f
        }
        return (base - penalty).coerceIn(0.0f, 0.95f)
    }

    private fun computeConfidenceDelta(trust: Float, risk: Float, stability: Float, kind: LearningSignalKind): Float {
        val raw = (trust - 0.50f) * 0.08f + (stability - risk) * 0.04f
        val capped = raw.coerceIn(-0.06f, 0.07f)
        return when (kind) {
            LearningSignalKind.RISK_NEGATIVE -> min(capped, 0.0f)
            LearningSignalKind.COLD_START -> min(capped, 0.01f)
            else -> capped
        }
    }

    private fun computeRiskDelta(trust: Float, risk: Float, stability: Float, kind: LearningSignalKind): Float {
        val raw = (risk - stability) * 0.06f - (trust - 0.50f) * 0.03f
        val bounded = raw.coerceIn(-0.04f, 0.09f)
        return when (kind) {
            LearningSignalKind.RISK_NEGATIVE -> max(bounded, 0.04f)
            LearningSignalKind.COLD_START -> max(bounded, 0.01f)
            else -> bounded
        }
    }

    private fun blend(old: Float, next: Float, alpha: Float): Float {
        val safeOld = if (old <= 0.0f) next else old
        return safeOld * (1.0f - alpha) + next * alpha
    }

    private fun averageNonZero(vararg values: Float): Float {
        val valid = values.filter { it > 0.0f }
        if (valid.isEmpty()) return 0.0f
        return valid.sum() / valid.size.toFloat()
    }

    private fun keyOf(packageName: String, contextLabel: String, screenType: ScreenType): String {
        return "${packageName.ifBlank { "unknown" }}|$contextLabel|${screenType.name}"
    }

    private fun trim() {
        while (states.size > 64) {
            val first = states.keys.firstOrNull() ?: return
            states.remove(first)
        }
    }
}

private fun Float.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
