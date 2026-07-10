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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

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

class AccessibilityGestureExecutor(
    private val config: GestureExecutionConfig = GestureExecutionConfig()
) : ScreenActionExecutor {
    companion object {
        private const val TAG = "AetherGestureExecutor"
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

        val end = clampToDisplay(
            service = service,
            x = start.x + config.swipeDeltaXPx,
            y = start.y + config.swipeDeltaYPx
        )

        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(end.x, end.y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    config.swipeDurationMs
                )
            )
            .build()

        dispatchOrThrow(service, gesture)

        Log.d(
            TAG,
            "SWIPE dispatched start=(${start.x},${start.y}) end=(${end.x},${end.y}) duration=${config.swipeDurationMs}"
        )
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
