package com.aethermind.execution

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewConfiguration
import com.aether.renderer.AetherIntegrationLoop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class CoordinateSpace {
    PIXELS,
    NORMALIZED_0_TO_1
}

data class GestureExecutionConfig(
    val coordinateSpace: CoordinateSpace = CoordinateSpace.PIXELS,

    // ABI เดิมมีแค่ x,y สำหรับ SWIPE จึงใช้ x,y เป็นจุดเริ่ม
    // แล้วลากตาม delta นี้
    val swipeDeltaXPx: Float = 0f,
    val swipeDeltaYPx: Float = -500f,
    val swipeDurationMs: Long = 220L,

    val tapDurationMs: Long = ViewConfiguration.getTapTimeout().toLong()
)

object GestureCommandPacking {
    /**
     * Packs a signed swipe delta into ActionCommand.reserved without changing
     * the 24-byte native ABI. Lower 16 bits = dx, upper 16 bits = dy.
     * Values are clamped to signed Int16 range for predictable JNI transfer.
     */
    fun packSwipeDelta(dxPx: Float, dyPx: Float): Int {
        val dx = dxPx.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        val dy = dyPx.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        return ((dy and 0xFFFF) shl 16) or (dx and 0xFFFF)
    }

    fun unpackSwipeDelta(packed: Int): Pair<Float, Float>? {
        if (packed == 0) return null
        val dx = (packed and 0xFFFF).toShort().toFloat()
        val dy = ((packed ushr 16) and 0xFFFF).toShort().toFloat()
        return dx to dy
    }
}

/**
 * Human-like gesture actuation.
 *
 * A real player never draws a perfect straight line at constant speed. This
 * executor turns the brain-owned [MotionProfile] into a CURVED, EASED,
 * micro-jittered swipe split into three phases:
 *   1) wind-up   — a small pull-back (like drawing the cue back),
 *   2) commit    — the main curved strike to the target,
 *   3) follow    — a short continue past the target (deceleration).
 * The curved quadratic path + phased timing remove the "robot line" tell.
 * Safety gates (PackageScopeGuard / EmergencyStop) live in the dispatcher and
 * are NOT touched here.
 */
class AccessibilityGestureExecutor(
    private val config: GestureExecutionConfig = GestureExecutionConfig()
) : ScreenActionExecutor {
    companion object {
        private const val TAG = "AetherGestureExecutor"
        private val motionRng = Random(0xC0FFEE)
    }

    override suspend fun execute(command: ActionCommand) {
        val service = AetherAccessibilityService.currentService()
            ?: throw IllegalStateException("AccessibilityService is not connected.")

        when (command.type) {
            ActionCommandType.TAP -> tap(service, command)
            ActionCommandType.SWIPE -> swipe(service, command)
        }
    }

    private suspend fun tap(service: AccessibilityService, command: ActionCommand) {
        val point = toScreenPoint(service, command.x, command.y)

        val path = Path().apply {
            moveTo(point.x, point.y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    config.tapDurationMs
                )
            )
            .build()

        dispatchOrThrow(service, gesture)

        Log.d(TAG, "TAP dispatched x=${point.x} y=${point.y}")
    }

    private suspend fun swipe(service: AccessibilityService, command: ActionCommand) {
        val start = toScreenPoint(service, command.x, command.y)

        val commandDelta = GestureCommandPacking.unpackSwipeDelta(command.reserved)
        val deltaX = commandDelta?.first ?: config.swipeDeltaXPx
        val deltaY = commandDelta?.second ?: config.swipeDeltaYPx
        val endRaw = clampToDisplay(
            service = service,
            x = start.x + deltaX,
            y = start.y + deltaY
        )

        // Brain-owned human-like kinematics (C++ decides the style).
        val len = hypot(deltaX.toDouble(), deltaY.toDouble())
        val difficulty = (len / 900.0).coerceIn(0.0, 1.0)
        val profile = parseMotionProfile(
            runCatching {
                AetherIntegrationLoop.nativeHumanMotionProfile(difficulty.toFloat())
            }.getOrNull()
        ) ?: MotionProfile()

        // Micro positional jitter on anchors: no two shots are pixel-identical.
        val jx = (motionRng.nextFloat() * 2f - 1f) * profile.jitterPx.toFloat()
        val jy = (motionRng.nextFloat() * 2f - 1f) * profile.jitterPx.toFloat()
        val s = PointF(start.x + jx, start.y + jy)
        val e = PointF(endRaw.x + jx * 0.4f, endRaw.y + jy * 0.4f)

        val gesture = buildHumanStroke(s, e, profile)
        dispatchOrThrow(service, gesture)

        Log.d(
            TAG,
            "SWIPE(human) start=(${s.x},${s.y}) end=(${e.x},${e.y}) " +
                "dur=${profile.durationMs} curve=${profile.curvature}"
        )
    }

    private fun buildHumanStroke(
        start: PointF,
        end: PointF,
        p: MotionProfile
    ): GestureDescription {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)

        // Perpendicular unit vector for the curved bow.
        val px = -dy / len
        val py = dx / len
        val bow = (p.curvature * len) * (if (motionRng.nextBoolean()) 1f else -1f)
        // Quadratic control point: midpoint bowed perpendicular => curved path.
        val ctrl = PointF(
            (start.x + end.x) / 2f + px * bow,
            (start.y + end.y) / 2f + py * bow
        )

        val wuLen = (len * p.windupScale.toFloat()).coerceAtLeast(6f)
        val windup = PointF(start.x - dx / len * wuLen, start.y - dy / len * wuLen)
        val ftLen = (len * p.followScale.toFloat()).coerceAtLeast(4f)
        val follow = PointF(end.x + dx / len * ftLen, end.y + dy / len * ftLen)

        val d1 = (p.durationMs * 0.35).toLong().coerceAtLeast(60L)   // wind-up
        val d2 = p.durationMs.toLong().coerceAtLeast(140L)           // commit
        val d3 = (p.durationMs * 0.25).toLong().coerceAtLeast(40L)   // follow-through

        val builder = GestureDescription.Builder()
        builder.addStroke(quadStroke(windup, start, ctrl, 0L, d1))
        builder.addStroke(quadStroke(start, end, ctrl, d1, d2))
        builder.addStroke(quadStroke(end, follow, ctrl, d1 + d2, d3))
        return builder.build()
    }

    private fun quadStroke(
        a: PointF,
        b: PointF,
        ctrl: PointF,
        startTimeMs: Long,
        durationMs: Long
    ): GestureDescription.StrokeDescription {
        val path = Path().apply {
            moveTo(a.x, a.y)
            quadTo(ctrl.x, ctrl.y, b.x, b.y)
        }
        return GestureDescription.StrokeDescription(path, startTimeMs, durationMs)
    }

    private data class MotionProfile(
        val durationMs: Double = 380.0,
        val curvature: Double = 0.16,
        val jitterPx: Double = 3.0,
        val windupScale: Double = 0.18,
        val followScale: Double = 0.10
    )

    private fun parseMotionProfile(json: String?): MotionProfile? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            fun num(key: String): Double {
                val i = json.indexOf("\"$key\"")
                if (i < 0) return 0.0
                val c = json.indexOf(':', i) + 1
                val e = json.indexOfAny(charArrayOf(',', '}'), c)
                return json.substring(c, e).toDouble()
            }
            MotionProfile(
                num("durationMs"), num("curvature"),
                num("jitterPx"), num("windupScale"), num("followScale")
            )
        }.getOrNull()
    }

    private fun toScreenPoint(
        service: AccessibilityService,
        x: Float,
        y: Float
    ): PointF {
        val metrics = service.resources.displayMetrics

        val rawX = when (config.coordinateSpace) {
            CoordinateSpace.PIXELS -> x
            CoordinateSpace.NORMALIZED_0_TO_1 -> x * metrics.widthPixels
        }

        val rawY = when (config.coordinateSpace) {
            CoordinateSpace.PIXELS -> y
            CoordinateSpace.NORMALIZED_0_TO_1 -> y * metrics.heightPixels
        }

        return clampToDisplay(service, rawX, rawY)
    }

    private fun clampToDisplay(
        service: AccessibilityService,
        x: Float,
        y: Float
    ): PointF {
        val metrics = service.resources.displayMetrics

        val maxX = max(0f, metrics.widthPixels - 1f)
        val maxY = max(0f, metrics.heightPixels - 1f)

        return PointF(
            min(max(x, 0f), maxX),
            min(max(y, 0f), maxY)
        )
    }

    private suspend fun dispatchOrThrow(
        service: AccessibilityService,
        gesture: GestureDescription
    ) {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            "dispatchGesture requires API 24+."
        }

        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val callback = object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("Gesture was cancelled by system or another gesture.")
                            )
                        }
                    }
                }

                val accepted = service.dispatchGesture(
                    gesture,
                    callback,
                    Handler(Looper.getMainLooper())
                )

                if (!accepted && continuation.isActive) {
                    continuation.resumeWithException(
                        IllegalStateException("dispatchGesture returned false.")
                    )
                }
            }
        }
    }
}
