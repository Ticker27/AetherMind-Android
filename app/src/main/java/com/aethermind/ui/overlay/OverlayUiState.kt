package com.aethermind.ui.overlay

import androidx.compose.ui.geometry.Offset

enum class BallKind {
    CUE,
    TARGET,
    OBJECT,
    GHOST
}

enum class TrajectoryLineType {
    PRIMARY,
    GHOST,
    COLLISION,
    REBOUND,
    POCKET
}

enum class AiSkillLevel(
    val nativeValue: Int,
    val label: String,
    val shortLabel: String,
    val description: String
) {
    BEGINNER(0, "Beginner", "Basic", "Short guide lines, higher tolerance, conservative hints"),
    INTERMEDIATE(1, "Intermediate", "Smart", "Balanced trajectory, stable hinting, normal planning"),
    ADVANCED(2, "Advanced", "Pro", "Longer prediction path, tighter aim and richer physics hints");

    companion object {
        fun fromNativeValue(value: Int): AiSkillLevel {
            return when (value) {
                BEGINNER.nativeValue -> BEGINNER
                ADVANCED.nativeValue -> ADVANCED
                else -> INTERMEDIATE
            }
        }
    }
}

data class ScreenBall(
    val id: Int,
    val center: Offset,
    val radiusPx: Float,
    val kind: BallKind,
    val confidence: Float = 1f,
    val label: String = ""
)

data class TrajectorySegment(
    val start: Offset,
    val end: Offset,
    val type: TrajectoryLineType,
    val confidence: Float = 1f
)

data class OverlayUiState(
    val aiSkillLevel: AiSkillLevel = AiSkillLevel.INTERMEDIATE,
    val showHud: Boolean = true,
    val showAimGuide: Boolean = true,
    val showVisionMarkers: Boolean = true,
    val showDebugLabels: Boolean = false,
    val confidence: Int = 91,
    val fps: Int = 30,
    val ballCount: Int = 0,
    val cueBall: ScreenBall? = null,
    val targetBall: ScreenBall? = null,
    val balls: List<ScreenBall> = emptyList(),
    val trajectory: List<TrajectorySegment> = emptyList(),
    val modeLabel: String = "PROPOSE ONLY"
) {
    val aiSkillLabel: String
        get() = aiSkillLevel.label

    val aiSkillShortLabel: String
        get() = aiSkillLevel.shortLabel
}
