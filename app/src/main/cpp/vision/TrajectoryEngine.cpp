#include "TrajectoryEngine.h"
#include <cmath>
#include <algorithm>

// =============================================================================
// Aether Trajectory Engine - C++ Implementation
// 
// คำนวณเส้นวิถีการยิงลูกบอลแบบ Physics-based
// =============================================================================

namespace aether {
namespace trajectory {

// =============================================================================
// Helper Functions
// =============================================================================

float TrajectoryEngine::distance(Point a, Point b) {
    float dx = b.x - a.x;
    float dy = b.y - a.y;
    return std::sqrt(dx * dx + dy * dy);
}

float TrajectoryEngine::normalizeAngle(float degrees) {
    while (degrees < 0) degrees += 360;
    while (degrees >= 360) degrees -= 360;
    return degrees;
}

Point TrajectoryEngine::calculateGhostBallPosition(
    Point cue,
    Point target,
    float ballRadius
) {
    // Vector from cue to target
    float dx = target.x - cue.x;
    float dy = target.y - cue.y;
    
    // Distance
    float dist = distance(cue, target);
    if (dist < 0.0001f) return target;
    
    // Normalize direction
    float nx = dx / dist;
    float ny = dy / dist;
    
    // Ghost ball position (extend past target by ball radius)
    float extension = ballRadius * 1.5f;
    
    return Point(
        target.x + nx * extension,
        target.y + ny * extension
    );
}

Point TrajectoryEngine::calculateReflection(
    Point incoming,
    Point normal
) {
    // R = D - 2(D·N)N
    float dot = incoming.x * normal.x + incoming.y * normal.y;
    return Point(
        incoming.x - 2 * dot * normal.x,
        incoming.y - 2 * dot * normal.y
    );
}

bool TrajectoryEngine::checkWallCollision(
    Point p,
    const TrajectoryConfig& config,
    Point& normal
) {
    float r = config.ballRadius;
    
    // Left wall
    if (p.x - r < 0) {
        normal = Point(1, 0);
        return true;
    }
    // Right wall
    if (p.x + r > config.tableWidth) {
        normal = Point(-1, 0);
        return true;
    }
    // Top wall
    if (p.y - r < 0) {
        normal = Point(0, 1);
        return true;
    }
    // Bottom wall
    if (p.y + r > config.tableHeight) {
        normal = Point(0, -1);
        return true;
    }
    
    return false;
}

bool TrajectoryEngine::checkBallCollision(
    Point p,
    Point targetBall,
    float ballRadius
) {
    float dist = distance(p, targetBall);
    return dist < (ballRadius * 2.5f);  // 2.5x for margin
}

// =============================================================================
// Trajectory Generation
// =============================================================================

void TrajectoryEngine::generateTrajectoryPoints(
    const Point& start,
    const Point& direction,
    float power,
    const TrajectoryConfig& config,
    std::vector<Point>& outPath
) {
    // Starting point
    outPath.push_back(start);
    
    // Current position
    Point current = start;
    
    // Velocity based on power
    float speed = power * 0.05f;  // Scaled for normalized coordinates
    float vx = direction.x * speed;
    float vy = direction.y * speed;
    
    // Step size for trajectory
    float stepSize = 0.005f;
    
    // Generate points along trajectory
    for (int i = 0; i < config.maxPoints; ++i) {
        // Next position
        Point next;
        next.x = current.x + vx * stepSize * 60;  // 60 FPS
        next.y = current.y + vy * stepSize * 60;
        
        // Check wall collision
        Point wallNormal;
        if (checkWallCollision(next, config, wallNormal)) {
            // Add collision point
            outPath.push_back(next);
            
            // Reflect velocity
            float dot = vx * wallNormal.x + vy * wallNormal.y;
            vx = (vx - 2 * dot * wallNormal.x) * config.restitution;
            vy = (vy - 2 * dot * wallNormal.y) * config.restitution;
            
            // Apply friction
            vx *= config.friction;
            vy *= config.friction;
        }
        
        current = next;
        outPath.push_back(current);
        
        // Stop if speed is too low
        float currentSpeed = std::sqrt(vx * vx + vy * vy);
        if (currentSpeed < 0.001f) break;
    }
}

// =============================================================================
// Main Calculation
// =============================================================================

PredictionResult TrajectoryEngine::calculatePath(
    Point cue,
    Point target,
    float tableWidth,
    float tableHeight
) {
    TrajectoryConfig config;
    config.tableWidth = tableWidth;
    config.tableHeight = tableHeight;
    
    return calculatePath(cue, target, config);
}

PredictionResult TrajectoryEngine::calculatePath(
    Point cue,
    Point target,
    const TrajectoryConfig& config
) {
    PredictionResult result;
    
    // Calculate distance
    float dist = distance(cue, target);
    result.distance = dist;
    
    // Direction from cue to target
    Point direction;
    if (dist > 0.0001f) {
        direction.x = (target.x - cue.x) / dist;
        direction.y = (target.y - cue.y) / dist;
    } else {
        direction = Point(1, 0);  // Default to right
    }
    
    // Calculate angle (degrees)
    float angleRadians = std::atan2(direction.y, direction.x);
    result.angleDegrees = normalizeAngle(angleRadians * 180.0f / M_PI);
    
    // Calculate power based on distance
    // Closer = less power, Farther = more power
    // Range: 0.3 to 1.0
    result.recommendedPower = std::min(1.0f, std::max(0.3f, dist * 0.8f + 0.3f));
    
    // Generate trajectory points
    generateTrajectoryPoints(cue, direction, result.recommendedPower, config, result.trajectoryPath);
    
    // Calculate collision point (first point where path meets target direction)
    result.collisionPoint = calculateGhostBallPosition(cue, target, config.ballRadius);
    
    // Calculate confidence based on distance and trajectory length
    // Closer targets = higher confidence
    float distanceScore = 1.0f - std::min(1.0f, dist / 0.5f);  // Max confidence at 50% distance
    float pathLengthScore = std::min(1.0f, (float)result.trajectoryPath.size() / 20.0f);
    
    result.confidence = (distanceScore * 0.7f + pathLengthScore * 0.3f);
    
    // Ensure minimum points
    if (result.trajectoryPath.empty()) {
        result.trajectoryPath.push_back(cue);
        result.trajectoryPath.push_back(target);
    }
    
    return result;
}

// =============================================================================
// Validation
// =============================================================================

bool TrajectoryEngine::validateShot(
    Point cue,
    Point target,
    Point pocket,
    float tolerance
) {
    // Calculate angle to target
    float dx = target.x - cue.x;
    float dy = target.y - cue.y;
    float distToTarget = std::sqrt(dx * dx + dy * dy);
    
    if (distToTarget < 0.0001f) return false;
    
    // Normalize to direction
    float dirX = dx / distToTarget;
    float dirY = dy / distToTarget;
    
    // Calculate angle to pocket
    float pdx = pocket.x - cue.x;
    float pdy = pocket.y - cue.y;
    float distToPocket = std::sqrt(pdx * pdx + pdy * pdy);
    
    if (distToPocket < 0.0001f) return false;
    
    float pocketDirX = pdx / distToPocket;
    float pocketDirY = pdy / distToPocket;
    
    // Check if target direction is within tolerance of pocket direction
    float dot = dirX * pocketDirX + dirY * pocketDirY;  // Dot product
    
    // Convert tolerance to dot product threshold
    float toleranceDot = std::cos(tolerance * M_PI / 180.0f);
    
    return dot >= toleranceDot;
}

} // namespace trajectory
} // namespace aether