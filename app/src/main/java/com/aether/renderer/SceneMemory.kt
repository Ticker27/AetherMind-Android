package com.aether.renderer

import kotlin.math.abs

data class SceneMemoryState(
    val packageName: String = "",
    val lastSceneLabel: String = "IDLE",
    val lastIntentLabel: String = "POSITIONING",
    val lastConfidence: Float = 0.0f,
    val lastRisk: Float = 0.0f,
    val stabilityScore: Float = 0.0f,
    val sampleCount: Int = 0,
    val changed: Boolean = false
) {
    val stabilityLabel: String
        get() = when {
            sampleCount < 3 -> "LEARNING"
            stabilityScore >= 0.72f -> "STABLE"
            stabilityScore >= 0.45f -> "WATCH"
            else -> "UNSTABLE"
        }
}

object SceneMemory {
    private const val MAX_PACKAGES = 24
    private val memory = LinkedHashMap<String, SceneMemoryState>()

    fun enrich(scene: SceneSnapshot, decision: RuntimeDecision): RuntimeDecision {
        val key = scene.packageName.ifBlank { "unknown" }
        val previous = memory[key]
        val changed = previous != null && (
            previous.lastSceneLabel != decision.sceneLabel ||
                abs(previous.lastConfidence - decision.confidence) > 0.22f ||
                abs(previous.lastRisk - decision.risk) > 0.18f
            )

        val continuity = when {
            previous == null -> 0.25f
            changed -> 0.25f
            else -> 0.86f
        }
        val confidenceBalance = (1.0f - abs(decision.confidence - (previous?.lastConfidence ?: decision.confidence))).coerceIn(0.0f, 1.0f)
        val rawStability = (continuity * 0.65f + confidenceBalance * 0.35f).coerceIn(0.0f, 1.0f)
        val stability = if (previous == null) rawStability else (previous.stabilityScore * 0.55f + rawStability * 0.45f).coerceIn(0.0f, 1.0f)

        val state = SceneMemoryState(
            packageName = key,
            lastSceneLabel = decision.sceneLabel,
            lastIntentLabel = decision.intentLabel,
            lastConfidence = decision.confidence,
            lastRisk = decision.risk,
            stabilityScore = stability,
            sampleCount = ((previous?.sampleCount ?: 0) + 1).coerceAtMost(999),
            changed = changed
        )
        memory[key] = state
        trim()

        val adjustedLevel = when {
            state.stabilityLabel == "UNSTABLE" && decision.safetyLevel == SafetyLevel.SAFE -> SafetyLevel.CAUTION
            else -> decision.safetyLevel
        }
        val adjustedBadge = when (adjustedLevel) {
            SafetyLevel.SAFE -> "SAFE"
            SafetyLevel.CAUTION -> "CAUTION"
            SafetyLevel.LOCKED -> "LOCKED"
        }
        val adjustedGate = when {
            state.stabilityLabel == "UNSTABLE" -> "unstable_scene"
            state.stabilityLabel == "LEARNING" -> "learning_scene"
            else -> decision.gateReason
        }
        return decision.copy(
            safetyLevel = adjustedLevel,
            stabilityScore = state.stabilityScore,
            stabilityLabel = state.stabilityLabel,
            sceneChanged = state.changed,
            memorySamples = state.sampleCount,
            safetyBadge = adjustedBadge,
            gateReason = adjustedGate
        )
    }

    fun current(packageName: String): SceneMemoryState? = memory[packageName.ifBlank { "unknown" }]

    private fun trim() {
        while (memory.size > MAX_PACKAGES) {
            val first = memory.keys.firstOrNull() ?: return
            memory.remove(first)
        }
    }
}
