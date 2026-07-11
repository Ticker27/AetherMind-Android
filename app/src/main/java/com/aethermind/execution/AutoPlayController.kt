package com.aethermind.execution

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.aether.renderer.AetherIntegrationLoop
import com.aethermind.ui.overlay.OverlayUiState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
    private var nextDelayMs = 1200L
    private val shotRng = Random(0xBADC0DE)

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
        // Variable, human-like cadence (brain-owned). No fixed robot clock.
        val intervalMs = nextDelayMs.coerceAtLeast(500)
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

        // Difficulty drives the brain's think-time + stroke style.
        val difficulty = (length / 700f).coerceIn(0f, 1f)

        // --- Human-like variance on the ACTUAL dispatched shot (not just the
        // overlay viz). A person never repeats the exact same power/angle. ---
        val powerPx = (nativePowerPx() * (0.92f + shotRng.nextFloat() * 0.16f))
            .coerceIn(160f, 900f)

        // small aim-angle wobble (rad), rotated into the swipe vector
        val aimJitter = (shotRng.nextFloat() * 2f - 1f) * 0.02f
        val ca = cos(aimJitter)
        val sa = sin(aimJitter)
        val rx = vx * ca - vy * sa
        val ry = vx * sa + vy * ca

        val dx = -(rx / length) * powerPx
        val dy = -(ry / length) * powerPx

        // tiny anchor jitter so the touch origin is never pixel-identical
        val ax = cue.center.x + (shotRng.nextFloat() * 2f - 1f) * 2f
        val ay = cue.center.y + (shotRng.nextFloat() * 2f - 1f) * 2f

        // In common pool touch controls, the player pulls the cue ball backward
        // from the intended shot direction. The command stores cue-ball start x/y
        // and a packed dx/dy swipe delta in the reserved field.
        val command = ActionCommand(
            x = ax,
            y = ay,
            type = ActionCommandType.SWIPE,
            reserved = GestureCommandPacking.packSwipeDelta(dx, dy),
            timestampNanos = System.nanoTime()
        )

        val result = AetherExecutionBridge.pushCommand(command)
        if (result == NativeExecutionStatus.OK) {
            lastShotUptimeMs = now
            // Sample NEXT gap from the brain (harder shots => longer think).
            nextDelayMs = runCatching {
                AetherIntegrationLoop.nativeHumanCadenceMs(difficulty)
            }.getOrDefault(nativeIntervalMs()).coerceAtLeast(500)
            status("QUEUED ${targetPackage.substringAfterLast('.')}")
            Log.d(TAG, "Auto shot queued target=$targetPackage dx=$dx dy=$dy interval=$intervalMs power=$powerPx diff=$difficulty")
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
