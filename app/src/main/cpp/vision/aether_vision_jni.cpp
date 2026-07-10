// =============================================================================
// Aether Vision Lite - JNI Bridge
//
// Zero-Copy DirectByteBuffer communication with Kotlin
// 
// Function: processFrameNative
// Signature: int processFrameNative(ByteBuffer input, ByteBuffer output, 
//                                   int width, int height, int stride)
// Returns: Number of balls detected, -1 on error
// =============================================================================

#include <jni.h>
#include <android/log.h>
#include "VisionProcessor.h"

#define LOG_TAG "AetherVision"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// =============================================================================
// JNI: processFrameNative
// 
// Entry point from Kotlin. Receives DirectByteBuffer for zero-copy access.
// 
// Parameters:
//   env - JNI environment
//   thiz - Java object (unused)
//   inputBuffer - DirectByteBuffer containing RGBA frame data
//   outputBuffer - DirectByteBuffer to receive BallOutput array
//   width - Frame width in pixels
//   height - Frame height in pixels
//   stride - Bytes per row (usually width * 4 for RGBA)
// 
// Returns:
//   Number of balls detected, or -1 on error
// =============================================================================
JNIEXPORT jint JNICALL
Java_com_aethermind_vision_AetherVisionNative_processFrameNative(
    JNIEnv* env,
    jobject thiz,
    jobject inputBuffer,
    jobject outputBuffer,
    jint width,
    jint height,
    jint stride
) {
    // Get direct buffer addresses (zero-copy)
    uint8_t* in = reinterpret_cast<uint8_t*>(
        env->GetDirectBufferAddress(inputBuffer)
    );
    uint8_t* out = reinterpret_cast<uint8_t*>(
        env->GetDirectBufferAddress(outputBuffer)
    );
    
    // Validate buffers
    if (!in) {
        LOGE("Input buffer address is null");
        return -1;
    }
    
    if (!out) {
        LOGE("Output buffer address is null");
        return -1;
    }
    
    // Validate dimensions
    if (width <= 0 || height <= 0 || stride <= 0) {
        LOGE("Invalid dimensions: width=%d, height=%d, stride=%d", 
             width, height, stride);
        return -1;
    }
    
    // Call vision processor
    int result = aether::vision::VisionProcessor::getInstance().processFrame(
        in, out, width, height, stride
    );
    
    LOGI("Frame processed: %d balls detected", result);
    
    return result;
}

} // extern "C"