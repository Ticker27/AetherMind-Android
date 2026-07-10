#pragma once

#include <cstdint>

// =============================================================================
// Aether Vision Lite - VisionProcessor.h
// Minimal header for VisionProcessor singleton
// =============================================================================

namespace aether::vision {

class VisionProcessor {
public:
    // Singleton accessor
    static VisionProcessor& getInstance();
    
    // Main processing function (Zero-allocation)
    // Returns number of balls detected
    int processFrame(
        uint8_t* frameData, 
        uint8_t* outputBuffer, 
        int width, 
        int height, 
        int stride
    );
    
private:
    VisionProcessor() = default;
    ~VisionProcessor() = default;
    
    // Disable copy
    VisionProcessor(const VisionProcessor&) = delete;
    VisionProcessor& operator=(const VisionProcessor&) = delete;
};

} // namespace aether::vision