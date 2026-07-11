package com.aethermind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * จุดบนโต๊ะในพิกัด Normalized (0.0 - 1.0)
 * x = 0 ซ้าย, 1 ขวา | y = 0 บน, 1 ล่าง
 */
data class AimPoint(val x: Float, val y: Float)

/**
 * ข้อมูล trajectory ทั้งหมดสำหรับวาด overlay
 */
data class PoolTrajectory(
    val cueBall: AimPoint,
    val targetBall: AimPoint,
    val primaryAim: List<AimPoint>,
    val collisionPoint: AimPoint,
    val reboundLines: List<List<AimPoint>>,
    val pocketTarget: AimPoint?,
    val ballCount: Int = 8,
    val confidence: Int = 91
)

/**
 * Mock trajectory จากคลิปอ้างอิง:
 * - cue ball อยู่ช่วงล่างกลางโต๊ะ
 * - aim line วิ่งไปยังลูกเป้าหมายด้านบน
 * - มีเส้นสะท้อนเข้าชิ่งและ pocket
 *
 * ใช้ข้อมูล mock ก่อน ยังไม่ต้องต่อ Vision จริง
 */
fun buildMockTrajectory(): PoolTrajectory {
    val cue = AimPoint(0.5f, 0.72f)       // ลูกขาว (ล่างกลาง)
    val target = AimPoint(0.5f, 0.34f)    // ลูกเป้าหมาย (บน)
    return PoolTrajectory(
        cueBall = cue,
        targetBall = target,
        primaryAim = listOf(cue, target),
        collisionPoint = target,
        reboundLines = listOf(
            // สะท้อนขึ้นไปชนขอบบน แล้วเข้าชิ่งมุมขวาบน
            listOf(AimPoint(0.5f, 0.34f), AimPoint(0.5f, 0.06f), AimPoint(0.92f, 0.06f)),
            // สะท้อนซ้าย แล้วเข้าชิ่งมุมซ้ายบน
            listOf(AimPoint(0.5f, 0.34f), AimPoint(0.08f, 0.30f), AimPoint(0.04f, 0.06f))
        ),
        pocketTarget = AimPoint(0.92f, 0.06f),
        ballCount = 8,
        confidence = 91
    )
}

/**
 * AetherAimCanvas - Composable Canvas สำหรับวาดเส้นช่วยเล็ง (Pool overlay)
 *
 * วาด:
 * - cue ball marker (วงกลมขาว)
 * - primary aim line (สีขาว, เส้นทึบ)
 * - collision line (สีเหลือง, dashed)
 * - rebound lines (สีฟ้า, dashed) หลายจังหวะ
 * - pocket target line (สีเขียว #00FF88, dashed)
 * - วง marker ที่จุดชน
 *
 * รองรับข้อมูล trajectory เป็น list ของจุด screen coordinate
 * (แปลงจาก Normalized → Screen ภายใน Canvas ด้วยขนาดหน้าจอ)
 */
@Composable
fun AetherAimCanvas(
    trajectory: PoolTrajectory,
    showCanvasOverlay: Boolean,
    showAimGuide: Boolean,
    showReboundLines: Boolean,
    showCollisionMarkers: Boolean,
    showBallMarkers: Boolean,
    showDebug: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (!showCanvasOverlay) return@Canvas

        val w = size.width
        val h = size.height
        fun toScreen(p: AimPoint) = Offset(p.x * w, p.y * h)

        val cue = toScreen(trajectory.cueBall)
        val target = toScreen(trajectory.targetBall)
        val col = toScreen(trajectory.collisionPoint)

        val segments = mutableListOf<TrajectorySegment>()

        // เส้นยิงหลัก (สีขาว)
        if (showAimGuide && trajectory.primaryAim.size >= 2) {
            segments.add(
                TrajectorySegment(
                    TrajectoryLineType.PRIMARY,
                    trajectory.primaryAim.map { toScreen(it) }
                )
            )
        }

        // เส้นชนลูก (สีเหลือง, dashed)
        segments.add(
            TrajectorySegment(
                TrajectoryLineType.COLLISION,
                listOf(cue, col),
                dashed = true
            )
        )

        // เส้นสะท้อน (สีฟ้า, dashed) หลายจังหวะ
        if (showReboundLines) {
            for (line in trajectory.reboundLines) {
                if (line.size >= 2) {
                    segments.add(
                        TrajectorySegment(
                            TrajectoryLineType.REBOUND,
                            line.map { toScreen(it) },
                            dashed = true
                        )
                    )
                }
            }
        }

        // เส้นไปชิ่ง/หลุม (สีเขียว, dashed)
        trajectory.pocketTarget?.let { p ->
            segments.add(
                TrajectorySegment(
                    TrajectoryLineType.POCKET,
                    listOf(col, toScreen(p)),
                    dashed = true
                )
            )
        }

        TrajectoryPathRenderer.drawSegments(this, segments)

        // วง marker ที่จุดชน
        if (showCollisionMarkers) {
            TrajectoryPathRenderer.drawMarkers(
                this,
                listOf(
                    CollisionMarker(col, radius = 10f, color = Color.Magenta),
                    CollisionMarker(target, radius = 13f, color = Color.Yellow)
                )
            )
        }

        // วง marker ลูกขาว + ลูกเป้า
        if (showBallMarkers) {
            TrajectoryPathRenderer.drawCueMarker(this, cue, radius = 18f)
            drawCircle(
                color = Color.Yellow,
                radius = 14f,
                center = target,
                style = Stroke(width = 3f)
            )
        }

        // debug labels = จุด vertex เล็กๆ ทุกจุด
        if (showDebug) {
            for (seg in segments) {
                for (pt in seg.points) {
                    drawCircle(Color(0xFF00FF88), radius = 3f, center = pt)
                }
            }
        }
    }
}
