package com.aethermind.execution

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.aether.renderer.AetherIntegrationLoop
import com.aethermind.ui.overlay.OverlayUiState
import kotlin.math.sqrt

/**
 * Skill-adaptive auto-play coordinator.
 *
 * C++ owns the policy switches and skill-derived power/cadence. Kotlin owns the
 * Android boundary checks because package visibility, Accessibility connection,
 * and gesture dispatch are Android-side responsibilities.
 *
 * Safety contract:
 * - Disabled by default.
 * - Requires explicit UI toggle.
 * - Requires a connected AccessibilityService.
 * - Uses latest foreground package as the target and still relies on
 *   PackageScopeGuard inside AetherActionDispatcher before every gesture.
 * - Stops and clears the native queue immediately when disabled or invalid.
 */
class AutoPlayController(
    private val context: Context,
    private val statusSink: (String) -> Unit
) : AutoCloseable {

    companion object {
        private const val TAG = "AetherAutoPlay"
        private const val MIN_VECTOR_PX = 8f
    }

    private var lastShotUptimeMs = 0L
    private var lastTargetPackage: String? = null

    fun onFrame(state: OverlayUiState) {
        if (!state.autoPlayEnabled) {
            status("OFF")
            return
        }

        if (!AetherIntegrationLoop.nativeAutoPlayEnabled()) {
            stopRuntime("NATIVE_DISABLED")
            status("NATIVE OFF")
            return
        }

        val service = AetherAccessibilityService.currentService()
        if (service == null) {
            stopRuntime("NO_ACCESSIBILITY")
            status("NO ACCESSIBILITY")
            return
        }

        val targetPackage = AetherAccessibilityService.latestEventPackage()
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != context.packageName }

        if (targetPackage == null) {
            stopRuntime("NO_FOREGROUND_TARGET")
            status("NO TARGET")
            return
        }

        if (targetPackage != lastTargetPackage) {
            AetherRuntime.startForTargetPackage(targetPackage)
            lastTargetPackage = targetPackage
        } else if (!AetherRuntime.isRunning()) {
            AetherRuntime.startForTargetPackage(targetPackage)
        }

        val now = SystemClock.uptimeMillis()
        val intervalMs = nativeIntervalMs().coerceAtLeast(350)
        if (now - lastShotUptimeMs < intervalMs) {
            status("ARMED ${intervalMs}ms")
            return
        }

        val cue = state.cueBall
        val target = state.targetBall
        if (cue == null || target == null) {
            status("NO SHOT")
            return
        }

        val vx = target.center.x - cue.center.x
        val vy = target.center.y - cue.center.y
        val length = sqrt(vx * vx + vy * vy)
        if (length < MIN_VECTOR_PX) {
            status("VECTOR LOW")
            return
        }

        // In common pool touch controls, the player pulls the cue ball backward
        // from the intended shot direction. The command stores cue-ball start x/y
        // and a packed dx/dy swipe delta in the reserved field.
        val powerPx = nativePowerPx().coerceIn(160f, 900f)
        val dx = -(vx / length) * powerPx
        val dy = -(vy / length) * powerPx

        val command = ActionCommand(
            x = cue.center.x,
            y = cue.center.y,
            type = ActionCommandType.SWIPE,
            reserved = GestureCommandPacking.packSwipeDelta(dx, dy),
            timestampNanos = System.nanoTime()
        )

        val result = AetherExecutionBridge.pushCommand(command)
        if (result == NativeExecutionStatus.OK) {
            lastShotUptimeMs = now
            status("QUEUED ${targetPackage.substringAfterLast('.')}")
            Log.d(TAG, "Auto shot queued target=$targetPackage dx=$dx dy=$dy interval=$intervalMs power=$powerPx")
        } else {
            status("QUEUE ${NativeExecutionStatus.nameOf(result)}")
            Log.w(TAG, "Auto shot rejected: ${NativeExecutionStatus.nameOf(result)}")
        }
    }

    fun stopRuntime(reason: String = "AUTO_PLAY_DISABLED") {
        if (AetherRuntime.isRunning()) {
            AetherRuntime.emergencyStop(reason)
        } else {
            AetherExecutionBridge.clearQueue()
        }
        lastTargetPackage = null
    }

    override fun close() {
        stopRuntime("AUTO_PLAY_CONTROLLER_CLOSED")
    }

    private fun nativeIntervalMs(): Int {
        return runCatching { AetherIntegrationLoop.nativeAutoPlayIntervalMs() }
            .getOrDefault(1200)
    }

    private fun nativePowerPx(): Float {
        return runCatching { AetherIntegrationLoop.nativeAutoPlaySwipePowerPx() }
            .getOrDefault(420f)
    }

    private fun status(value: String) {
        statusSink(value)
    }
}
