package com.aethermind.ui

import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner

/**
 * Aether Developer Overlay Service
 *
 * Pool Game Overlay จากคลิปอ้างอิง - 2 Windows Overlay System:
 *
 * AetherDevOverlayService
 * ├── Canvas Overlay Window (MATCH_PARENT, FLAG_NOT_TOUCHABLE)
 * │   ├── วาดเส้น aim / trajectory (AetherAimCanvas)
 * │   ├── วาดวงกลม cue marker
 * │   ├── วาด pocket target / collision point
 * │   └── ไม่รับ touch เพื่อไม่บังเกม
 * │
 * └── Floating Menu Window (WRAP_CONTENT, รับ touch)
 *     ├── Mini Bubble: AE 91%
 *     ├── Expanded Menu: Vision / HUD / Aim / Debug
 *     └── รับ touch เฉพาะตัวเมนู
 *
 * Shared state (PoolOverlayState) ใช้ MutableState ร่วมกันระหว่าง 2 ComposeView
 * ผ่าน global snapshot ของ Compose → กดปุ่มในเมนูจะอัปเดต Canvas ทันที
 */
class AetherDevOverlayService : LifecycleService() {

    private var windowManager: android.view.WindowManager? = null
    private var canvasView: ComposeView? = null
    private var menuView: ComposeView? = null

    // Shared UI state - observable across both ComposeView windows (global snapshot)
    private val overlayState = PoolOverlayState()

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
                AetherAimCanvas(
                    trajectory = overlayState.trajectory.value,
                    showCanvasOverlay = overlayState.showCanvasOverlay.value,
                    showAimGuide = overlayState.showAimGuide.value,
                    showReboundLines = overlayState.showReboundLines.value,
                    showCollisionMarkers = overlayState.showCollisionMarkers.value,
                    showBallMarkers = overlayState.showBallMarkers.value,
                    showDebug = overlayState.showDebug.value
                )
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
                            overlayVisible = overlayState.showCanvasOverlay.value,
                            trajectoryReady = true,
                            executionLocked = true,
                            fps = 30,
                            ballCount = overlayState.ballCount.value,
                            confidence = overlayState.confidence.value,
                            targetLabel = "Yellow Ball",
                            modeLabel = "PROPOSE ONLY"
                        ),
                        onClose = {
                            stopSelf()
                        },
                        onHideHud = {
                            // HUD = toggle canvas overlay
                            overlayState.showCanvasOverlay.value = !overlayState.showCanvasOverlay.value
                        },
                        onReset = {
                            // Reset = reset mock trajectory
                            overlayState.resetTrajectory()
                        },
                        onOpenPermissions = {
                            // TODO: open settings screen
                        },
                        onToggleVision = {
                            // Vision = toggle mock ball markers
                            overlayState.showBallMarkers.value = !overlayState.showBallMarkers.value
                        },
                        onToggleHud = {
                            // HUD = toggle canvas overlay
                            overlayState.showCanvasOverlay.value = !overlayState.showCanvasOverlay.value
                        },
                        onToggleAim = {
                            // Aim = toggle aim guide
                            overlayState.showAimGuide.value = !overlayState.showAimGuide.value
                        },
                        onToggleDebug = {
                            // Debug = toggle debug labels
                            overlayState.showDebug.value = !overlayState.showDebug.value
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
    // SHARED OVERLAY STATE
    // ========================================================================

    class PoolOverlayState {
        val showCanvasOverlay = mutableStateOf(true)
        val showAimGuide = mutableStateOf(true)
        val showReboundLines = mutableStateOf(true)
        val showCollisionMarkers = mutableStateOf(true)
        val showBallMarkers = mutableStateOf(true)
        val showDebug = mutableStateOf(false)
        val showFloatingMenu = mutableStateOf(true)
        val confidence = mutableStateOf(91)
        val ballCount = mutableStateOf(8)
        val trajectory = mutableStateOf(buildMockTrajectory())

        fun resetTrajectory() {
            trajectory.value = buildMockTrajectory()
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
