package com.aethermind.ui

import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.aethermind.execution.AetherRuntime
import com.aethermind.execution.VisionShotIntegration

/**
 * Aether Developer Overlay Service
 * 
 * Stub-Driven Development: ใช้ Mock Data ทดสอบ UI ก่อน
 * จากนั้นค่อยเปลี่ยนเป็นข้อมูลจริงจาก VisionProcessor (C++)
 * 
 * Key Features:
 * - Canvas Overlay ทับเกม 8 Ball Pool
 * - CoordinateMapper สำหรับแปลงพิกัด Engine → Screen
 * - Debug Menu สำหรับเปิด/ปิด ฟีเจอร์
 * - Mock Ball Positions สำหรับทดสอบ
 */
class AetherDevOverlayService : LifecycleService() {

    private var windowManager: android.view.WindowManager? = null
    private var canvasView: ComposeView? = null
    private var menuView: ComposeView? = null

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager

        // ====================================================================
        // 1. Canvas Overlay Window - ทับเกม ไม่รับ touch (FLAG_NOT_TOUCHABLE)
        // ====================================================================
        val canvasParams = android.view.WindowManager.LayoutParams(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        canvasView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AetherDevOverlayService)
            setContent {
                AetherCanvasOverlay()
            }
        }

        windowManager?.addView(canvasView, canvasParams)

        // ====================================================================
        // 2. Floating Menu Window - ลอยมุมขวาบน รับ touch ได้
        // ====================================================================
        menuView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AetherDevOverlayService)
            setContent {
                MaterialTheme {
                    AetherFloatingMenuRoot(
                        state = FloatingMenuState(
                            engineStatus = EngineStatus.Ready,
                            visionActive = true,
                            overlayVisible = true,
                            trajectoryReady = true,
                            executionLocked = true,
                            fps = 30,
                            ballCount = 8,
                            confidence = 91,
                            targetLabel = "Yellow Ball",
                            modeLabel = "PROPOSE ONLY"
                        ),
                        onClose = {
                            stopSelf()
                        },
                        onHideHud = {
                            // TODO: hide canvas overlay
                        },
                        onReset = {
                            // TODO: reset runtime state
                        },
                        onOpenPermissions = {
                            // TODO: open settings screen
                        },
                        onToggleVision = {
                            // TODO: enable/disable vision layer
                        },
                        onToggleHud = {
                            // TODO: show/hide HUD layer
                        }
                    )
                }
            }
        }

        windowManager?.addView(menuView, createMenuLayoutParams())
    }

    override fun onDestroy() {
        super.onDestroy()
        canvasView?.let { windowManager?.removeView(it) }
        menuView?.let { windowManager?.removeView(it) }
        canvasView = null
        menuView = null
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    // ========================================================================
    // MOCK DATA - Ball Positions (Normalized 0.0-1.0)
    // ========================================================================

    // ใช้ BallPosition จาก CoordinateMapper.kt
    private val mockBalls = listOf(
        BallPosition(id = 0, normX = 0.5f, normY = 0.7f, isCue = true),    // ลูกขาว (Cue ball)
        BallPosition(id = 9, normX = 0.5f, normY = 0.4f, isCue = false)   // ลูกเป้าหมาย (Target ball)
    )
    
    // เส้นวิถีจาก Mock Data
    private val mockTrajectory = TrajectoryLine.fromCueToTargetExtended(
        cue = mockBalls.first { it.isCue },
        target = mockBalls.first { !it.isCue },
        extensionFactor = 0.5f
    )

    // ========================================================================
    // COMPOSE UI - Aether Developer Overlay
    // ========================================================================

    // AUTO PLAY State
    private var visionIntegration: VisionShotIntegration? by mutableStateOf(null)
    private var isAutoPlayRunning by mutableStateOf(false)
    
    // Target package for auto play
    private val targetPackage = "com.miniclip.eightballpool"

    @Composable
    fun AetherCanvasOverlay() {
        // State สำหรับควบคุมการวาด Canvas overlay
        var showAimLine by remember { mutableStateOf(true) }
        var showBallMarkers by remember { mutableStateOf(true) }
        var showGhostBall by remember { mutableStateOf(true) }
        var showPowerBar by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            
            // =================================================================
            // 1. Canvas - วาดเส้นและตัวบอลทับเกม (ใช้ CoordinateMapper)
            // =================================================================
            if (showAimLine || showBallMarkers || showGhostBall || showPowerBar) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // ขนาดหน้าจอปัจจุบัน (จาก Canvas)
                    val currentSize = IntSize(size.width.toInt(), size.height.toInt())
                    
                    // คำนวณ scale factor สำหรับ ball radius
                    val baseRadius = CoordinateMapper.scaleRadius(24f, currentSize)
                    val ghostRadius = CoordinateMapper.scaleRadius(28f, currentSize)
                    
                    // --- วาดตัวบอล (Ball Markers) ---
                    if (showBallMarkers) {
                        mockBalls.forEach { ball ->
                            // แปลงพิกัด Normalized → Screen Pixel
                            val screenPos = ball.toScreenOffset(currentSize)
                            
                            // วาดวงกลม (Stroke ไม่ต้อง Fill)
                            drawCircle(
                                color = if (ball.isCue) Color.White else Color.Yellow,
                                radius = baseRadius,
                                center = screenPos,
                                style = Stroke(width = 4f)
                            )
                            
                            // วาดจุดเล็งในวงกลม
                            drawCircle(
                                color = if (ball.isCue) Color.White.copy(alpha = 0.5f) 
                                        else Color.Yellow.copy(alpha = 0.5f),
                                radius = 6f,
                                center = screenPos
                            )
                        }
                    }

                    // --- วาดเส้นเล็ง (Aim Line) ---
                    if (showAimLine) {
                        // ใช้ TrajectoryLine.toScreenOffsets() สำหรับแปลงพิกัด
                        val (startPx, endPx) = mockTrajectory.toScreenOffsets(currentSize)
                        
                        // เส้นหลัก (สีฟ้าอมเขียว)
                        drawLine(
                            color = Color.Cyan.copy(alpha = 0.9f),
                            start = startPx,
                            end = endPx,
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        
                        // เส้นรอง (สีขาวโปร่ง - ขยายต่อไปอีก)
                        val extendedTraj = CoordinateMapper.extendLine(
                            mockTrajectory.startX, mockTrajectory.startY,
                            mockTrajectory.endX, mockTrajectory.endY,
                            0.3f  // ยาวขึ้น 30%
                        )
                        val extendedEnd = CoordinateMapper.mapToScreen(
                            extendedTraj.first, extendedTraj.second, currentSize
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = startPx,
                            end = extendedEnd,
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // --- วาด Ghost Ball (ตำแหน่งที่ลูกขาวจะไปชน) ---
                    if (showGhostBall) {
                        val cue = mockBalls.first { it.isCue }
                        val target = mockBalls.first { !it.isCue }
                        
                        // คำนวณตำแหน่ง Ghost Ball (ขยายจาก target ไปอีกนิดหน่อย)
                        val ghostTraj = CoordinateMapper.extendLine(
                            cue.normX, cue.normY,
                            target.normX, target.normY,
                            0.15f  // ขยาย 15% ผ่าน target
                        )
                        val ghostPos = CoordinateMapper.mapToScreen(
                            ghostTraj.first, ghostTraj.second, currentSize
                        )
                        
                        // วาด Ghost Ball (วงกลมประ)
                        drawCircle(
                            color = Color.Magenta.copy(alpha = 0.7f),
                            radius = ghostRadius,
                            center = ghostPos,
                            style = Stroke(width = 3f)
                        )
                        
                        // วาดเส้นประระหว่าง target กับ ghost
                        val targetPos = target.toScreenOffset(currentSize)
                        drawLine(
                            color = Color.Magenta.copy(alpha = 0.4f),
                            start = targetPos,
                            end = ghostPos,
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    // --- วาด Power Bar (Mock) ---
                    if (showPowerBar) {
                        val power = 0.7f // Mock power level
                        val barWidth = 240f
                        val barHeight = 24f
                        val barX = size.width / 2 - barWidth / 2
                        val barY = size.height * 0.85f
                        
                        // Background bar
                        drawRoundRect(
                            color = Color.Gray.copy(alpha = 0.6f),
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                        
                        // Power fill
                        val fillColor = when {
                            power > 0.8f -> Color.Red
                            power > 0.5f -> Color.Yellow
                            else -> Color.Green
                        }
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth * power, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                    }
                }
            }

        }
    }

    // ========================================================================
    // MENU WINDOW LAYOUT PARAMS (Floating Menu - touchable, top-end corner)
    // ========================================================================

    private fun createMenuLayoutParams(): android.view.WindowManager.LayoutParams {
        return android.view.WindowManager.LayoutParams(
            android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT,
            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
            x = 24
            y = 180
        }
    }
}