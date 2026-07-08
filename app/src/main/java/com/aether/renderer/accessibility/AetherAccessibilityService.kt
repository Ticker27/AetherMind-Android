package com.aether.renderer.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.aether.renderer.AetherIntegrationLoop
import com.aether.renderer.AetherOverlayService
import com.aether.renderer.AetherRuntimeBus
import com.aether.renderer.NativeTrajectoryBridge
import com.aether.renderer.PhysicsStateBuffer
import java.nio.ByteBuffer

class AetherAccessibilityService : AccessibilityService() {

    private lateinit var observerStateBuffer: ByteBuffer
    private lateinit var outputStateBuffer: ByteBuffer
    private val stateMapper by lazy { AccessibilityStateMapper(this) }
    private val devKey: String? = null
    private var overlayRequested = false

    private val handler = Handler(Looper.getMainLooper())
    private val runtimeLoop = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 100L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!AetherIntegrationLoop.initializeAetherNative()) return
        observerStateBuffer = NativeTrajectoryBridge.newStateBuffer()
        outputStateBuffer = NativeTrajectoryBridge.newStateBuffer()
        ensureOverlayStarted()
        handler.removeCallbacks(runtimeLoop)
        handler.post(runtimeLoop)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = runCatching { rootInActiveWindow }.getOrNull()
        stateMapper.onEvent(event, root)
        root?.recycle()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(runtimeLoop)
        stopService(Intent(this, AetherOverlayService::class.java))
        overlayRequested = false
        super.onDestroy()
    }

    private fun tick() {
        if (!::observerStateBuffer.isInitialized || !::outputStateBuffer.isInitialized) return
        ensureOverlayStarted()

        stateMapper.writeFrame(
            buffer = observerStateBuffer,
            hudVisible = AetherIntegrationLoop.nativeHudVisible(),
            aiActive = AetherIntegrationLoop.nativeAiActive()
        )

        val success = AetherIntegrationLoop.runFrameStrict(observerStateBuffer, outputStateBuffer, devKey)
        if (!success) return

        val telemetry = PhysicsStateBuffer.readTelemetry(outputStateBuffer).copy(telemetryOnly = true)
        AetherRuntimeBus.publishTelemetry(telemetry)
        AetherOverlayService.instance?.updateHud(telemetry)
    }

    private fun ensureOverlayStarted() {
        if (overlayRequested || !Settings.canDrawOverlays(this)) return
        overlayRequested = true
        startService(Intent(this, AetherOverlayService::class.java))
    }
}
