package com.aether.renderer

import kotlin.math.max

data class ContextState(
    val packageName: String = "",
    val contextLabel: String = "CTX_UNKNOWN",
    val contextConfidence: Float = 0.0f,
    val contextRisk: Float = 0.0f,
    val contextReason: String = "unknown",
    val screenType: ScreenType = ScreenType.SCREEN_UNKNOWN,
    val anchorLabel: String = "NO_ANCHOR",
    val sessionTrend: String = "NEW"
)

object ContextEngine {
    private val latest = LinkedHashMap<String, ContextState>()

    fun update(
        scene: SceneSnapshot,
        decision: RuntimeDecision,
        screen: ScreenTypeState,
        anchorMemory: AnchorMemoryState,
        sceneMemory: SceneMemoryState,
        session: SessionMemoryState
    ): ContextState {
        val key = scene.packageName.ifBlank { "unknown" }
        val label = when {
            scene.nodeCount <= 0 -> "CTX_WAITING"
            screen.type == ScreenType.SCREEN_ERROR -> "CTX_ERROR_RECOVERY"
            screen.type == ScreenType.SCREEN_CONFIRM -> "CTX_CONFIRMATION"
            screen.type == ScreenType.SCREEN_FORM -> "CTX_FORM_ENTRY"
            screen.type == ScreenType.SCREEN_DIALOG -> "CTX_DIALOG_FOCUS"
            screen.type == ScreenType.SCREEN_LIST && decision.actionIntent == ActionIntent.MENU_GRID -> "CTX_MENU_GRID"
            screen.type == ScreenType.SCREEN_LIST -> "CTX_BROWSING_LIST"
            decision.actionIntent == ActionIntent.TEXT_REVIEW -> "CTX_READING"
            decision.actionIntent == ActionIntent.SEARCH_SCAN -> "CTX_SEARCH_SCAN"
            else -> "CTX_GENERAL"
        }
        val confidence = (
            decision.confidence * 0.42f +
                screen.confidence * 0.22f +
                anchorMemory.anchorStability * 0.18f +
                sceneMemory.stabilityScore * 0.12f +
                session.avgConfidence * 0.06f
            ).coerceIn(0.05f, 0.98f)
        val risk = max(
            decision.risk,
            when (label) {
                "CTX_CONFIRMATION" -> 0.34f
                "CTX_ERROR_RECOVERY" -> 0.40f
                "CTX_FORM_ENTRY" -> 0.26f
                "CTX_WAITING" -> 0.18f
                else -> 0.10f
            }
        ).coerceIn(0.02f, 0.60f)
        val reason = "$label:${screen.type.name}:${anchorMemory.stabilityLabel}:${sceneMemory.stabilityLabel}:${session.trend}"
        val state = ContextState(
            packageName = key,
            contextLabel = label,
            contextConfidence = confidence,
            contextRisk = risk,
            contextReason = reason,
            screenType = screen.type,
            anchorLabel = anchorMemory.stabilityLabel,
            sessionTrend = session.trend
        )
        latest[key] = state
        trim()
        return state
    }

    fun enrich(decision: RuntimeDecision, context: ContextState): RuntimeDecision {
        val confidence = (decision.confidence * 0.78f + context.contextConfidence * 0.22f).coerceIn(0.05f, 0.98f)
        val risk = max(decision.risk, context.contextRisk).coerceIn(0.02f, 0.60f)
        return decision.copy(
            confidence = confidence,
            risk = risk,
            contextLabel = context.contextLabel,
            contextConfidence = context.contextConfidence,
            contextRisk = context.contextRisk,
            contextReason = context.contextReason
        )
    }

    fun current(packageName: String): ContextState? = latest[packageName.ifBlank { "unknown" }]

    private fun trim() {
        while (latest.size > 32) {
            val first = latest.keys.firstOrNull() ?: return
            latest.remove(first)
        }
    }
}
