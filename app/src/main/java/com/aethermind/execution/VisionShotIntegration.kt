package com.aethermind.execution

import android.util.Log
import com.aethermind.ui.BallPosition
import com.aethermind.vision.VisionBridge
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Vision Shot Integration - เชื่อม Vision กับ Execution
 * 
 * Architecture: Native-Driven Autoplay
 * - Vision ตรวจจับลูกบอล
 * - Shot Calculator คำนวณการยิง
 * - Push ActionCommand ไปยัง Execution Queue
 */
class VisionShotIntegration(
    private val targetPackage: String,
    private val screenWidth: Int,
    private val screenHeight: Int
) : AutoCloseable {
    
    companion object {
        private const val TAG = "VisionShotIntegration"
    }
    
    // Configuration
    data class ShotConfig(
        val aimExtension: Float = 0.5f,
        val swipeDurationMs: Long = 220L,
        val powerMultiplier: Float = 0.6f,
        val minBallDistance: Float = 0.1f,
        val maxBalls: Int = 8
    )
    
    private var config = ShotConfig()
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var visionBridge: VisionBridge? = null
    private var isRunning = false
    
    private var lastCueBall: BallPosition? = null
    private var lastTargetBall: BallPosition? = null
    private var lastShotTime: Long = 0
    
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Already running")
            return
        }
        
        isRunning = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        visionBridge = VisionBridge()
        
        job = scope?.launch {
            runVisionLoop()
        }
        
        Log.i(TAG, "VisionShotIntegration started for $targetPackage")
    }
    
    fun stop() {
        isRunning = false
        job?.cancel()
        scope?.cancel()
        visionBridge = null
        scope = null
        job = null
        
        Log.i(TAG, "VisionShotIntegration stopped")
    }
    
    override fun close() = stop()
    
    private suspend fun runVisionLoop() {
        while (isRunning && scope?.isActive == true) {
            try {
                val balls = detectBalls()
                
                if (balls.size >= 2) {
                    val (cueBall, targetBall) = findCueAndTarget(balls)
                    
                    if (cueBall != null && targetBall != null) {
                        calculateAndPushShot(cueBall, targetBall)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in vision loop", e)
            }
            
            delay(16) // 60 FPS
        }
    }
    
    private suspend fun detectBalls(): List<BallPosition> {
        // Stub: Return mock balls for testing
        return listOf(
            BallPosition(id = 0, normX = 0.5f, normY = 0.7f, isCue = true),
            BallPosition(id = 9, normX = 0.5f, normY = 0.4f, isCue = false)
        )
    }
    
    private fun findCueAndTarget(balls: List<BallPosition>): Pair<BallPosition?, BallPosition?> {
        val cue = balls.find { it.isCue }
        val target = balls.filter { !it.isCue }
            .minByOrNull { ball -> 
                cue?.let { c -> 
                    distance(c.normX, c.normY, ball.normX, ball.normY) 
                } ?: Float.MAX_VALUE
            }
        
        return Pair(cue, target)
    }
    
    private suspend fun calculateAndPushShot(cue: BallPosition, target: BallPosition) {
        val dirX = target.normX - cue.normX
        val dirY = target.normY - cue.normY
        
        val dist = distance(cue.normX, cue.normY, target.normX, target.normY)
        
        if (dist < config.minBallDistance) {
            Log.d(TAG, "Balls too close, skipping")
            return
        }
        
        // Calculate swipe start (at cue ball)
        val startX = cue.normX - dirX * 0.05f
        val startY = cue.normY - dirY * 0.05f
        
        // Calculate swipe end (extended past target)
        val endX = target.normX + dirX * config.aimExtension
        val endY = target.normY + dirY * config.aimExtension
        
        // Convert to pixel coordinates
        val startXPx = startX * screenWidth
        val startYPx = startY * screenHeight
        val endXPx = endX * screenWidth
        val endYPx = endY * screenHeight
        
        // Create ActionCommand
        val command = ActionCommand(
            x = startXPx,
            y = startYPx,
            type = ActionCommandType.SWIPE,
            reserved = 0,
            timestampNanos = System.nanoTime()
        )
        
        // Create command buffer and write
        val commandBuffer = AetherExecutionBridge.newCommandBuffer()
        ActionCommand.writeTo(commandBuffer, command)
        
        // Push to Native Queue
        val status = AetherExecutionBridge.pushCommand(commandBuffer)
        
        if (status == NativeExecutionStatus.OK) {
            lastCueBall = cue
            lastTargetBall = target
            lastShotTime = System.currentTimeMillis()
            
            Log.d(TAG, "Shot queued: start=($startXPx,$startYPx) end=($endXPx,$endYPx)")
        } else {
            Log.w(TAG, "Failed to push command: ${NativeExecutionStatus.nameOf(status)}")
        }
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
    
    fun getLastShotInfo(): ShotInfo? {
        if (lastCueBall == null || lastTargetBall == null) return null
        
        return ShotInfo(
            cuePosition = lastCueBall!!,
            targetPosition = lastTargetBall!!,
            distance = distance(
                lastCueBall!!.normX, lastCueBall!!.normY,
                lastTargetBall!!.normX, lastTargetBall!!.normY
            ),
            angle = angle(
                lastCueBall!!.normX, lastCueBall!!.normY,
                lastTargetBall!!.normX, lastTargetBall!!.normY
            ),
            timestamp = lastShotTime
        )
    }
    
    fun updateConfig(newConfig: ShotConfig) {
        config = newConfig
        Log.i(TAG, "Config updated: $config")
    }
    
    data class ShotInfo(
        val cuePosition: BallPosition,
        val targetPosition: BallPosition,
        val distance: Float,
        val angle: Float,
        val timestamp: Long
    )
}