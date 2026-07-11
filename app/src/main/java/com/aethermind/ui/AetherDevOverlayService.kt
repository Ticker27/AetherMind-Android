package com.aethermind.ui

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.aether.renderer.AetherIntegrationLoop
import com.aethermind.execution.AutoPlayController
import com.aethermind.ui.overlay.AetherAimCanvas
import com.aethermind.ui.overlay.AiSkillLevel
import com.aethermind.ui.overlay.MockPoolOverlayProvider
import com.aethermind.ui.overlay.OverlayUiState

/**
 * Real-time pool overlay service.
 *
 * The service intentionally owns two independent application-overlay windows:
 *
 * 1. Canvas window: full screen, translucent, not touchable. It draws aim lines,
 *    ghost ball, collision markers, rebound lines, and vision markers without
 *    stealing input from the game below it.
 *
 * 2. Floating menu window: wrap-content and touchable. It controls which visual
 *    layers are visible and can be dragged without making the full screen overlay
 *    consume touch events.
 *
 * Execution remains locked. This layer is visual/propose-only and does not send
 * gestures or taps.
 */
class AetherDevOverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateController by lazy { SavedStateRegistryController.create(this) }
    private val serviceViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    private var windowManager: WindowManager? = null
    private var canvasView: ComposeView? = null
    private var menuView: ComposeView? = null
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

    private var overlayState by mutableStateOf(OverlayUiState())

    private val frameTicker = object : Runnable {
        override fun run() {
            if (!tickerRunning) return
            updateMockOverlayState()
            autoPlayController?.onFrame(overlayState)
            handler.postDelayed(this, FRAME_DELAY_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()

        savedStateController.performAttach()
        savedStateController.performRestore(null)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        aiSkillLevel = readNativeSkillLevel()
        autoPlayEnabled = readNativeAutoPlayEnabled()
        autoPlayStatus = if (autoPlayEnabled) "ARMED" else "OFF"
        autoPlayController = AutoPlayController(this) { status ->
            autoPlayStatus = status
        }
        overlayState = createInitialState()

        createCanvasWindow()
        createMenuWindow()
        startTicker()
    }

    override fun onDestroy() {
        stopTicker()
        autoPlayController?.close()
        autoPlayController = null
        removeOverlayViews()
        serviceViewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun createCanvasWindow() {
        val params = createCanvasLayoutParams()
        canvasView = ComposeView(this).apply {
            installComposeOwners()
            setContent {
                MaterialTheme {
                    AetherAimCanvas(state = overlayState)
                }
            }
        }
        windowManager?.addView(canvasView, params)
    }

    private fun createMenuWindow() {
        val params = createMenuLayoutParams()
        menuParams = params
        menuView = ComposeView(this).apply {
            installComposeOwners()
            setContent {
                MaterialTheme {
                    AetherFloatingMenuRoot(
                        state = overlayState,
                        engineStatus = EngineStatus.Ready,
                        onClose = { stopSelf() },
                        onHideHud = {
                            showHud = false
                            publishStateNow()
                        },
                        onReset = {
                            showHud = true
                            showAimGuide = true
                            showVisionMarkers = true
                            showDebugLabels = false
                            publishStateNow()
                        },
                        onOpenPermissions = { openOverlaySettings() },
                        onToggleVision = {
                            showVisionMarkers = !showVisionMarkers
                            publishStateNow()
                        },
                        onToggleHud = {
                            showHud = !showHud
                            publishStateNow()
                        },
                        onToggleAim = {
                            showAimGuide = !showAimGuide
                            publishStateNow()
                        },
                        onToggleDebug = {
                            showDebugLabels = !showDebugLabels
                            publishStateNow()
                        },
                        onSetAiSkillLevel = { level ->
                            setAiSkillLevel(level)
                        },
                        onToggleAutoPlay = {
                            setAutoPlayEnabled(!autoPlayEnabled)
                        },
                        onDrag = { dx, dy -> moveMenuWindow(dx, dy) }
                    )
                }
            }
        }
        windowManager?.addView(menuView, params)
    }

    private fun ComposeView.installComposeOwners() {
        setViewTreeLifecycleOwner(this@AetherDevOverlayService)
        ViewTreeViewModelStoreOwner.set(this, this@AetherDevOverlayService)
        ViewTreeSavedStateRegistryOwner.set(this, this@AetherDevOverlayService)
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

    private fun moveMenuWindow(dx: Float, dy: Float) {
        val view = menuView ?: return
        val params = menuParams ?: return

        // With TOP|END gravity, positive x moves the window inward from the end
        // edge. Dragging right should visually move toward the edge, therefore dx
        // is subtracted while y follows the drag direction.
        params.x = (params.x - dx.toInt()).coerceAtLeast(0)
        params.y = (params.y + dy.toInt()).coerceAtLeast(0)

        runCatching { windowManager?.updateViewLayout(view, params) }
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

    private fun publishStateNow() {
        updateMockOverlayState()
    }

    private fun updateMockOverlayState() {
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

    private fun createInitialState(): OverlayUiState {
        val metrics = resources.displayMetrics
        return MockPoolOverlayProvider.generate(
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


    private fun readNativeSkillLevel(): AiSkillLevel {
        val nativeValue = runCatching { AetherIntegrationLoop.nativeSkillLevel() }
            .getOrDefault(AiSkillLevel.INTERMEDIATE.nativeValue)
        return AiSkillLevel.fromNativeValue(nativeValue)
    }

    private fun setAiSkillLevel(level: AiSkillLevel) {
        val accepted = runCatching {
            AetherIntegrationLoop.nativeSetSkillLevel(level.nativeValue)
        }.getOrDefault(false)

        aiSkillLevel = if (accepted) {
            readNativeSkillLevel()
        } else {
            // Keep the UI responsive even if native loading failed in a preview or
            // restricted environment. Real device builds should return true.
            level
        }

        publishStateNow()
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

    private fun setAutoPlayEnabled(enabled: Boolean) {
        val accepted = runCatching {
            AetherIntegrationLoop.nativeSetAutoPlayEnabled(enabled)
        }.getOrDefault(false)

        autoPlayEnabled = if (accepted) {
            readNativeAutoPlayEnabled()
        } else {
            enabled
        }

        if (!autoPlayEnabled) {
            autoPlayStatus = "OFF"
            autoPlayController?.stopRuntime("AUTO_PLAY_TOGGLED_OFF")
        } else {
            autoPlayStatus = "ARMED"
        }

        publishStateNow()
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

    private companion object {
        const val FRAME_DELAY_MS = 33L
    }
}
