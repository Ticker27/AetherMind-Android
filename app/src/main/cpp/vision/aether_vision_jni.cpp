// =============================================================================
// Aether Vision Lite - JNI Bridge
//
// Zero-Copy DirectByteBuffer communication with Kotlin
// 
// Functions:
//   - processFrameNative: Ball detection from frame data
//   - calculateTrajectoryNative: Calculate trajectory from C++ Engine
// =============================================================================

#include <jni.h>
#include <android/log.h>
#include <vector>
#include "VisionProcessor.h"
#include "TrajectoryEngine.h"

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

// =============================================================================
// JNI: calculateTrajectoryNative
// 
// Calculate trajectory path from cue ball to target ball using C++ Engine
// 
// Parameters:
//   env - JNI environment
//   thiz - Java object (unused)
//   cueX, cueY - Cue ball position (Normalized 0.0-1.0)
//   targetX, targetY - Target ball position (Normalized 0.0-1.0)
// 
// Returns:
//   jfloatArray: Flattened array of trajectory points [x0, y0, x1, y1, ...]
// =============================================================================
JNIEXPORT jfloatArray JNICALL
Java_com_aethermind_vision_VisionBridge_calculateTrajectoryNative(
    JNIEnv* env,
    jobject /* this */,
    jfloat cueX, jfloat cueY,
    jfloat targetX, jfloat targetY
) {
    using namespace aether::trajectory;
    
    // Create points from parameters
    Point cue(cueX, cueY);
    Point target(targetX, targetY);
    
    // Configure table (Normalized 1.0 x 1.0)
    TrajectoryConfig config;
    config.tableWidth = 1.0f;
    config.tableHeight = 1.0f;
    config.ballRadius = 0.02f;  // ~2% of table width
    config.maxPoints = 50;
    
    // Calculate trajectory using C++ Engine
    PredictionResult result = TrajectoryEngine::calculatePath(cue, target, config);
    
    // Flatten trajectory points to float array
    // Format: [x0, y0, x1, y1, x2, y2, ...]
    std::vector<float> flattened;
    flattened.reserve(result.trajectoryPath.size() * 2);
    
    for (const auto& point : result.trajectoryPath) {
        flattened.push_back(point.x);
        flattened.push_back(point.y);
    }
    
    // Create Java float array
    jfloatArray output = env->NewFloatArray(flattened.size());
    if (output == nullptr) {
        LOGE("Failed to create float array");
        return nullptr;
    }
    
    // Copy data to Java array
    env->SetFloatArrayRegion(output, 0, flattened.size(), flattened.data());
    
    LOGI("Trajectory calculated: %zu points, angle=%.1f, power=%.2f, confidence=%.2f",
         result.trajectoryPath.size(),
         result.angleDegrees,
         result.recommendedPower,
         result.confidence);
    
    return output;
}

// =============================================================================
// JNI: getTrajectoryInfoNative
// 
// Get trajectory metadata (angle, power, confidence)
// Returns: float[] = [angleDegrees, power, confidence, distance]
// =============================================================================
JNIEXPORT jfloatArray JNICALL
Java_com_aethermind_vision_VisionBridge_getTrajectoryInfoNative(
    JNIEnv* env,
    jobject /* this */,
    jfloat cueX, jfloat cueY,
    jfloat targetX, jfloat targetY
) {
    using namespace aether::trajectory;
    
    Point cue(cueX, cueY);
    Point target(targetX, targetY);
    
    TrajectoryConfig config;
    PredictionResult result = TrajectoryEngine::calculatePath(cue, target, config);
    
    // Create info array: [angle, power, confidence, distance]
    float info[4] = {
        result.angleDegrees,
        result.recommendedPower,
        result.confidence,
        result.distance
    };
    
    jfloatArray output = env->NewFloatArray(4);
    if (output == nullptr) {
        return nullptr;
    }
    
    env->SetFloatArrayRegion(output, 0, 4, info);
    
    return output;
}

} // extern "C"