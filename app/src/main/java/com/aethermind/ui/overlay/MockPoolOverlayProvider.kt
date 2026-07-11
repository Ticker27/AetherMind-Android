package com.aethermind.ui.overlay

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

object MockPoolOverlayProvider {

    fun generate(
        width: Int,
        height: Int,
        timeMs: Long,
        showHud: Boolean,
        showAimGuide: Boolean,
        showVisionMarkers: Boolean,
        showDebugLabels: Boolean,
        aiSkillLevel: AiSkillLevel = AiSkillLevel.INTERMEDIATE,
        autoPlayEnabled: Boolean = false,
        autoPlayArmed: Boolean = false,
        autoPlayStatus: String = if (autoPlayEnabled) "ARMED" else "OFF",
        autoPlayIntervalMs: Int = 1200,
        autoPlayPowerPx: Float = 420f
    ): OverlayUiState {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        val skillStability = when (aiSkillLevel) {
            AiSkillLevel.BEGINNER -> 0.45f
            AiSkillLevel.INTERMEDIATE -> 0.70f
            AiSkillLevel.ADVANCED -> 0.92f
        }

        val confidenceValue = when (aiSkillLevel) {
            AiSkillLevel.BEGINNER -> 68
            AiSkillLevel.INTERMEDIATE -> 84
            AiSkillLevel.ADVANCED -> 96
        }

        val fpsValue = when (aiSkillLevel) {
            AiSkillLevel.BEGINNER -> 24
            AiSkillLevel.INTERMEDIATE -> 30
            AiSkillLevel.ADVANCED -> 60
        }

        val phase = timeMs / 720.0
        val sway = sin(phase).toFloat() * (26f * (1.05f - skillStability))
        val micro = cos(phase * 0.7).toFloat() * (10f * (1.05f - skillStability))

        val cue = ScreenBall(
            id = 0,
            center = Offset(w * 0.48f + sway, h * 0.72f + micro),
            radiusPx = (w * 0.020f).coerceIn(15f, 26f),
            kind = BallKind.CUE,
            confidence = 0.75f + (skillStability * 0.23f),
            label = "Cue"
        )

        val target = ScreenBall(
            id = 1,
            center = Offset(w * 0.56f, h * 0.38f),
            radiusPx = (w * 0.018f).coerceIn(14f, 24f),
            kind = BallKind.TARGET,
            confidence = 0.70f + (skillStability * 0.24f),
            label = "Target"
        )

        val ghost = computeGhostBall(cue, target, skillStability)

        val pocket = Offset(w * 0.84f, h * 0.18f)
        val cushion = Offset(w * 0.22f, h * 0.25f)
        val collisionExit = Offset(w * 0.68f, h * 0.29f)

        val balls = listOf(
            cue,
            target,
            ghost,
            ScreenBall(3, Offset(w * 0.34f, h * 0.53f), target.radiusPx, BallKind.OBJECT, 0.83f, "3"),
            ScreenBall(4, Offset(w * 0.72f, h * 0.50f), target.radiusPx, BallKind.OBJECT, 0.80f, "4"),
            ScreenBall(5, Offset(w * 0.64f, h * 0.67f), target.radiusPx, BallKind.OBJECT, 0.77f, "5"),
            ScreenBall(6, Offset(w * 0.41f, h * 0.29f), target.radiusPx, BallKind.OBJECT, 0.70f, "6")
        )

        val trajectory = mutableListOf(
            TrajectorySegment(cue.center, ghost.center, TrajectoryLineType.PRIMARY, 0.78f + skillStability * 0.20f),
            TrajectorySegment(ghost.center, target.center, TrajectoryLineType.GHOST, 0.48f + skillStability * 0.34f)
        )

        if (aiSkillLevel != AiSkillLevel.BEGINNER) {
            trajectory += TrajectorySegment(target.center, collisionExit, TrajectoryLineType.COLLISION, 0.58f + skillStability * 0.26f)
            trajectory += TrajectorySegment(collisionExit, pocket, TrajectoryLineType.POCKET, 0.56f + skillStability * 0.25f)
        }

        if (aiSkillLevel == AiSkillLevel.ADVANCED) {
            trajectory += TrajectorySegment(cue.center, cushion, TrajectoryLineType.REBOUND, 0.72f)
            trajectory += TrajectorySegment(cushion, target.center, TrajectoryLineType.REBOUND, 0.68f)
        }

        return OverlayUiState(
            aiSkillLevel = aiSkillLevel,
            autoPlayEnabled = autoPlayEnabled,
            autoPlayArmed = autoPlayArmed,
            autoPlayStatus = autoPlayStatus,
            autoPlayIntervalMs = autoPlayIntervalMs,
            autoPlayPowerPx = autoPlayPowerPx,
            showHud = showHud,
            showAimGuide = showAimGuide,
            showVisionMarkers = showVisionMarkers,
            showDebugLabels = showDebugLabels,
            confidence = confidenceValue,
            fps = fpsValue,
            ballCount = balls.count { it.kind != BallKind.GHOST },
            cueBall = cue,
            targetBall = target,
            balls = balls,
            trajectory = trajectory,
            modeLabel = if (autoPlayEnabled) "AUTO PLAY" else "PROPOSE ONLY"
        )
    }

    private fun computeGhostBall(
        cue: ScreenBall,
        target: ScreenBall,
        skillStability: Float
    ): ScreenBall {
        val dx = target.center.x - cue.center.x
        val dy = target.center.y - cue.center.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val nx = dx / length
        val ny = dy / length
        val radius = (cue.radiusPx + target.radiusPx) * (0.50f + skillStability * 0.06f)
        val ghostCenter = Offset(
            x = target.center.x - nx * radius,
            y = target.center.y - ny * radius
        )

        return ScreenBall(
            id = 100,
            center = ghostCenter,
            radiusPx = cue.radiusPx * 1.06f,
            kind = BallKind.GHOST,
            confidence = 0.50f + skillStability * 0.38f,
            label = "Ghost"
        )
    }
}
