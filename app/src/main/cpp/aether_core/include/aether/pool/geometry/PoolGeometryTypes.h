#pragma once

#include "aether/pool/geometry/Vec2.h"

namespace aether::pool::geometry {

struct Ball {
    int id = -1;
    Vec2 center{};
    double radius = 0.0;
};

struct Pocket {
    int id = -1;
    Vec2 center{};
    double mouthRadius = 0.0;
};

struct TableBounds {
    double minX = 0.0;
    double minY = 0.0;
    double maxX = 1.0;
    double maxY = 1.0;
};

enum class GeometryRisk {
    LOW,
    MEDIUM,
    HIGH,
    INVALID
};

enum class GeometryFailureReason {
    NONE,
    INVALID_INPUT,
    ZERO_LENGTH_OBJECT_TO_POCKET,
    ZERO_LENGTH_CUE_TO_GHOST,
    GHOST_BALL_OUT_OF_TABLE,
    CUE_PATH_OBSTRUCTED,
    OBJECT_PATH_OBSTRUCTED,
    EXTREME_CUT_ANGLE,
    LOW_CONFIDENCE
};

struct BallSet {
    static constexpr int MAX_BALLS = 16;

    Ball balls[MAX_BALLS]{};
    int count = 0;
};

struct GeometryShotRequest {
    Ball cueBall{};
    Ball objectBall{};
    Pocket targetPocket{};
    TableBounds table{};
    BallSet otherBalls{};
};

struct GeometryShotResult {
    bool valid = false;

    Vec2 ghostBallCenter{};
    Vec2 contactPoint{};
    Vec2 collisionNormal{};
    Vec2 cueToGhostDirection{};
    Vec2 objectToPocketDirection{};

    double cutAngleDegrees = 0.0;
    double cueToGhostDistance = 0.0;
    double objectToPocketDistance = 0.0;
    double geometryConfidence = 0.0;

    GeometryRisk risk = GeometryRisk::INVALID;
    GeometryFailureReason failureReason = GeometryFailureReason::INVALID_INPUT;
};

inline bool isValidTableBounds(const TableBounds& table) {
    return isFinite(table.minX) &&
           isFinite(table.minY) &&
           isFinite(table.maxX) &&
           isFinite(table.maxY) &&
           table.maxX > table.minX + EPSILON &&
           table.maxY > table.minY + EPSILON;
}

inline bool isValidBall(const Ball& ball) {
    return ball.radius > EPSILON &&
           isFinite(ball.radius) &&
           isFinite(ball.center);
}

inline bool isValidPocket(const Pocket& pocket) {
    return pocket.mouthRadius >= 0.0 &&
           isFinite(pocket.mouthRadius) &&
           isFinite(pocket.center);
}

inline bool isValidBallSet(const BallSet& ballSet) {
    return ballSet.count >= 0 && ballSet.count <= BallSet::MAX_BALLS;
}

inline bool isInsideTable(const Vec2& point, const TableBounds& table) {
    return isInsideBoundsInclusive(point, table.minX, table.minY, table.maxX, table.maxY);
}

} // namespace aether::pool::geometry
