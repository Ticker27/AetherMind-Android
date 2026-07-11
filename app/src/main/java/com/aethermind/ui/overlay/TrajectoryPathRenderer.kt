package com.aethermind.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextMeasurer
import kotlin.math.roundToInt

object TrajectoryPathRenderer {

    fun DrawScope.drawTrajectorySegments(
        segments: List<TrajectorySegment>,
        showDebugLabels: Boolean,
        textMeasurer: TextMeasurer?
    ) {
        segments.forEachIndexed { index, segment ->
            val color = segment.type.color().copy(alpha = segment.alpha())
            val strokeWidth = when (segment.type) {
                TrajectoryLineType.PRIMARY -> 5.5f
                TrajectoryLineType.GHOST -> 3.0f
                TrajectoryLineType.COLLISION -> 4.0f
                TrajectoryLineType.REBOUND -> 3.0f
                TrajectoryLineType.POCKET -> 4.0f
            }
            val pathEffect = when (segment.type) {
                TrajectoryLineType.GHOST,
                TrajectoryLineType.REBOUND -> PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                else -> null
            }

            drawLine(
                color = Color.Black.copy(alpha = 0.45f),
                start = segment.start,
                end = segment.end,
                strokeWidth = strokeWidth + 4f,
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )
            drawLine(
                color = color,
                start = segment.start,
                end = segment.end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )

            drawImpactMarker(segment.end, segment.type, segment.confidence)

            if (showDebugLabels && textMeasurer != null) {
                drawDebugText(
                    textMeasurer = textMeasurer,
                    text = "${index + 1}:${segment.type.name}",
                    origin = segment.midpoint()
                )
            }
        }
    }

    fun DrawScope.drawBallMarkers(
        balls: List<ScreenBall>,
        showDebugLabels: Boolean,
        textMeasurer: TextMeasurer?
    ) {
        balls.forEach { ball ->
            val color = ball.kind.color().copy(alpha = if (ball.kind == BallKind.GHOST) 0.55f else 0.90f)
            val outer = ball.radiusPx + if (ball.kind == BallKind.TARGET) 5f else 2f

            drawCircle(
                color = Color.Black.copy(alpha = 0.50f),
                radius = outer + 4f,
                center = ball.center,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = color,
                radius = outer,
                center = ball.center,
                style = Stroke(width = if (ball.kind == BallKind.GHOST) 3f else 4f)
            )
            drawCircle(
                color = color.copy(alpha = 0.28f),
                radius = ball.radiusPx * 0.30f,
                center = ball.center
            )

            if (showDebugLabels && textMeasurer != null) {
                val confidence = (ball.confidence * 100f).roundToInt()
                drawDebugText(
                    textMeasurer = textMeasurer,
                    text = "${ball.label} $confidence%",
                    origin = Offset(ball.center.x + ball.radiusPx + 8f, ball.center.y - ball.radiusPx)
                )
            }
        }
    }

    fun DrawScope.drawGhostBall(ball: ScreenBall?) {
        if (ball == null) return
        drawCircle(
            color = Color(0xFFFF4DFF).copy(alpha = 0.20f),
            radius = ball.radiusPx * 1.36f,
            center = ball.center
        )
        drawCircle(
            color = Color(0xFFFF4DFF).copy(alpha = 0.74f),
            radius = ball.radiusPx,
            center = ball.center,
            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
        )
    }

    private fun DrawScope.drawImpactMarker(
        center: Offset,
        type: TrajectoryLineType,
        confidence: Float
    ) {
        val color = type.color().copy(alpha = (0.35f + confidence * 0.40f).coerceIn(0.35f, 0.85f))
        val radius = when (type) {
            TrajectoryLineType.PRIMARY -> 10f
            TrajectoryLineType.GHOST -> 7f
            TrajectoryLineType.COLLISION -> 9f
            TrajectoryLineType.REBOUND -> 7f
            TrajectoryLineType.POCKET -> 11f
        }
        drawCircle(
            color = Color.Black.copy(alpha = 0.50f),
            radius = radius + 3f,
            center = center,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )
    }

    private fun DrawScope.drawDebugText(
        textMeasurer: TextMeasurer,
        text: String,
        origin: Offset
    ) {
        drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = origin,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp
            )
        )
    }

    private fun TrajectorySegment.midpoint(): Offset {
        return Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f)
    }

    private fun TrajectorySegment.alpha(): Float {
        return when (type) {
            TrajectoryLineType.PRIMARY -> 0.96f
            TrajectoryLineType.GHOST -> 0.66f
            TrajectoryLineType.COLLISION -> 0.82f
            TrajectoryLineType.REBOUND -> 0.58f
            TrajectoryLineType.POCKET -> 0.86f
        } * confidence.coerceIn(0.35f, 1f)
    }

    private fun TrajectoryLineType.color(): Color {
        return when (this) {
            TrajectoryLineType.PRIMARY -> Color.White
            TrajectoryLineType.GHOST -> Color(0xFFFF4DFF)
            TrajectoryLineType.COLLISION -> Color(0xFFFFD33D)
            TrajectoryLineType.REBOUND -> Color(0xFF4DA3FF)
            TrajectoryLineType.POCKET -> Color(0xFF38E27D)
        }
    }

    private fun BallKind.color(): Color {
        return when (this) {
            BallKind.CUE -> Color.White
            BallKind.TARGET -> Color(0xFFFFD33D)
            BallKind.OBJECT -> Color(0xFF4DA3FF)
            BallKind.GHOST -> Color(0xFFFF4DFF)
        }
    }
}
