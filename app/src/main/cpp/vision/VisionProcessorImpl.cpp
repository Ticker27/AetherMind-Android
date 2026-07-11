#include "VisionProcessor.h"
#include <cstdint>
#include <algorithm>

// =============================================================================
// Aether Vision Lite - VisionProcessorImpl.cpp
// Final Implementation with Fixed-point Q0.16 and Branchless Logic
//
// OPTIMIZATION NOTES:
// - Zero-allocation: No malloc/new in processFrame
// - Row-pointer access with stride
// - Fixed-point Q0.16 for coordinates
// - Branchless thresholding (avoid if-statements in inner loop)
// - Loop unrolling: 4 pixels per iteration
// =============================================================================

namespace aether::vision {

// =============================================================================
// BRANCHLESS THRESHOLD MACROS
// =============================================================================

// Branchless comparison: (x >= min && x <= max)
// Returns 1 if true, 0 if false
// Uses bit manipulation to avoid branches
#define BRANCHLESS_IN_RANGE(x, min, max) \
    ((~(((min) - (x) - 1) | ((x) - (max) - 1)) >> 31) & 1)

// Branchless comparison: (x > threshold)
#define BRANCHLESS_GT(x, threshold) \
    (((x) - (threshold) - 1) >> 31 ^ 1)

// Branchless comparison: (x < threshold)  
#define BRANCHLESS_LT(x, threshold) \
    ((threshold) - (x) - 1) >> 31 & 1

// =============================================================================
// BALL OUTPUT STRUCT (Compact 16 bytes)
// =============================================================================

struct BallOutput {
    int32_t xQ16;        // X coordinate, Q16 fixed-point pixel units
    int32_t yQ16;        // Y coordinate, Q16 fixed-point pixel units
    uint16_t radius;     // Approximate radius in pixels
    uint8_t confidence;  // Confidence score (0-255)
    uint8_t colorIndex;  // Color classification (1=Red, 2=Yellow, 3=Green)
    uint8_t padding[4];  // Alignment padding

    BallOutput() : xQ16(0), yQ16(0), radius(0), confidence(0), colorIndex(0), padding{0,0,0,0} {}
};

// =============================================================================
// VISION PROCESSOR IMPLEMENTATION
// =============================================================================

VisionProcessor& VisionProcessor::getInstance() {
    static VisionProcessor instance;
    return instance;
}

int VisionProcessor::processFrame(
    uint8_t* frameData,
    uint8_t* outputBuffer,
    int width,
    int height,
    int stride
) {
    // Output buffer is array of BallOutput structs
    BallOutput* balls = reinterpret_cast<BallOutput*>(outputBuffer);
    int ballCount = 0;
    
    // Maximum balls we can write (outputBuffer size / BallOutput size)
    constexpr int MAX_BALLS = 32;
    
    // =================================================================
    // SCANNING LOOP
    // Row-major access pattern for cache efficiency
    // =================================================================
    
    for (int y = 0; y < height; ++y) {
        // Row pointer with stride (accounts for row padding)
        uint8_t* rowPtr = frameData + (y * stride);
        
        // Process 4 pixels at a time (loop unrolling)
        int x = 0;
        const int unrollLimit = (width / 4) * 4;
        
        // ===== UNROLLED INNER LOOP =====
        // Process 4 pixels per iteration for better ILP
        for (; x < unrollLimit; x += 4) {
            // =====================================
            // Pixel 0 - Red/Yellow/Green detection
            // =====================================
            {
                uint8_t* pixel = rowPtr + (x * 4);
                
                // Extract RGB components
                uint8_t r = pixel[0];
                uint8_t g = pixel[1];
                uint8_t b = pixel[2];
                
                // Fast brightness calculation
                // Y = 0.299*R + 0.587*G + 0.114*B
                // Optimized: Y = (77*R + 150*G + 29*B) >> 8
                uint16_t brightness = (77 * r + 150 * g + 29 * b) >> 8;
                
                // Branchless threshold: bright pixel is candidate
                uint8_t isCandidate = BRANCHLESS_GT(brightness, 150);
                
                // Saturation check
                uint8_t minRGB = (r < g) ? ((r < b) ? r : b) : ((g < b) ? g : b);
                uint16_t sumRGB = r + g + b;
                uint8_t saturation = (sumRGB > 0) ? 
                    ((sumRGB - 3 * minRGB) * 255 / sumRGB) : 0;
                
                // Color detection (branchless)
                // Red: high R, lower G and B
                uint8_t isRed = BRANCHLESS_IN_RANGE(r, 150, 255) & 
                                BRANCHLESS_LT(g, 100) &
                                BRANCHLESS_LT(b, 100);
                
                // Yellow: high R and G, low B
                uint8_t isYellow = BRANCHLESS_IN_RANGE(r, 150, 255) &
                                   BRANCHLESS_IN_RANGE(g, 150, 255) &
                                   BRANCHLESS_LT(b, 100);
                
                // Green: high G, lower R and B
                uint8_t isGreen = BRANCHLESS_IN_RANGE(g, 100, 255) &
                                  BRANCHLESS_LT(r, 150) &
                                  BRANCHLESS_LT(b, 150);
                
                // Combine all conditions (branchless OR)
                uint8_t colorMatch = isRed | (isYellow << 1) | (isGreen << 2);
                
                // Final candidate check
                uint8_t isBallPixel = isCandidate & (saturation > 30) & (colorMatch > 0);
                
                if (isBallPixel && ballCount < MAX_BALLS) {
                    BallOutput& ball = balls[ballCount];
                    
                    // Convert to fixed-point Q0.16 (multiply by 65536)
                    ball.xQ16 = static_cast<int32_t>(x) << 16;
                    ball.yQ16 = static_cast<int32_t>(y) << 16;
                    ball.radius = 20;  // Approximate
                    ball.confidence = std::min<uint8_t>(255, saturation + 50);
                    ball.colorIndex = colorMatch;
                    
                    ballCount++;
                }
            }
            
            // =====================================
            // Pixel 1
            // =====================================
            {
                uint8_t* pixel = rowPtr + ((x + 1) * 4);
                
                uint8_t r = pixel[0];
                uint8_t g = pixel[1];
                uint8_t b = pixel[2];
                
                uint16_t brightness = (77 * r + 150 * g + 29 * b) >> 8;
                uint8_t isCandidate = BRANCHLESS_GT(brightness, 150);
                
                uint8_t minRGB = (r < g) ? ((r < b) ? r : b) : ((g < b) ? g : b);
                uint16_t sumRGB = r + g + b;
                uint8_t saturation = (sumRGB > 0) ? 
                    ((sumRGB - 3 * minRGB) * 255 / sumRGB) : 0;
                
                uint8_t isRed = BRANCHLESS_IN_RANGE(r, 150, 255) & 
                                BRANCHLESS_LT(g, 100) &
                                BRANCHLESS_LT(b, 100);
                uint8_t isYellow = BRANCHLESS_IN_RANGE(r, 150, 255) &
                                   BRANCHLESS_IN_RANGE(g, 150, 255) &
                                   BRANCHLESS_LT(b, 100);
                uint8_t isGreen = BRANCHLESS_IN_RANGE(g, 100, 255) &
                                  BRANCHLESS_LT(r, 150) &
                                  BRANCHLESS_LT(b, 150);
                
                uint8_t colorMatch = isRed | (isYellow << 1) | (isGreen << 2);
                uint8_t isBallPixel = isCandidate & (saturation > 30) & (colorMatch > 0);
                
                if (isBallPixel && ballCount < MAX_BALLS) {
                    BallOutput& ball = balls[ballCount];
                    ball.xQ16 = static_cast<int32_t>(x + 1) << 16;
                    ball.yQ16 = static_cast<int32_t>(y) << 16;
                    ball.radius = 20;
                    ball.confidence = std::min<uint8_t>(255, saturation + 50);
                    ball.colorIndex = colorMatch;
                    ballCount++;
                }
            }
            
            // =====================================
            // Pixel 2
            // =====================================
            {
                uint8_t* pixel = rowPtr + ((x + 2) * 4);
                
                uint8_t r = pixel[0];
                uint8_t g = pixel[1];
                uint8_t b = pixel[2];
                
                uint16_t brightness = (77 * r + 150 * g + 29 * b) >> 8;
                uint8_t isCandidate = BRANCHLESS_GT(brightness, 150);
                
                uint8_t minRGB = (r < g) ? ((r < b) ? r : b) : ((g < b) ? g : b);
                uint16_t sumRGB = r + g + b;
                uint8_t saturation = (sumRGB > 0) ? 
                    ((sumRGB - 3 * minRGB) * 255 / sumRGB) : 0;
                
                uint8_t isRed = BRANCHLESS_IN_RANGE(r, 150, 255) & 
                                BRANCHLESS_LT(g, 100) &
                                BRANCHLESS_LT(b, 100);
                uint8_t isYellow = BRANCHLESS_IN_RANGE(r, 150, 255) &
                                   BRANCHLESS_IN_RANGE(g, 150, 255) &
                                   BRANCHLESS_LT(b, 100);
                uint8_t isGreen = BRANCHLESS_IN_RANGE(g, 100, 255) &
                                  BRANCHLESS_LT(r, 150) &
                                  BRANCHLESS_LT(b, 150);
                
                uint8_t colorMatch = isRed | (isYellow << 1) | (isGreen << 2);
                uint8_t isBallPixel = isCandidate & (saturation > 30) & (colorMatch > 0);
                
                if (isBallPixel && ballCount < MAX_BALLS) {
                    BallOutput& ball = balls[ballCount];
                    ball.xQ16 = static_cast<int32_t>(x + 2) << 16;
                    ball.yQ16 = static_cast<int32_t>(y) << 16;
                    ball.radius = 20;
                    ball.confidence = std::min<uint8_t>(255, saturation + 50);
                    ball.colorIndex = colorMatch;
                    ballCount++;
                }
            }
            
            // =====================================
            // Pixel 3
            // =====================================
            {
                uint8_t* pixel = rowPtr + ((x + 3) * 4);
                
                uint8_t r = pixel[0];
                uint8_t g = pixel[1];
                uint8_t b = pixel[2];
                
                uint16_t brightness = (77 * r + 150 * g + 29 * b) >> 8;
                uint8_t isCandidate = BRANCHLESS_GT(brightness, 150);
                
                uint8_t minRGB = (r < g) ? ((r < b) ? r : b) : ((g < b) ? g : b);
                uint16_t sumRGB = r + g + b;
                uint8_t saturation = (sumRGB > 0) ? 
                    ((sumRGB - 3 * minRGB) * 255 / sumRGB) : 0;
                
                uint8_t isRed = BRANCHLESS_IN_RANGE(r, 150, 255) & 
                                BRANCHLESS_LT(g, 100) &
                                BRANCHLESS_LT(b, 100);
                uint8_t isYellow = BRANCHLESS_IN_RANGE(r, 150, 255) &
                                   BRANCHLESS_IN_RANGE(g, 150, 255) &
                                   BRANCHLESS_LT(b, 100);
                uint8_t isGreen = BRANCHLESS_IN_RANGE(g, 100, 255) &
                                  BRANCHLESS_LT(r, 150) &
                                  BRANCHLESS_LT(b, 150);
                
                uint8_t colorMatch = isRed | (isYellow << 1) | (isGreen << 2);
                uint8_t isBallPixel = isCandidate & (saturation > 30) & (colorMatch > 0);
                
                if (isBallPixel && ballCount < MAX_BALLS) {
                    BallOutput& ball = balls[ballCount];
                    ball.xQ16 = static_cast<int32_t>(x + 3) << 16;
                    ball.yQ16 = static_cast<int32_t>(y) << 16;
                    ball.radius = 20;
                    ball.confidence = std::min<uint8_t>(255, saturation + 50);
                    ball.colorIndex = colorMatch;
                    ballCount++;
                }
            }
        }
        
        // ===== REMAINING PIXELS =====
        // Handle width not divisible by 4
        for (; x < width; ++x) {
            uint8_t* pixel = rowPtr + (x * 4);
            
            uint8_t r = pixel[0];
            uint8_t g = pixel[1];
            uint8_t b = pixel[2];
            
            uint16_t brightness = (77 * r + 150 * g + 29 * b) >> 8;
            uint8_t isCandidate = BRANCHLESS_GT(brightness, 150);
            
            uint8_t minRGB = (r < g) ? ((r < b) ? r : b) : ((g < b) ? g : b);
            uint16_t sumRGB = r + g + b;
            uint8_t saturation = (sumRGB > 0) ? 
                ((sumRGB - 3 * minRGB) * 255 / sumRGB) : 0;
            
            uint8_t isRed = BRANCHLESS_IN_RANGE(r, 150, 255) & 
                            BRANCHLESS_LT(g, 100) &
                            BRANCHLESS_LT(b, 100);
            uint8_t isYellow = BRANCHLESS_IN_RANGE(r, 150, 255) &
                               BRANCHLESS_IN_RANGE(g, 150, 255) &
                               BRANCHLESS_LT(b, 100);
            uint8_t isGreen = BRANCHLESS_IN_RANGE(g, 100, 255) &
                              BRANCHLESS_LT(r, 150) &
                              BRANCHLESS_LT(b, 150);
            
            uint8_t colorMatch = isRed | (isYellow << 1) | (isGreen << 2);
            uint8_t isBallPixel = isCandidate & (saturation > 30) & (colorMatch > 0);
            
            if (isBallPixel && ballCount < MAX_BALLS) {
                BallOutput& ball = balls[ballCount];
                ball.xQ16 = static_cast<int32_t>(x) << 16;
                ball.yQ16 = static_cast<int32_t>(y) << 16;
                ball.radius = 20;
                ball.confidence = std::min<uint8_t>(255, saturation + 50);
                ball.colorIndex = colorMatch;
                ballCount++;
            }
        }
    }
    
    return ballCount;
}

} // namespace aether::vision