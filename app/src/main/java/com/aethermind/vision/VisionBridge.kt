package com.aethermind.vision

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import java.nio.ByteBuffer
import java.nio.ByteOrder

// =============================================================================
// Aether Vision Bridge - Kotlin Interface
// 
// รวม Ball Detection และ Trajectory Calculation
// 
// Usage:
//   val bridge = VisionBridge()
//   val balls = bridge.detectBalls(bitmap)
//   val path = bridge.getTrajectoryPath(cueX, cueY, targetX, targetY)
//   val info = bridge.getTrajectoryInfo(cueX, cueY, targetX, targetY)
// =============================================================================

class VisionBridge {
    
    init {
        System.loadLibrary("aether_vision")
    }
    
    // ========================================================================
    // NATIVE METHODS - Ball Detection
    // ========================================================================
    
    external fun processFrameNative(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        width: Int,
        height: Int,
        stride: Int
    ): Int
    
    // ========================================================================
    // NATIVE METHODS - Trajectory Calculation (C++ Engine)
    // ========================================================================
    
    /**
     * คำนวณเส้นวิถีจาก C++ Engine
     * 
     * @return FloatArray: [x0, y0, x1, y1, x2, y2, ...] ที่ Normalized 0.0-1.0
     */
    external fun calculateTrajectoryNative(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float
    ): FloatArray
    
    /**
     * ดึงข้อมูล trajectory metadata
     * 
     * @return FloatArray: [angleDegrees, power, confidence, distance]
     */
    external fun getTrajectoryInfoNative(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float
    ): FloatArray
    
    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================
    
    /**
     * ดึง trajectory path เป็น List<Offset> สำหรับ Compose Canvas
     * 
     * @param cueX ตำแหน่ง X ลูกขาว (Normalized 0.0-1.0)
     * @param cueY ตำแหน่ง Y ลูกขาว (Normalized 0.0-1.0)
     * @param targetX ตำแหน่ง X ลูกเป้าหมาย (Normalized 0.0-1.0)
     * @param targetY ตำแหน่ง Y ลูกเป้าหมาย (Normalized 0.0-1.0)
     * @return List<Offset> รายการพิกัดที่วาดเส้นได้เลย
     */
    fun getTrajectoryPath(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float
    ): List<Offset> {
        val rawData = calculateTrajectoryNative(cueX, cueY, targetX, targetY)
        val path = mutableListOf<Offset>()
        
        // วนลูปอ่านทีละคู่ (x, y)
        for (i in rawData.indices step 2) {
            if (i + 1 < rawData.size) {
                path.add(Offset(rawData[i], rawData[i + 1]))
            }
        }
        
        return path
    }
    
    /**
     * ดึง trajectory info (metadata)
     * 
     * @return TrajectoryInfo พร้อม angle, power, confidence, distance
     */
    fun getTrajectoryInfo(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float
    ): TrajectoryInfo {
        val data = getTrajectoryInfoNative(cueX, cueY, targetX, targetY)
        
        return if (data.size >= 4) {
            TrajectoryInfo(
                angleDegrees = data[0],
                power = data[1],
                confidence = data[2],
                distance = data[3]
            )
        } else {
            TrajectoryInfo()
        }
    }
    
    /**
     * ตรวจจับลูกบอลจาก bitmap
     * 
     * @param bitmap ภาพที่จะตรวจจับ
     * @return List<BallResult> รายการลูกบอลที่พบ
     */
    fun detectBalls(bitmap: Bitmap): List<BallResult> {
        // TODO: Implement with VisionProcessor
        return emptyList()
    }
    
    // ========================================================================
    // DATA CLASSES
    // ========================================================================
    
    /**
     * Trajectory Information
     */
    data class TrajectoryInfo(
        val angleDegrees: Float = 0f,      // มุมที่ต้องยิง (0-360)
        val power: Float = 0.5f,           // พลังที่แนะนำ (0.0-1.0)
        val confidence: Float = 0f,         // ความมั่นใจ (0.0-1.0)
        val distance: Float = 0f            // ระยะห่าง (Normalized)
    ) {
        /**
         * ตรวจสอบว่าเป็น shot ที่ดีหรือไม่
         */
        fun isGoodShot(): Boolean {
            return confidence >= 0.6f && power in 0.3f..0.9f
        }
        
        /**
         * องศาที่อ่านง่าย
         */
        fun angleFormatted(): String {
            return String.format("%.1f°", angleDegrees)
        }
        
        /**
         * พลังในรูปแบบเปอร์เซ็นต์
         */
        fun powerPercent(): Int {
            return (power * 100).toInt()
        }
    }
    
    /**
     * Ball Detection Result
     */
    data class BallResult(
        val id: Int,
        val x: Float,              // Normalized 0.0-1.0
        val y: Float,              // Normalized 0.0-1.0
        val confidence: Float,       // 0.0-1.0
        val colorIndex: Int,        // 0=unknown, 1=red, 2=yellow, 3=green
        val radius: Int             // Approximate radius in pixels
    ) {
        val isCueBall: Boolean
            get() = colorIndex == 0
    }
    
    companion object {
        const val LIB_NAME = "aether_vision"
    }
}

// =============================================================================
// Extension Functions for Compose
// =============================================================================

/**
 * หาเส้นที่สั้นที่สุดจาก trajectory path
 * ใช้สำหรับวาดเฉพาะส่วนที่เราต้องการ
 */
fun List<Offset>.takeNearest(count: Int): List<Offset> {
    if (size <= count) return this
    return take(count)
}

/**
 * แปลง trajectory path ให้อยู่ในขอบเขตหน้าจอ
 */
fun List<Offset>.scaleToScreen(
    screenWidth: Int,
    screenHeight: Int
): List<Offset> {
    return map { offset ->
        Offset(
            x = offset.x * screenWidth,
            y = offset.y * screenHeight
        )
    }
}

/**
 * หาความยาวของ path
 */
fun List<Offset>.pathLength(): Float {
    if (size < 2) return 0f
    
    var length = 0f
    for (i in 0 until size - 1) {
        val dx = this[i + 1].x - this[i].x
        val dy = this[i + 1].y - this[i].y
        length += kotlin.math.sqrt(dx * dx + dy * dy)
    }
    return length
}