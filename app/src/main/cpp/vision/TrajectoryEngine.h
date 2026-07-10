#pragma once

#include <cstdint>
#include <vector>

// =============================================================================
// Aether Trajectory Engine - C++ Header
// 
// คำนวณเส้นวิถีการยิงลูกบอล
// ใช้ Normalized Coordinates (0.0 - 1.0) สำหรับ cross-device compatibility
// =============================================================================

namespace aether {
namespace trajectory {

/**
 * 2D Point (Normalized 0.0 - 1.0)
 */
struct Point {
    float x;
    float y;
    
    Point() : x(0), y(0) {}
    Point(float px, float py) : x(px), y(py) {}
};

/**
 * Prediction Result - ผลลัพธ์การคำนวณเส้นวิถี
 */
struct PredictionResult {
    // เส้นทางหลัก (trajectory path)
    std::vector<Point> trajectoryPath;
    
    // จุดที่ลูกบอลจะไปชน (collision point)
    Point collisionPoint;
    
    // มุมที่ต้องยิง (องศา)
    float angleDegrees;
    
    // พลังที่แนะนำ (0.0 - 1.0)
    float recommendedPower;
    
    // ระยะห่างระหว่างลูกขาวกับลูกเป้าหมาย
    float distance;
    
    // ความมั่นใจในการคำนวณ (0.0 - 1.0)
    float confidence;
    
    PredictionResult() 
        : angleDegrees(0), 
          recommendedPower(0.5f), 
          distance(0), 
          confidence(0) {}
};

/**
 * Configuration สำหรับ Trajectory Engine
 */
struct TrajectoryConfig {
    // ขนาดโต๊ะ (Normalized)
    float tableWidth;
    float tableHeight;
    
    // รัศมีลูกบอล (Normalized)
    float ballRadius;
    
    // Coefficient of restitution (ความยืดหยุ่นของการกระทบ)
    float restitution;
    
    // Friction factor
    float friction;
    
    // Maximum trajectory points
    int maxPoints;
    
    TrajectoryConfig() 
        : tableWidth(1.0f),
          tableHeight(1.0f),
          ballRadius(0.02f),    // ~2% ของโต๊ะ (ประมาณรัศมีลูกบอลจริง)
          restitution(0.95f),   // กระทบแล้วเด้ง 95%
          friction(0.98f),      // ลดความเร็ว 2% ต่อครั้งที่กระทบ
          maxPoints(50) {}
};

/**
 * TrajectoryEngine
 * 
 * Singleton class สำหรับคำนวณเส้นวิถีการยิง
 */
class TrajectoryEngine {
public:
    /**
     * คำนวณเส้นวิถีจากลูกขาวไปยังลูกเป้าหมาย
     * 
     * @param cue ตำแหน่งลูกขาว (Normalized 0.0-1.0)
     * @param target ตำแหน่งลูกเป้าหมาย (Normalized 0.0-1.0)
     * @param tableWidth ความกว้างโต๊ะ (Normalized)
     * @param tableHeight ความสูงโต๊ะ (Normalized)
     * @return PredictionResult พร้อม trajectory path และ metadata
     */
    static PredictionResult calculatePath(
        Point cue,
        Point target,
        float tableWidth = 1.0f,
        float tableHeight = 1.0f
    );
    
    /**
     * คำนวณเส้นวิถีพร้อม Configuration
     */
    static PredictionResult calculatePath(
        Point cue,
        Point target,
        const TrajectoryConfig& config
    );
    
    /**
     * ตรวจสอบว่ามุมยิงถูกต้องหรือไม่
     */
    static bool validateShot(
        Point cue,
        Point target,
        Point pocket,
        float tolerance = 0.05f
    );
    
    /**
     * หาตำแหน่ง Ghost Ball (ตำแหน่งที่ลูกขาวจะไปชน)
     */
    static Point calculateGhostBallPosition(
        Point cue,
        Point target,
        float ballRadius
    );
    
    /**
     * คำนวณมุมสะท้อนเมื่อกระทบขอบโต๊ะ
     */
    static Point calculateReflection(
        Point incoming,
        Point normal
    );
    
private:
    // Helper: Generate trajectory points
    static void generateTrajectoryPoints(
        const Point& start,
        const Point& direction,
        float power,
        const TrajectoryConfig& config,
        std::vector<Point>& outPath
    );
    
    // Helper: Check wall collision
    static bool checkWallCollision(
        Point p,
        const TrajectoryConfig& config,
        Point& normal
    );
    
    // Helper: Check ball collision
    static bool checkBallCollision(
        Point p,
        Point targetBall,
        float ballRadius
    );
    
    // Helper: Distance between points
    static float distance(Point a, Point b);
    
    // Helper: Normalize angle to 0-360
    static float normalizeAngle(float degrees);
};

} // namespace trajectory
} // namespace aether