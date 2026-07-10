#pragma once

#include "aether/pool/geometry/PoolGeometryTypes.h"

namespace aether::pool::geometry {

struct CutAngleResult {
    bool valid = false;
    Vec2 cueToGhostDirection{};
    double cueToGhostDistance = 0.0;
    double cutAngleDegrees = 0.0;
    GeometryFailureReason failureReason = GeometryFailureReason::INVALID_INPUT;
};

class CutAngleSolver {
public:
    CutAngleResult compute(
        const Ball& cueBall,
        const Vec2& ghostBallCenter,
        const Vec2& objectToPocketDirection
    ) const;
};

} // namespace aether::pool::geometry
