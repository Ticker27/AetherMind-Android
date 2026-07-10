package com.aethermind.vision

// =============================================================================
// Aether Vision Native - JNI Bridge
//
// Kotlin interface for native vision processing
// Matches: Java_com_aethermind_vision_AetherVisionNative_processFrameNative
// =============================================================================

/**
 * AetherVisionNative - Native JNI Bridge
 * 
 * Loads libaether_vision.so and exposes native functions.
 * All data transfer uses DirectByteBuffer for zero-copy performance.
 */
object AetherVisionNative {
    
    private const val LIB_NAME = "aether_vision"
    
    init {
        System.loadLibrary(LIB_NAME)
    }
    
    /**
     * Process a single frame for ball detection
     * 
     * @param inputBuffer DirectByteBuffer with RGBA frame data
     * @param outputBuffer DirectByteBuffer for BallOutput array
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @param stride Bytes per row
     * @return Number of balls detected, -1 on error
     */
    external fun processFrameNative(
        inputBuffer: java.nio.ByteBuffer,
        outputBuffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        stride: Int
    ): Int
    
    /**
     * Get library loading status
     */
    val isLoaded: Boolean
        get() = try {
            System.loadLibrary(LIB_NAME)
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
}