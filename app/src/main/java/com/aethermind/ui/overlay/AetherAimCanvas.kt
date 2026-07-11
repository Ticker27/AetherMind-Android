package com.aethermind.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer

@Composable
fun AetherAimCanvas(
    state: OverlayUiState,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!state.showHud) return@Canvas

        with(TrajectoryPathRenderer) {
            if (state.showAimGuide) {
                drawTrajectorySegments(
                    segments = state.trajectory,
                    showDebugLabels = state.showDebugLabels,
                    textMeasurer = textMeasurer
                )
                drawGhostBall(state.balls.firstOrNull { it.kind == BallKind.GHOST })
            }

            if (state.showVisionMarkers) {
                drawBallMarkers(
                    balls = state.balls.filter { it.kind != BallKind.GHOST || state.showAimGuide },
                    showDebugLabels = state.showDebugLabels,
                    textMeasurer = textMeasurer
                )
            }
        }

        if (state.showDebugLabels) {
            val top = 26f
            val left = 24f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = androidx.compose.ui.geometry.Offset(left - 10f, top - 10f),
                size = androidx.compose.ui.geometry.Size(245f, 86f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
                style = Stroke(width = 2f)
            )
        }
    }
}
