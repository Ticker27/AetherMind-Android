package com.aether.renderer

enum class ScreenType {
    SCREEN_HOME,
    SCREEN_LIST,
    SCREEN_FORM,
    SCREEN_DIALOG,
    SCREEN_CONFIRM,
    SCREEN_ERROR,
    SCREEN_LOADING,
    SCREEN_UNKNOWN
}

data class ScreenTypeState(
    val type: ScreenType = ScreenType.SCREEN_UNKNOWN,
    val confidence: Float = 0.0f,
    val reason: String = "unknown"
)

object ScreenTypeModel {
    fun classify(scene: SceneSnapshot, anchors: List<NodeAnchor>, world: ScreenWorld): ScreenTypeState {
        val clickable = anchors.count { it.clickable }
        val text = anchors.count { it.roleGuess == "TEXT" || it.roleGuess == "BUTTON_TEXT" }
        val inputs = anchors.count { it.roleGuess == "INPUT" }
        val checks = anchors.count { it.roleGuess == "CHECK" }
        val bottomButtons = anchors.count { it.clickable && it.bounds.zone == "BOTTOM" }
        val centerArea = anchors.count { it.bounds.zone == "CENTER" }
        val hasErrorWord = anchors.any { it.textHash == "error".hashCode() || it.textHash == "failed".hashCode() }
        val type = when {
            scene.nodeCount <= 0 -> ScreenType.SCREEN_LOADING
            hasErrorWord -> ScreenType.SCREEN_ERROR
            inputs >= 1 || checks >= 2 -> ScreenType.SCREEN_FORM
            bottomButtons >= 1 && clickable <= 4 && text >= 2 -> ScreenType.SCREEN_CONFIRM
            centerArea >= 3 && anchors.size <= 12 && clickable <= 5 -> ScreenType.SCREEN_DIALOG
            clickable >= 8 || text >= 18 -> ScreenType.SCREEN_LIST
            clickable <= 3 && text <= 6 && scene.maxDepth <= 5 -> ScreenType.SCREEN_HOME
            else -> ScreenType.SCREEN_UNKNOWN
        }
        val conf = when (type) {
            ScreenType.SCREEN_LOADING -> 0.42f
            ScreenType.SCREEN_UNKNOWN -> 0.28f
            else -> (0.45f + anchors.size.coerceAtMost(24) * 0.018f + world.aspectRatio.coerceIn(0.4f, 1.0f) * 0.05f).coerceIn(0.45f, 0.88f)
        }
        val reason = "${type.name}:a=${anchors.size}:c=$clickable:t=$text:i=$inputs:b=$bottomButtons"
        return ScreenTypeState(type = type, confidence = conf, reason = reason)
    }

    fun enrich(decision: RuntimeDecision, screen: ScreenTypeState, anchorMemory: AnchorMemoryState): RuntimeDecision {
        val boostedConfidence = (decision.confidence + screen.confidence * 0.08f + anchorMemory.anchorStability * 0.06f).coerceIn(0.05f, 0.98f)
        val adjustedRisk = when (screen.type) {
            ScreenType.SCREEN_CONFIRM -> (decision.risk + 0.08f).coerceAtMost(0.60f)
            ScreenType.SCREEN_ERROR -> (decision.risk + 0.10f).coerceAtMost(0.60f)
            ScreenType.SCREEN_LOADING -> (decision.risk + 0.03f).coerceAtMost(0.60f)
            else -> decision.risk
        }
        return decision.copy(
            confidence = boostedConfidence,
            risk = adjustedRisk,
            screenType = screen.type,
            screenConfidence = screen.confidence,
            screenReason = screen.reason,
            anchorCount = anchorMemory.anchorCount,
            stableAnchorCount = anchorMemory.stableAnchorCount,
            anchorStability = anchorMemory.anchorStability,
            anchorLabel = anchorMemory.stabilityLabel
        )
    }
}
