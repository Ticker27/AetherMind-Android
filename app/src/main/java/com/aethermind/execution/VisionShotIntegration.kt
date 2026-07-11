package com.aethermind.execution

import android.util.Log
import com.aethermind.ui.BallPosition
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * AT168 Core Truth Recovery.
 *
 * Legacy vision-to-shot bridge has been cut. Vision may produce observation data,
 * but it may not calculate or push gameplay commands. Keep this class as a
 * compatibility shell for old references only.
 */
class VisionShotIntegration(
    private val targetPackage: String,
    private val screenWidth: Int,
    private val screenHeight: Int
) : AutoCloseable {

    companion object {
        private const val TAG = "VisionShotIntegration"
    }

    data class ShotConfig(
        val aimExtension: Float = 0.5f,
        val swipeDurationMs: Long = 220L,
        val powerMultiplier: Float = 0.6f,
        val minBallDistance: Float = 0.1f,
        val maxBalls: Int = 8
    )

    data class ShotInfo(
        val cuePosition: BallPosition,
        val targetPosition: BallPosition,
        val distance: Float,
        val angle: Float,
        val timestamp: Long
    )

    private var config = ShotConfig()
    private var lastCueBall: BallPosition? = null
    private var lastTargetBall: BallPosition? = null
    private var lastShotTime: Long = 0L
    private var isRunning = false

    fun start() {
        isRunning = false
        AetherExecutionBridge.clearQueue()
        Log.i(TAG, "VisionShotIntegration locked: target=$targetPackage size=${screenWidth}x$screenHeight")
    }

    fun stop() {
        isRunning = false
        AetherExecutionBridge.clearQueue()
        Log.i(TAG, "VisionShotIntegration stopped/locked")
    }

    override fun close() = stop()

    fun getLastShotInfo(): ShotInfo? {
        val cue = lastCueBall ?: return null
        val target = lastTargetBall ?: return null
        return ShotInfo(
            cuePosition = cue,
            targetPosition = target,
            distance = distance(cue.normX, cue.normY, target.normX, target.normY),
            angle = angle(cue.normX, cue.normY, target.normX, target.normY),
            timestamp = lastShotTime
        )
    }

    fun updateConfig(newConfig: ShotConfig) {
        config = newConfig
        Log.i(TAG, "Config stored for diagnostics only: $config")
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun angle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        var degrees = Math.toDegrees(radians).toFloat()
        if (degrees < 0) degrees += 360f
        return degrees
    }
}
