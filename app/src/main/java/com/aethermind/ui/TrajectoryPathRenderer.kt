package com.aethermind.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * ประเภทเส้นวิถีที่วาดบน Canvas overlay
 */
enum class TrajectoryLineType {
    PRIMARY,    // เส้นยิงหลัก (สีขาว)
    COLLISION,  // เส้นชนลูก (สีเหลือง)
    REBOUND,    // เส้นสะท้อน (สีฟ้า)
    POCKET      // เส้นไปชิ่ง/หลุม (สีเขียว #00FF88)
}

/**
 * เส้นวิถี 1 เส้น (ประกอบด้วยจุด screen coordinate หลายจุด)
 */
data class TrajectorySegment(
    val type: TrajectoryLineType,
    val points: List<Offset>,
    val dashed: Boolean = false,
    val width: Float = 4f
)

/**
 * วงกลม marker ที่จุดชน / จุดกระทบ
 */
data class CollisionMarker(
    val position: Offset,
    val radius: Float = 9f,
    val color: Color = Color.Magenta
)

/**
 * TrajectoryPathRenderer - แยก logic การวาดเส้น trajectory ออกจาก Service
 *
 * วาด:
 * - เส้น trajectory ตาม line type (สีแยกประเภท)
 * - เส้นคาดการณ์แบบ dashed
 * - วงกลม marker ที่จุดชน
 */
object TrajectoryPathRenderer {

    private val LINE_COLORS = mapOf(
        TrajectoryLineType.PRIMARY to Color.White,
        TrajectoryLineType.COLLISION to Color.Yellow,
        TrajectoryLineType.REBOUND to Color.Cyan,
        TrajectoryLineType.POCKET to Color(0xFF00FF88)
    )

    fun drawSegments(scope: DrawScope, segments: List<TrajectorySegment>) {
        for (seg in segments) {
            if (seg.points.size < 2) continue
            val color = LINE_COLORS[seg.type] ?: Color.White
            val path = Path().apply {
                moveTo(seg.points.first().x, seg.points.first().y)
                for (i in 1 until seg.points.size) {
                    lineTo(seg.points[i].x, seg.points[i].y)
                }
            }
            val style = if (seg.dashed) {
                Stroke(
                    width = seg.width,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            } else {
                Stroke(width = seg.width)
            }
            scope.drawPath(path = path, color = color, style = style)
        }
    }

    fun drawMarkers(scope: DrawScope, markers: List<CollisionMarker>) {
        for (m in markers) {
            scope.drawCircle(color = m.color, radius = m.radius, center = m.position)
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = m.radius * 0.4f,
                center = m.position
            )
        }
    }

    fun drawCueMarker(scope: DrawScope, center: Offset, radius: Float) {
        scope.drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 3f)
        )
        scope.drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = radius * 0.6f,
            center = center
        )
    }
}
