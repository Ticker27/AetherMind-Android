#pragma once

#include "aether/pool/geometry/PoolGeometryTypes.h"
#include "aether/pool/geometry/GhostBallSolver.h"
#include "aether/pool/geometry/CutAngleSolver.h"

namespace aether::pool::geometry {

class PoolGeometryEngine {
public:
    GeometryShotResult evaluateShot(const GeometryShotRequest& request) const;

private:
    static constexpr double MAX_RECOMMENDED_CUT_ANGLE_DEGREES = 75.0;
    static constexpr double HIGH_RISK_CUT_ANGLE_DEGREES = 60.0;
    static constexpr double MEDIUM_RISK_CUT_ANGLE_DEGREES = 35.0;

    static constexpr double OBSTRUCTION_MARGIN = 1e-6;
    static constexpr double MIN_LOW_RISK_CONFIDENCE = 0.80;
    static constexpr double MIN_MEDIUM_RISK_CONFIDENCE = 0.50;

    bool validateRequest(const GeometryShotRequest& request) const;

    bool isPathObstructed(
        const Vec2& start,
        const Vec2& end,
        const BallSet& otherBalls,
        double clearanceRadius
    ) const;

    double computeConfidence(
        const GeometryShotRequest& request,
        const GhostBallResult& ghostResult,
        const CutAngleResult& cutResult,
        bool cuePathObstructed,
        bool objectPathObstructed
    ) const;

    GeometryRisk classifyRisk(
        bool valid,
        double confidence,
        double cutAngleDegrees
    ) const;

    GeometryShotResult invalidResult(GeometryFailureReason reason) const;
};

} // namespace aether::pool::geometry
