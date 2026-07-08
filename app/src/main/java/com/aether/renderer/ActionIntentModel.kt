package com.aether.renderer

enum class ActionIntent {
    POSITIONING,
    DEFENSIVE,
    SAFETY_PLAY,
    SEARCH_SCAN,
    TEXT_REVIEW,
    MENU_GRID,
    WAITING
}

object ActionIntentModel {
    fun refine(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): RuntimeDecision {
        val action = when {
            scene.nodeCount <= 0 -> ActionIntent.WAITING
            decision.risk >= 0.42f -> ActionIntent.SAFETY_PLAY
            scene.textNodeCount >= 28 && scene.clickableCount <= 6 -> ActionIntent.TEXT_REVIEW
            scene.clickableCount >= 12 -> ActionIntent.MENU_GRID
            scene.textNodeCount >= 14 && scene.clickableCount >= 4 -> ActionIntent.SEARCH_SCAN
            decision.sceneLabel == "DEEP_TREE" -> ActionIntent.DEFENSIVE
            else -> ActionIntent.POSITIONING
        }
        val label = UiReadability.actionIntentLabel(action)
        val detail = "${scene.density}:${profile.source.name}:${action.name.lowercase()}"
        return decision.copy(
            intentLabel = label,
            actionIntent = action,
            actionDetail = detail
        )
    }
}
