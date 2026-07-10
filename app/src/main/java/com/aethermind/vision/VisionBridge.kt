package com.aethermind.vision

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

// =============================================================================
// Aether Vision Bridge - High-Level Kotlin API
// =============================================================================

/**
 * Vision Bridge - Simple API for ball detection
 * 
 * Usage:
 * ```kotlin
 * val bridge = VisionBridge()
 * bridge.initialize(640, 480)
 * 
 * val balls = bridge.detectBalls(bitmap)
 * balls.forEach { println("Ball at (${it.x}, ${it.y})") }
 * 
 * bridge.shutdown()
 * ```
 */
class VisionBridge {
    
    private var inputBuffer: ByteBuffer? = null
    private var outputBuffer: ByteBuffer? = null
    
    // Ball output struct size (matches C++ BallOutput)
    companion object {
        const val BALL_OUTPUT_SIZE = 16  // 4 + 4 + 2 + 1 + 1 + 2 padding
        const val MAX_BALLS = 32
        const val OUTPUT_BUFFER_SIZE = BALL_OUTPUT_SIZE * MAX_BALLS
    }
    
    /**
     * Initialize buffers
     */
    fun initialize(width: Int, height: Int) {
        val pixelCount = width * height
        val inputSize = pixelCount * 4  // RGBA
        
        inputBuffer = ByteBuffer.allocateDirect(inputSize).apply {
            order(ByteOrder.nativeOrder())
        }
        
        outputBuffer = ByteBuffer.allocateDirect(OUTPUT_BUFFER_SIZE).apply {
            order(ByteOrder.nativeOrder())
        }
    }
    
    /**
     * Detect balls in bitmap
     * 
     * @param bitmap Input bitmap (RGBA format)
     * @return List of detected balls
     */
    fun detectBalls(bitmap: Bitmap): List<BallResult> {
        val input = inputBuffer ?: run {
            initialize(bitmap.width, bitmap.height)
            inputBuffer!!
        }
        val output = outputBuffer!!
        
        // Get pixels from bitmap
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Write to input buffer (RGBA format)
        input.clear()
        for (pixel in pixels) {
            // ARGB to RGBA
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val a = (pixel shr 24) and 0xFF
            input.put(r.toByte())
            input.put(g.toByte())
            input.put(b.toByte())
            input.put(a.toByte())
        }
        input.flip()
        
        // Clear output buffer
        output.clear()
        for (i in 0 until OUTPUT_BUFFER_SIZE) {
            output.put(i, 0.toByte())
        }
        
        // Call native
        val ballCount = AetherVisionNative.processFrameNative(
            input,
            output,
            bitmap.width,
            bitmap.height,
            bitmap.width * 4
        )
        
        if (ballCount <= 0) {
            return emptyList()
        }
        
        // Parse results
        val results = mutableListOf<BallResult>()
        for (i in 0 until ballCount) {
            val offset = i * BALL_OUTPUT_SIZE
            
            // Read BallOutput struct
            // x, y are in Q0.16 fixed-point (need to convert to float)
            val xRaw = output.getShort(offset).toInt() and 0xFFFF
            val yRaw = output.getShort(offset + 2).toInt() and 0xFFFF
            
            val x = xRaw / 65536.0f
            val y = yRaw / 65536.0f
            
            val radius = output.getShort(offset + 4).toInt() and 0xFFFF
            val confidence = output.get(offset + 6).toInt() and 0xFF
            val colorIndex = output.get(offset + 7).toInt() and 0xFF
            
            results.add(BallResult(
                x = x,
                y = y,
                radius = radius,
                confidence = confidence / 255.0f,
                colorIndex = colorIndex
            ))
        }
        
        return results
    }
    
    /**
     * Release resources
     */
    fun shutdown() {
        inputBuffer = null
        outputBuffer = null
    }
}

/**
 * Ball detection result
 */
data class BallResult(
    val x: Float,
    val y: Float,
    val radius: Int,
    val confidence: Float,
    val colorIndex: Int
) {
    val isHighConfidence: Boolean
        get() = confidence >= 0.7f
    
    companion object {
        const val COLOR_RED = 1
        const val COLOR_YELLOW = 2
        const val COLOR_GREEN = 3
    }
}