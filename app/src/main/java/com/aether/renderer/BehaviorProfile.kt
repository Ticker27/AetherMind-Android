package com.aether.renderer

import kotlin.math.abs

data class BehaviorProfileState(
    val packageName: String = "",
    val dominantContext: String = "CTX_UNKNOWN",
    val dominantScreen: ScreenType = ScreenType.SCREEN_UNKNOWN,
    val samples: Int = 0,
    val behaviorStability: Float = 0.0f,
    val behaviorRisk: Float = 0.0f,
    val behaviorLabel: String = "BEHAVIOR_LEARNING"
)

object BehaviorProfile {
    private data class MutableBehavior(
        var samples: Int = 0,
        var lastContext: String = "CTX_UNKNOWN",
        var lastScreen: ScreenType = ScreenType.SCREEN_UNKNOWN,
        var stability: Float = 0.0f,
        var risk: Float = 0.0f
    )

    private val memory = LinkedHashMap<String, MutableBehavior>()
    private val latest = LinkedHashMap<String, BehaviorProfileState>()

    fun update(scene: SceneSnapshot, decision: RuntimeDecision, context: ContextState): BehaviorProfileState {
        val key = scene.packageName.ifBlank { "unknown" }
        val state = memory.getOrPut(key) { MutableBehavior() }
        val sameContext = state.lastContext == context.contextLabel && state.lastScreen == context.screenType
        val continuity = if (sameContext) 0.86f else 0.24f
        val confidenceBalance = (1.0f - abs(decision.confidence - context.contextConfidence)).coerceIn(0.0f, 1.0f)
        val nextStability = (continuity * 0.60f + confidenceBalance * 0.40f).coerceIn(0.0f, 1.0f)
        state.stability = if (state.samples <= 0) nextStability else (state.stability * 0.62f + nextStability * 0.38f).coerceIn(0.0f, 1.0f)
        state.risk = if (state.samples <= 0) decision.risk else (state.risk * 0.70f + decision.risk * 0.30f).coerceIn(0.02f, 0.60f)
        state.samples = (state.samples + 1).coerceAtMost(999)
        state.lastContext = context.contextLabel
        state.lastScreen = context.screenType

        val label = when {
            state.samples < 4 -> "BEHAVIOR_LEARNING"
            state.stability >= 0.76f && state.risk < 0.28f -> "BEHAVIOR_STABLE"
            state.stability >= 0.50f -> "BEHAVIOR_WATCH"
            else -> "BEHAVIOR_VOLATILE"
        }
        val snapshot = BehaviorProfileState(
            packageName = key,
            dominantContext = state.lastContext,
            dominantScreen = state.lastScreen,
            samples = state.samples,
            behaviorStability = state.stability,
            behaviorRisk = state.risk,
            behaviorLabel = label
        )
        latest[key] = snapshot
        trim()
        return snapshot
    }

    fun enrich(decision: RuntimeDecision, behavior: BehaviorProfileState): RuntimeDecision {
        val risk = when (behavior.behaviorLabel) {
            "BEHAVIOR_VOLATILE" -> (decision.risk + 0.08f).coerceAtMost(0.60f)
            "BEHAVIOR_STABLE" -> (decision.risk - 0.03f).coerceAtLeast(0.02f)
            else -> decision.risk
        }
        val confidence = when (behavior.behaviorLabel) {
            "BEHAVIOR_STABLE" -> (decision.confidence + 0.05f).coerceAtMost(0.98f)
            "BEHAVIOR_VOLATILE" -> (decision.confidence - 0.04f).coerceAtLeast(0.05f)
            else -> decision.confidence
        }
        return decision.copy(
            confidence = confidence,
            risk = risk,
            behaviorLabel = behavior.behaviorLabel,
            behaviorStability = behavior.behaviorStability,
            behaviorSamples = behavior.samples,
            behaviorRisk = behavior.behaviorRisk
        )
    }

    fun current(packageName: String): BehaviorProfileState? = latest[packageName.ifBlank { "unknown" }]

    private fun trim() {
        while (memory.size > 32) {
            val first = memory.keys.firstOrNull() ?: return
            memory.remove(first)
            latest.remove(first)
        }
    }
}
