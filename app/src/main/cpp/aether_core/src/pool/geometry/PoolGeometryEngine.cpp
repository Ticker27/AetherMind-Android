#include "aether/pool/geometry/PoolGeometryEngine.h"

#include <cmath>

namespace aether::pool::geometry {

GeometryShotResult PoolGeometryEngine::evaluateShot(const GeometryShotRequest& request) const {
    if (!validateRequest(request)) {
        return invalidResult(GeometryFailureReason::INVALID_INPUT);
    }

    const GhostBallSolver ghostBallSolver;
    const GhostBallResult ghostResult = ghostBallSolver.solve(
        request.objectBall,
        request.targetPocket
    );

    if (!ghostResult.valid) {
        return invalidResult(ghostResult.failureReason);
    }

    if (!isInsideTable(ghostResult.ghostBallCenter, request.table)) {
        GeometryShotResult result = invalidResult(GeometryFailureReason::GHOST_BALL_OUT_OF_TABLE);
        result.ghostBallCenter = ghostResult.ghostBallCenter;
        result.contactPoint = ghostResult.contactPoint;
        result.collisionNormal = ghostResult.collisionNormal;
        result.objectToPocketDirection = ghostResult.objectToPocketDirection;
        result.objectToPocketDistance = ghostResult.objectToPocketDistance;
        return result;
    }

    const CutAngleSolver cutAngleSolver;
    const CutAngleResult cutResult = cutAngleSolver.compute(
        request.cueBall,
        ghostResult.ghostBallCenter,
        ghostResult.objectToPocketDirection
    );

    if (!cutResult.valid) {
        return invalidResult(cutResult.failureReason);
    }

    const double clearanceRadius = (request.cueBall.radius + request.objectBall.radius) + OBSTRUCTION_MARGIN;

    const bool cuePathObstructed = isPathObstructed(
        request.cueBall.center,
        ghostResult.ghostBallCenter,
        request.otherBalls,
        clearanceRadius
    );

    if (cuePathObstructed) {
        GeometryShotResult result = invalidResult(GeometryFailureReason::CUE_PATH_OBSTRUCTED);
        result.ghostBallCenter = ghostResult.ghostBallCenter;
        result.contactPoint = ghostResult.contactPoint;
        result.collisionNormal = ghostResult.collisionNormal;
        result.cueToGhostDirection = cutResult.cueToGhostDirection;
        result.objectToPocketDirection = ghostResult.objectToPocketDirection;
        result.cutAngleDegrees = cutResult.cutAngleDegrees;
        result.cueToGhostDistance = cutResult.cueToGhostDistance;
        result.objectToPocketDistance = ghostResult.objectToPocketDistance;
        return result;
    }

    const bool objectPathObstructed = isPathObstructed(
        request.objectBall.center,
        request.targetPocket.center,
        request.otherBalls,
        clearanceRadius
    );

    if (objectPathObstructed) {
        GeometryShotResult result = invalidResult(GeometryFailureReason::OBJECT_PATH_OBSTRUCTED);
        result.ghostBallCenter = ghostResult.ghostBallCenter;
        result.contactPoint = ghostResult.contactPoint;
        result.collisionNormal = ghostResult.collisionNormal;
        result.cueToGhostDirection = cutResult.cueToGhostDirection;
        result.objectToPocketDirection = ghostResult.objectToPocketDirection;
        result.cutAngleDegrees = cutResult.cutAngleDegrees;
        result.cueToGhostDistance = cutResult.cueToGhostDistance;
        result.objectToPocketDistance = ghostResult.objectToPocketDistance;
        return result;
    }

    const double confidence = computeConfidence(
        request,
        ghostResult,
        cutResult,
        cuePathObstructed,
        objectPathObstructed
    );

    if (cutResult.cutAngleDegrees > MAX_RECOMMENDED_CUT_ANGLE_DEGREES) {
        GeometryShotResult result = invalidResult(GeometryFailureReason::EXTREME_CUT_ANGLE);
        result.ghostBallCenter = ghostResult.ghostBallCenter;
        result.contactPoint = ghostResult.contactPoint;
        result.collisionNormal = ghostResult.collisionNormal;
        result.cueToGhostDirection = cutResult.cueToGhostDirection;
        result.objectToPocketDirection = ghostResult.objectToPocketDirection;
        result.cutAngleDegrees = cutResult.cutAngleDegrees;
        result.cueToGhostDistance = cutResult.cueToGhostDistance;
        result.objectToPocketDistance = ghostResult.objectToPocketDistance;
        result.geometryConfidence = confidence;
        result.risk = GeometryRisk::HIGH;
        return result;
    }

    GeometryShotResult result{};
    result.valid = true;
    result.ghostBallCenter = ghostResult.ghostBallCenter;
    result.contactPoint = ghostResult.contactPoint;
    result.collisionNormal = ghostResult.collisionNormal;
    result.cueToGhostDirection = cutResult.cueToGhostDirection;
    result.objectToPocketDirection = ghostResult.objectToPocketDirection;
    result.cutAngleDegrees = cutResult.cutAngleDegrees;
    result.cueToGhostDistance = cutResult.cueToGhostDistance;
    result.objectToPocketDistance = ghostResult.objectToPocketDistance;
    result.geometryConfidence = confidence;
    result.risk = classifyRisk(true, confidence, cutResult.cutAngleDegrees);
    result.failureReason = GeometryFailureReason::NONE;

    return result;
}

bool PoolGeometryEngine::validateRequest(const GeometryShotRequest& request) const {
    if (!isValidTableBounds(request.table)) {
        return false;
    }

    if (!isValidBall(request.cueBall) || !isValidBall(request.objectBall)) {
        return false;
    }

    if (!isValidPocket(request.targetPocket)) {
        return false;
    }

    if (!isValidBallSet(request.otherBalls)) {
        return false;
    }

    if (!isInsideTable(request.cueBall.center, request.table)) {
        return false;
    }

    if (!isInsideTable(request.objectBall.center, request.table)) {
        return false;
    }

    if (!isInsideTable(request.targetPocket.center, request.table)) {
        return false;
    }

    for (int i = 0; i < request.otherBalls.count; ++i) {
        const Ball& ball = request.otherBalls.balls[i];

        if (!isValidBall(ball)) {
            return false;
        }

        if (!isInsideTable(ball.center, request.table)) {
            return false;
        }
    }

    return true;
}

bool PoolGeometryEngine::isPathObstructed(
    const Vec2& start,
    const Vec2& end,
    const BallSet& otherBalls,
    double clearanceRadius
) const {
    if (!isFinite(start) || !isFinite(end) || clearanceRadius <= 0.0 || !isFinite(clearanceRadius)) {
        return true;
    }

    if (distance(start, end) <= EPSILON) {
        return true;
    }

    for (int i = 0; i < otherBalls.count; ++i) {
        const Ball& obstacle = otherBalls.balls[i];

        if (!isValidBall(obstacle)) {
            return true;
        }

        const double d = distancePointToSegment(obstacle.center, start, end);

        if (d < clearanceRadius) {
            return true;
        }
    }

    return false;
}

double PoolGeometryEngine::computeConfidence(
    const GeometryShotRequest& request,
    const GhostBallResult& ghostResult,
    const CutAngleResult& cutResult,
    bool cuePathObstructed,
    bool objectPathObstructed
) const {
    double confidence = 1.0;

    if (!ghostResult.valid || !cutResult.valid) {
        return 0.0;
    }

    if (cuePathObstructed) {
        confidence -= 0.40;
    }

    if (objectPathObstructed) {
        confidence -= 0.40;
    }

    if (cutResult.cutAngleDegrees > MEDIUM_RISK_CUT_ANGLE_DEGREES) {
        confidence -= 0.15;
    }

    if (cutResult.cutAngleDegrees > HIGH_RISK_CUT_ANGLE_DEGREES) {
        confidence -= 0.25;
    }

    const double tableWidth = request.table.maxX - request.table.minX;
    const double tableHeight = request.table.maxY - request.table.minY;
    const double tableDiagonal = std::sqrt((tableWidth * tableWidth) + (tableHeight * tableHeight));

    if (tableDiagonal > EPSILON && isFinite(tableDiagonal)) {
        const double cueDistanceRatio = cutResult.cueToGhostDistance / tableDiagonal;
        const double objectDistanceRatio = ghostResult.objectToPocketDistance / tableDiagonal;

        if (cueDistanceRatio > 0.65) {
            confidence -= 0.10;
        }

        if (objectDistanceRatio > 0.65) {
            confidence -= 0.10;
        }
    }

    const double railMargin = request.objectBall.radius * 2.0;

    const bool ghostNearRail =
        ghostResult.ghostBallCenter.x < request.table.minX + railMargin ||
        ghostResult.ghostBallCenter.x > request.table.maxX - railMargin ||
        ghostResult.ghostBallCenter.y < request.table.minY + railMargin ||
        ghostResult.ghostBallCenter.y > request.table.maxY - railMargin;

    if (ghostNearRail) {
        confidence -= 0.10;
    }

    return clampDouble(confidence, 0.0, 1.0);
}

GeometryRisk PoolGeometryEngine::classifyRisk(
    bool valid,
    double confidence,
    double cutAngleDegrees
) const {
    if (!valid) {
        return GeometryRisk::INVALID;
    }

    if (confidence >= MIN_LOW_RISK_CONFIDENCE &&
        cutAngleDegrees <= MEDIUM_RISK_CUT_ANGLE_DEGREES) {
        return GeometryRisk::LOW;
    }

    if (confidence >= MIN_MEDIUM_RISK_CONFIDENCE &&
        cutAngleDegrees <= HIGH_RISK_CUT_ANGLE_DEGREES) {
        return GeometryRisk::MEDIUM;
    }

    return GeometryRisk::HIGH;
}

GeometryShotResult PoolGeometryEngine::invalidResult(GeometryFailureReason reason) const {
    GeometryShotResult result{};
    result.valid = false;
    result.geometryConfidence = 0.0;
    result.risk = GeometryRisk::INVALID;
    result.failureReason = reason;
    return result;
}

} // namespace aether::pool::geometry
