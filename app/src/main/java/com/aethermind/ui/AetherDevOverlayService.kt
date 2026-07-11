package com.aethermind.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.aether.renderer.AetherIntegrationLoop
import com.aethermind.execution.AutoPlayController
import com.aethermind.ui.overlay.AiSkillLevel
import com.aethermind.ui.overlay.BallKind
import com.aethermind.ui.overlay.MockPoolOverlayProvider
import com.aethermind.ui.overlay.OverlayUiState
import com.aethermind.ui.overlay.TrajectoryLineType

/**
 * Runtime-safe overlay service for AT164.
 *
 * Why this file is deliberately NOT Compose-based:
 * - The AT163 build fix removed ViewTreeViewModelStoreOwner/ViewTreeSavedStateRegistryOwner
 *   to make compileDebugKotlin pass.
 * - On real devices, ComposeView hosted directly inside a Service can still crash when the
 *   expected tree owners are absent or resolved inconsistently by AndroidX versions.
 * - This implementation uses plain Android Views + a custom Canvas View. It needs no
 *   ViewTree owner plumbing, so it is safer for SYSTEM_ALERT_WINDOW overlays on MIUI/Android.
 *
 * Window model:
 * - Canvas overlay window: MATCH_PARENT + NOT_TOUCHABLE. It draws aim/ball markers and never
 *   steals touches from the game/home screen.
 * - Menu overlay window: WRAP_CONTENT + touchable. It can be dragged and controls overlay state.
 */
class AetherDevOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var canvasView: AimOverlayView? = null
    private var menuView: View? = null
    private var menuParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var tickerRunning = false

    private var showHud = true
    private var showAimGuide = true
    private var showVisionMarkers = true
    private var showDebugLabels = false
    private var aiSkillLevel = AiSkillLevel.INTERMEDIATE
    private var autoPlayEnabled = false
    private var autoPlayStatus = "OFF"
    private var autoPlayController: AutoPlayController? = null

    private var overlayState = OverlayUiState()

    private var titleText: TextView? = null
    private var modeText: TextView? = null
    private var statusText: TextView? = null
    private var visionText: TextView? = null
    private var autoText: TextView? = null
    private var skillText: TextView? = null
    private var hudButton: Button? = null
    private var aimButton: Button? = null
    private var visionButton: Button? = null
    private var debugButton: Button? = null
    private var autoButton: Button? = null

    private val frameTicker = object : Runnable {
        override fun run() {
            if (!tickerRunning) return
            updateOverlayState()
            autoPlayController?.onFrame(overlayState)
            canvasView?.state = overlayState
            canvasView?.invalidate()
            updateMenuTexts()
            handler.postDelayed(this, FRAME_DELAY_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        aiSkillLevel = readNativeSkillLevel()
        autoPlayEnabled = readNativeAutoPlayEnabled()
        autoPlayStatus = if (autoPlayEnabled) "ARMED" else "OFF"
        autoPlayController = AutoPlayController(this) { status ->
            autoPlayStatus = status
        }
        updateOverlayState()

        val ok = createOverlayWindowsSafely()
        if (!ok) {
            stopSelf()
            return
        }

        startTicker()
    }

    override fun onDestroy() {
        stopTicker()
        autoPlayController?.close()
        autoPlayController = null
        removeOverlayViews()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlayWindowsSafely(): Boolean {
        val wm = windowManager ?: return false
        if (!Settings.canDrawOverlays(this)) return false

        return runCatching {
            val canvas = AimOverlayView(this).apply {
                state = overlayState
            }
            canvasView = canvas
            wm.addView(canvas, createCanvasLayoutParams())

            val menu = createMenuView()
            menuView = menu
            val params = createMenuLayoutParams()
            menuParams = params
            wm.addView(menu, params)
        }.onFailure {
            removeOverlayViews()
        }.isSuccess
    }

    private fun createCanvasLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun createMenuLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 180
        }
    }

    private fun createMenuView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(Color.argb(238, 18, 22, 32), dp(22).toFloat(), Color.argb(70, 255, 255, 255))
            minimumWidth = dp(292)
        }

        titleText = label("Aether Engine", 18f, Color.WHITE, true)
        modeText = label("PROPOSE ONLY", 11f, Color.rgb(160, 170, 200), false)
        statusText = label("Overlay: STARTING", 13f, Color.rgb(70, 255, 140), false)
        visionText = label("Vision: mock / FPS 30", 13f, Color.rgb(220, 230, 255), false)
        skillText = label("AI Skill: Smart", 13f, Color.rgb(255, 205, 90), false)
        autoText = label("Auto Play: OFF", 13f, Color.rgb(150, 170, 210), false)

        root.addView(titleText)
        root.addView(modeText)
        root.addView(space(8))
        root.addView(statusText)
        root.addView(visionText)
        root.addView(skillText)
        root.addView(autoText)
        root.addView(space(10))

        val row1 = row()
        hudButton = button("HUD", ::toggleHud)
        aimButton = button("Aim", ::toggleAim)
        visionButton = button("Vision", ::toggleVision)
        row1.addView(hudButton, weightParams())
        row1.addView(aimButton, weightParams())
        row1.addView(visionButton, weightParams())
        root.addView(row1)

        val row2 = row()
        debugButton = button("Debug", ::toggleDebug)
        autoButton = button("Auto", ::toggleAutoPlay)
        val close = button("Close") { stopSelf() }
        row2.addView(debugButton, weightParams())
        row2.addView(autoButton, weightParams())
        row2.addView(close, weightParams())
        root.addView(row2)

        root.addView(space(8))
        root.addView(label("Skill Level", 12f, Color.rgb(160, 170, 200), true))

        val row3 = row()
        row3.addView(button("Basic") { setAiSkillLevel(AiSkillLevel.BEGINNER) }, weightParams())
        row3.addView(button("Smart") { setAiSkillLevel(AiSkillLevel.INTERMEDIATE) }, weightParams())
        row3.addView(button("Pro") { setAiSkillLevel(AiSkillLevel.ADVANCED) }, weightParams())
        root.addView(row3)

        val row4 = row()
        row4.addView(button("Reset") { resetOverlayState() }, weightParams())
        row4.addView(button("Perms") { openOverlaySettings() }, weightParams())
        root.addView(row4)

        installDrag(root)
        updateMenuTexts()
        return root
    }

    private fun installDrag(view: View) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0

        view.setOnTouchListener { _, event ->
            val params = menuParams ?: return@setOnTouchListener false
            val wm = windowManager ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    params.x = (startX - dx.toInt()).coerceAtLeast(0)
                    params.y = (startY + dy.toInt()).coerceAtLeast(0)
                    runCatching { wm.updateViewLayout(view, params) }
                    true
                }
                else -> false
            }
        }
    }

    private fun label(text: String, sp: Float, color: Int, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sp
            setTextColor(color)
            includeFontPadding = true
            if (bold) typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
    }

    private fun button(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 11f
            minHeight = dp(38)
            minimumHeight = dp(38)
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { action() }
        }
    }

    private fun row(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
    }

    private fun space(heightDp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
        }
    }

    private fun weightParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
    }

    private fun roundedBackground(fill: Int, radius: Float, stroke: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radius
            setStroke(dp(1), stroke)
        }
    }

    private fun toggleHud() {
        showHud = !showHud
        publishNow()
    }

    private fun toggleAim() {
        showAimGuide = !showAimGuide
        publishNow()
    }

    private fun toggleVision() {
        showVisionMarkers = !showVisionMarkers
        publishNow()
    }

    private fun toggleDebug() {
        showDebugLabels = !showDebugLabels
        publishNow()
    }

    private fun resetOverlayState() {
        showHud = true
        showAimGuide = true
        showVisionMarkers = true
        showDebugLabels = false
        autoPlayStatus = if (autoPlayEnabled) "ARMED" else "OFF"
        publishNow()
    }

    private fun publishNow() {
        updateOverlayState()
        canvasView?.state = overlayState
        canvasView?.invalidate()
        updateMenuTexts()
    }

    private fun updateOverlayState() {
        val metrics = resources.displayMetrics
        overlayState = MockPoolOverlayProvider.generate(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            timeMs = android.os.SystemClock.uptimeMillis(),
            showHud = showHud,
            showAimGuide = showAimGuide,
            showVisionMarkers = showVisionMarkers,
            showDebugLabels = showDebugLabels,
            aiSkillLevel = aiSkillLevel,
            autoPlayEnabled = autoPlayEnabled,
            autoPlayArmed = autoPlayEnabled && autoPlayStatus != "OFF",
            autoPlayStatus = autoPlayStatus,
            autoPlayIntervalMs = readNativeAutoPlayIntervalMs(),
            autoPlayPowerPx = readNativeAutoPlayPowerPx()
        )
    }

    private fun updateMenuTexts() {
        titleText?.text = "Aether Engine"
        modeText?.text = "${overlayState.modeLabel} / AI ${overlayState.aiSkillShortLabel}"
        statusText?.text = "Overlay: ${if (showHud) "ON" else "OFF"}  Aim: ${if (showAimGuide) "ON" else "OFF"}"
        visionText?.text = "Vision: ${if (showVisionMarkers) "mock" else "hidden"} / balls=${overlayState.ballCount} / ${overlayState.fps} FPS"
        skillText?.text = "AI Skill: ${overlayState.aiSkillLabel}"
        autoText?.text = "Auto Play: ${overlayState.autoPlayStatus} / ${overlayState.autoPlayIntervalMs}ms / ${overlayState.autoPlayPowerPx.toInt()}px"
        hudButton?.text = if (showHud) "HUD ON" else "HUD OFF"
        aimButton?.text = if (showAimGuide) "Aim ON" else "Aim OFF"
        visionButton?.text = if (showVisionMarkers) "Vision ON" else "Vision OFF"
        debugButton?.text = if (showDebugLabels) "Debug ON" else "Debug OFF"
        autoButton?.text = if (autoPlayEnabled) "Auto ON" else "Auto OFF"
    }

    private fun startTicker() {
        tickerRunning = true
        handler.removeCallbacks(frameTicker)
        handler.post(frameTicker)
    }

    private fun stopTicker() {
        tickerRunning = false
        handler.removeCallbacks(frameTicker)
    }

    private fun readNativeSkillLevel(): AiSkillLevel {
        val nativeValue = runCatching { AetherIntegrationLoop.nativeSkillLevel() }
            .getOrDefault(AiSkillLevel.INTERMEDIATE.nativeValue)
        return AiSkillLevel.fromNativeValue(nativeValue)
    }

    private fun setAiSkillLevel(level: AiSkillLevel) {
        val accepted = runCatching {
            AetherIntegrationLoop.nativeSetSkillLevel(level.nativeValue)
        }.getOrDefault(false)
        aiSkillLevel = if (accepted) readNativeSkillLevel() else level
        publishNow()
    }

    private fun readNativeAutoPlayEnabled(): Boolean {
        return runCatching { AetherIntegrationLoop.nativeAutoPlayEnabled() }
            .getOrDefault(false)
    }

    private fun readNativeAutoPlayIntervalMs(): Int {
        return runCatching { AetherIntegrationLoop.nativeAutoPlayIntervalMs() }
            .getOrDefault(1200)
    }

    private fun readNativeAutoPlayPowerPx(): Float {
        return runCatching { AetherIntegrationLoop.nativeAutoPlaySwipePowerPx() }
            .getOrDefault(420f)
    }

    private fun toggleAutoPlay() {
        val requested = !autoPlayEnabled
        val accepted = runCatching { AetherIntegrationLoop.nativeSetAutoPlayEnabled(requested) }
            .getOrDefault(false)
        autoPlayEnabled = if (accepted) readNativeAutoPlayEnabled() else requested
        if (!autoPlayEnabled) {
            autoPlayStatus = "OFF"
            autoPlayController?.stopRuntime("AUTO_PLAY_TOGGLED_OFF")
        } else {
            autoPlayStatus = "ARMED"
        }
        publishNow()
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    private fun removeOverlayViews() {
        val wm = windowManager
        menuView?.let { view -> runCatching { wm?.removeView(view) } }
        canvasView?.let { view -> runCatching { wm?.removeView(view) } }
        menuView = null
        canvasView = null
        menuParams = null
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private class AimOverlayView(context: Context) : View(context) {
        var state: OverlayUiState = OverlayUiState()

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val s = state
            if (!s.showHud) return

            if (s.showAimGuide) drawTrajectory(canvas, s)
            if (s.showVisionMarkers) drawBalls(canvas, s)
            if (s.showDebugLabels) drawDebug(canvas, s)
        }

        private fun drawTrajectory(canvas: Canvas, s: OverlayUiState) {
            for (segment in s.trajectory) {
                val color = when (segment.type) {
                    TrajectoryLineType.PRIMARY -> Color.argb(230, 255, 255, 255)
                    TrajectoryLineType.GHOST -> Color.argb(170, 220, 235, 255)
                    TrajectoryLineType.COLLISION -> Color.argb(210, 255, 204, 64)
                    TrajectoryLineType.REBOUND -> Color.argb(210, 90, 180, 255)
                    TrajectoryLineType.POCKET -> Color.argb(210, 60, 255, 130)
                }
                linePaint.color = color
                linePaint.strokeWidth = if (segment.type == TrajectoryLineType.PRIMARY) 5f else 3.5f
                linePaint.pathEffect = when (segment.type) {
                    TrajectoryLineType.GHOST,
                    TrajectoryLineType.REBOUND -> DashPathEffect(floatArrayOf(18f, 13f), 0f)
                    else -> null
                }
                canvas.drawLine(segment.start.x, segment.start.y, segment.end.x, segment.end.y, linePaint)
                linePaint.pathEffect = null

                fillPaint.color = color
                canvas.drawCircle(segment.end.x, segment.end.y, 7f, fillPaint)
            }
        }

        private fun drawBalls(canvas: Canvas, s: OverlayUiState) {
            for (ball in s.balls) {
                val color = when (ball.kind) {
                    BallKind.CUE -> Color.argb(230, 255, 255, 255)
                    BallKind.TARGET -> Color.argb(230, 255, 220, 65)
                    BallKind.GHOST -> Color.argb(95, 125, 210, 255)
                    BallKind.OBJECT -> Color.argb(190, 255, 100, 100)
                }
                linePaint.color = color
                linePaint.strokeWidth = if (ball.kind == BallKind.GHOST) 3f else 4f
                fillPaint.color = Color.argb(if (ball.kind == BallKind.GHOST) 32 else 48, Color.red(color), Color.green(color), Color.blue(color))
                canvas.drawCircle(ball.center.x, ball.center.y, ball.radiusPx, fillPaint)
                canvas.drawCircle(ball.center.x, ball.center.y, ball.radiusPx + 3f, linePaint)
            }
        }

        private fun drawDebug(canvas: Canvas, s: OverlayUiState) {
            textPaint.color = Color.argb(220, 180, 255, 220)
            canvas.drawText("AE overlay mock / balls=${s.ballCount} / skill=${s.aiSkillShortLabel}", 28f, 52f, textPaint)
            canvas.drawText("auto=${s.autoPlayStatus} / ${s.autoPlayIntervalMs}ms", 28f, 86f, textPaint)
        }
    }

    private companion object {
        const val FRAME_DELAY_MS = 33L
    }
}
