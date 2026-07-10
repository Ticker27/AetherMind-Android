package com.aethermind.execution

import android.util.Log
import com.aethermind.ui.BallPosition
import com.aethermind.vision.AetherVisionNative
import com.aethermind.vision.VisionBridge
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Vision Shot Integration - เชื่อม Vision กับ Execution
 * 
 * Architecture: Native-Driven Autoplay
 * - Vision ตรวจจับลูกบอล
 * - Shot Calculator คำนวณการยิง
 * - Push ActionCommand ไปยัง Execution Queue
 * 
 * Flow:
 *   VisionProcessor (C++) 
 *       ↓ Ball positions
 *   VisionShotIntegration 
 *       ↓ Shot calculation  
 *   ExecutionBridge
 *       ↓ ActionCommand
 *   AetherActionDispatcher 
 *       ↓ execute()
 *   AccessibilityGestureExecutor → dispatchGesture()
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
        val aimExtension: Float = 0.5f,      // ขยายเส้นเล็งออกไป 50%
        val swipeDurationMs: Long = 220L,      // ระยะเวลาลาก (ms)
        val powerMultiplier: Float = 0.6f,     // คูณกับ delta พื้นฐาน
        val minBallDistance: Float = 0.1f,     // ลูกต้องห่างกันอย่างน้อย 10%
        val maxBalls: Int = 8                  // สแกนได้สูงสุดกี่ลูก
    )
    
    private var config = ShotConfig()
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var visionBridge: VisionBridge? = null
    private var isRunning = false
    
    // Shot state
    private var lastCueBall: BallPosition? = null
    private var lastTargetBall: BallPosition? = null
    private var lastShotTime: Long = 0
    
    /**
     * เริ่มการทำงาน
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Already running")
            return
        }
        
        isRunning = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        // Initialize vision bridge
        visionBridge = VisionBridge()
        visionBridge?.initialize(screenWidth, screenHeight)
        
        job = scope?.launch {
            runVisionLoop()
        }
        
        Log.i(TAG, "VisionShotIntegration started for $targetPackage")
    }
    
    /**
     * หยุดการทำงาน
     */
    fun stop() {
        isRunning = false
        job?.cancel()
        scope?.cancel()
        visionBridge?.shutdown()
        scope = null
        job = null
        visionBridge = null
        
        Log.i(TAG, "VisionShotIntegration stopped")
    }
    
    override fun close() = stop()
    
    /**
     * Vision Loop - วนลูปตรวจจับลูกบอลและส่งคำสั่ง
     */
    private suspend fun runVisionLoop() {
        while (isRunning && scope?.isActive == true) {
            try {
                // 1. ตรวจจับลูกบอลจาก Vision
                val balls = detectBalls()
                
                if (balls.size >= 2) {
                    // 2. หาลูกขาวและลูกเป้าหมายที่ใกล้สุด
                    val (cueBall, targetBall) = findCueAndTarget(balls)
                    
                    if (cueBall != null && targetBall != null) {
                        // 3. คำนวณ Shot และ Push ลง Queue
                        calculateAndPushShot(cueBall, targetBall)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in vision loop", e)
            }
            
            // Delay ระหว่างเฟรม (60 FPS = 16ms)
            delay(16)
        }
    }
    
    /**
     * ตรวจจับลูกบอลจาก Vision
     * สำหรับ Stub Mode จะใช้ mock data แทน
     */
    private suspend fun detectBalls(): List<BallPosition> {
        // TODO: แทนที่ด้วยการเรียก VisionProcessor จริง
        // val bitmap = captureScreen()
        // return visionBridge?.detectBalls(bitmap) ?: emptyList()
        
        // Stub: Return mock balls
        return listOf(
            BallPosition(id = 0, normX = 0.5f, normY = 0.7f, isCue = true),
            BallPosition(id = 9, normX = 0.5f, normY = 0.4f, isCue = false)
        )
    }
    
    /**
     * หาลูกขาว (cue) และลูกเป้าหมาย (target)
     * ใน Stub Mode จะเลือกลูกแรกสุดที่ไม่ใช่ cue
     */
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
    
    /**
     * คำนวณ Shot และ Push ลง Native Queue
     */
    private suspend fun calculateAndPushShot(cue: BallPosition, target: BallPosition) {
        // คำนวณเวกเตอร์จากลูกขาวไปยังลูกเป้าหมาย
        val dirX = target.normX - cue.normX
        val dirY = target.normY - cue.normY
        
        // คำนวณระยะ
        val dist = distance(cue.normX, cue.normY, target.normX, target.normY)
        
        // Skip ถ้าลูกอยู่ใกล้เกินไป
        if (dist < config.minBallDistance) {
            Log.d(TAG, "Balls too close, skipping")
            return
        }
        
        // คำนวณจุดเริ่มต้นของ Swipe
        // อยู่ที่ลูกขาว แต่เลื่อนออกไปนิดหน่อยในทิศตรงข้ามกับเป้า
        val startX = cue.normX - dirX * 0.05f
        val startY = cue.normY - dirY * 0.05f
        
        // คำนวณจุดสิ้นสุดของ Swipe (ขยายไปจากเป้าหมาย)
        val endX = target.normX + dirX * config.aimExtension
        val endY = target.normY + dirY * config.aimExtension
        
        // แปลงเป็นพิกัด Pixel
        val startXPx = startX * screenWidth
        val startYPx = startY * screenHeight
        val endXPx = endX * screenWidth
        val endYPx = endY * screenHeight
        
        // สร้าง ActionCommand
        val commandBuffer = AetherExecutionBridge.newCommandBuffer()
        
        // Write command to buffer (format: x, y, type, reserved, timestamp)
        commandBuffer.clear()
        commandBuffer.putFloat(startXPx)        // x (pixel)
        commandBuffer.putFloat(startYPx)        // y (pixel)  
        commandBuffer.putInt(ActionCommandType.SWIPE.ordinal)  // type
        commandBuffer.putInt(0)                   // reserved
        commandBuffer.putLong(System.nanoTime())  // timestamp
        
        // Push to Native Queue
        val status = AetherExecutionBridge.pushCommand(commandBuffer)
        
        if (status == NativeExecutionStatus.OK) {
            lastCueBall = cue
            lastTargetBall = target
            lastShotTime = System.currentTimeMillis()
            
            Log.d(TAG, "Shot queued: start=($startXPx,$startYPx) end=($endXPx,$endYPx)")
        } else {
            Log.w(TAG, "Failed to push command: $status")
        }
    }
    
    /**
     * คำนวณระยะห่างระหว่าง 2 จุด
     */
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * คำนวณมุมระหว่าง 2 จุด (องศา)
     */
    private fun angle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        var degrees = Math.toDegrees(radians).toFloat()
        if (degrees < 0) degrees += 360f
        return degrees
    }
    
    /**
     * Get last shot info for UI
     */
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
    
    /**
     * ปรับแต่ง Shot Configuration
     */
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

/**
 * Native Execution Status (matches C++ ExecutionStatus)
 */
enum class NativeExecutionStatus {
    OK,
    QUEUE_EMPTY,
    
    ERROR_NULL_BUFFER,
    ERROR_NON_DIRECT_BUFFER,
    ERROR_INSUFFICIENT_CAPACITY,
    ERROR_INVALID_COMMAND_TYPE,
    ERROR_INVALID_COORDINATE,
    ERROR_ALLOCATION_FAILED,
    ERROR_INTERNAL
}

/**
 * Action Command Type (matches C++ ActionCommandType)
 */
enum class ActionCommandType {
    TAP,
    SWIPE
}